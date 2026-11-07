package kizzy.gateway.remoteauth

import com.my.kizzy.domain.interfaces.Logger
import com.my.kizzy.domain.interfaces.NoOpLogger
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.serialization.json.*
import java.security.*
import java.security.spec.MGF1ParameterSpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import kotlin.coroutines.CoroutineContext

sealed interface RemoteAuthState {
    object Idle : RemoteAuthState
    object Connecting : RemoteAuthState
    data class QrReady(val fingerprint: String, val url: String) : RemoteAuthState
    data class UserPending(val username: String, val avatar: String?, val discriminator: String?) : RemoteAuthState
    data class Success(val token: String) : RemoteAuthState
    data class Error(val message: String) : RemoteAuthState
    object Cancelled : RemoteAuthState
}

class DiscordRemoteAuth(
    private val logger: Logger = NoOpLogger
) : CoroutineScope {

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    private val _state = MutableStateFlow<RemoteAuthState>(RemoteAuthState.Idle)
    val state: StateFlow<RemoteAuthState> = _state.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient {
        install(WebSockets)
    }

    private var websocketSession: DefaultClientWebSocketSession? = null
    private var heartbeatJob: Job? = null
    private var keyPair: KeyPair? = null

    fun start() {
        if (_state.value is RemoteAuthState.Connecting || _state.value is RemoteAuthState.QrReady) return
        _state.value = RemoteAuthState.Connecting
        launch {
            try {
                // Generate RSA-2048 keypair
                val keyGen = KeyPairGenerator.getInstance("RSA")
                keyGen.initialize(2048)
                val generatedKeyPair = keyGen.generateKeyPair()
                keyPair = generatedKeyPair

                val spkiDer = generatedKeyPair.public.encoded
                val base64PublicKey = Base64.getEncoder().encodeToString(spkiDer)

                logger.i("RemoteAuth", "Connecting to Discord Remote Auth Gateway...")
                val session = client.webSocketSession {
                    url("wss://remote-auth-gateway.discord.gg/?v=2")
                    header(HttpHeaders.Origin, "https://discord.com")
                }
                websocketSession = session

                session.incoming.receiveAsFlow().collect { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        handleMessage(text, base64PublicKey, generatedKeyPair)
                    }
                }
            } catch (e: CancellationException) {
                logger.i("RemoteAuth", "Remote auth session cancelled")
            } catch (e: Exception) {
                logger.e("RemoteAuth", "Remote auth error: ${e.message}")
                _state.value = RemoteAuthState.Error(e.message ?: "Connection error")
            } finally {
                cleanup()
            }
        }
    }

    private suspend fun handleMessage(
        jsonString: String,
        base64PublicKey: String,
        kp: KeyPair
    ) {
        val root = try {
            json.parseToJsonElement(jsonString).jsonObject
        } catch (_: Exception) {
            return
        }

        val op = root["op"]?.jsonPrimitive?.content ?: return
        logger.d("RemoteAuth", "Received op: $op")

        when (op) {
            "hello" -> {
                val interval = root["heartbeat_interval"]?.jsonPrimitive?.long ?: 41250L
                startHeartbeat(interval)

                // Send init with public key
                val initPayload = buildJsonObject {
                    put("op", "init")
                    put("encoded_public_key", base64PublicKey)
                }
                sendJson(initPayload)
            }

            "nonce_proof" -> {
                val encryptedNonce = root["encrypted_nonce"]?.jsonPrimitive?.content ?: return
                val decryptedNonce = decryptRsaOaep(
                    Base64.getDecoder().decode(encryptedNonce),
                    kp.private
                )
                val digest = MessageDigest.getInstance("SHA-256").digest(decryptedNonce)
                val proof = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)

                val proofPayload = buildJsonObject {
                    put("op", "nonce_proof")
                    put("proof", proof)
                }
                sendJson(proofPayload)
            }

            "fingerprint" -> {
                val fingerprint = root["fingerprint"]?.jsonPrimitive?.content ?: return
                val qrUrl = "https://discord.com/ra/$fingerprint"
                logger.i("RemoteAuth", "QR fingerprint ready: $fingerprint")
                _state.value = RemoteAuthState.QrReady(fingerprint = fingerprint, url = qrUrl)
            }

            "pending_remote_init" -> {
                val fingerprint = root["fingerprint"]?.jsonPrimitive?.content
                if (fingerprint != null) {
                    val qrUrl = "https://discord.com/ra/$fingerprint"
                    _state.value = RemoteAuthState.QrReady(fingerprint = fingerprint, url = qrUrl)
                }
                val username = root["user_username"]?.jsonPrimitive?.content
                if (username != null) {
                    val avatar = root["user_avatar"]?.jsonPrimitive?.contentOrNull
                    val discriminator = root["user_discriminator"]?.jsonPrimitive?.contentOrNull
                    _state.value = RemoteAuthState.UserPending(
                        username = username,
                        avatar = avatar,
                        discriminator = discriminator
                    )
                }
            }

            "pending_ticket" -> {
                val encryptedTicket = root["ticket"]?.jsonPrimitive?.content ?: return
                val decryptedTicketBytes = decryptRsaOaep(
                    Base64.getDecoder().decode(encryptedTicket),
                    kp.private
                )
                val ticket = String(decryptedTicketBytes, Charsets.UTF_8)
                exchangeTicketForToken(ticket, kp.private)
            }

            "pending_login" -> {
                val ticket = root["ticket"]?.jsonPrimitive?.contentOrNull
                if (ticket != null) {
                    val decryptedTicketBytes = decryptRsaOaep(
                        Base64.getDecoder().decode(ticket),
                        kp.private
                    )
                    val ticketStr = String(decryptedTicketBytes, Charsets.UTF_8)
                    exchangeTicketForToken(ticketStr, kp.private)
                }
            }

            "cancel" -> {
                logger.i("RemoteAuth", "Login cancelled by user")
                _state.value = RemoteAuthState.Cancelled
                cleanup()
            }
        }
    }

    private suspend fun exchangeTicketForToken(ticket: String, privateKey: PrivateKey) {
        try {
            logger.i("RemoteAuth", "Exchanging ticket for auth token...")
            val response = client.post("https://discord.com/api/v9/users/@me/remote-auth/login") {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Origin, "https://discord.com")
                setBody(buildJsonObject { put("ticket", ticket) }.toString())
            }

            val bodyString = response.bodyAsText()
            val responseJson = json.parseToJsonElement(bodyString).jsonObject
            val encryptedToken = responseJson["encrypted_token"]?.jsonPrimitive?.content

            if (encryptedToken != null) {
                val decryptedTokenBytes = decryptRsaOaep(
                    Base64.getDecoder().decode(encryptedToken),
                    privateKey
                )
                val userToken = String(decryptedTokenBytes, Charsets.UTF_8)
                logger.i("RemoteAuth", "Successfully decrypted user token!")
                _state.value = RemoteAuthState.Success(userToken)
            } else {
                val errorMsg = responseJson["message"]?.jsonPrimitive?.content ?: "Token exchange failed"
                _state.value = RemoteAuthState.Error(errorMsg)
            }
        } catch (e: Exception) {
            logger.e("RemoteAuth", "Failed to exchange ticket: ${e.message}")
            _state.value = RemoteAuthState.Error(e.message ?: "Failed to exchange ticket")
        } finally {
            cleanup()
        }
    }

    private fun decryptRsaOaep(cipherText: ByteArray, privateKey: PrivateKey): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepSpec = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepSpec)
        return cipher.doFinal(cipherText)
    }

    private suspend fun sendJson(jsonElement: JsonElement) {
        val text = jsonElement.toString()
        websocketSession?.send(Frame.Text(text))
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = launch {
            while (isActive) {
                delay(intervalMs)
                sendJson(buildJsonObject { put("op", "heartbeat") })
            }
        }
    }

    private fun cleanup() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        val session = websocketSession
        websocketSession = null
        launch {
            try {
                session?.close()
            } catch (_: Exception) {}
        }
    }

    fun cancel() {
        _state.value = RemoteAuthState.Cancelled
        job.cancelChildren()
        cleanup()
    }
}
