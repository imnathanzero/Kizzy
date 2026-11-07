/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * KizzyRPC.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.data.rpc

import com.my.kizzy.domain.interfaces.Logger
import com.my.kizzy.domain.repository.KizzyRepository
import com.my.kizzy.preference.Prefs
import com.my.kizzy.preference.Prefs.CUSTOM_ACTIVITY_TYPE
import kizzy.gateway.DiscordWebSocket
import kizzy.gateway.entities.presence.Activity
import kizzy.gateway.entities.presence.Assets
import kizzy.gateway.entities.presence.Metadata
import kizzy.gateway.entities.presence.Party
import kizzy.gateway.entities.presence.Presence
import kizzy.gateway.entities.presence.Timestamps
import kotlinx.coroutines.isActive

class KizzyRPC(
    private val token: String,
    private val kizzyRepository: KizzyRepository,
    private val discordWebSocket: DiscordWebSocket,
    private val logger: Logger
) {
    private lateinit var presence: Presence
    private var activityName: String? = null
    private var applicationIdNumber = Prefs[Prefs.CUSTOM_ACTIVITY_APPLICATION_ID, Constants.APPLICATION_ID]
    private var details: String? = null
    private var state: String? = null
    private var party: Party? = null
    private var largeImage: RpcImage? = null
    private var smallImage: RpcImage? = null
    private var largeText: String? = null
    private var smallText: String? = null
    private var status: String? = null
    private var startTimestamps: Long? = null
    private var stopTimestamps: Long? = null
    private var type: Int = 0
    private var platform: String? = null
    private var buttons = ArrayList<String>()
    private var buttonUrl = ArrayList<String>()
    private var url: String? = null

    fun closeRPC() {
        discordWebSocket.close()
    }

    fun isRpcRunning(): Boolean {
        return discordWebSocket.isWebSocketConnected()
    }

    /**
     * Use Regex to check if token are valid
     * @return true if token is valid else false
     *
     * source: [#token-structure](https://gist.github.com/aydynx/5d29e903417354fd25641b98efc9d437#token-structure)
     */
    private fun isUserTokenValid(): Boolean {
        /*val regex = Regex(
            "[a-z\\d]{24}\\.[a-z\\d]{6}\\.([\\w-]{107}|[\\w-]{38}|[\\w-]{27})|mfa\\.[\\w-]{84}",
            RegexOption.IGNORE_CASE
        )
        return regex.matches(token)*/
        return token.isNotBlank()
    }

    /**
     * Activity Name of Rpc
     *
     * @param activity_name
     * @return
     */
    fun setName(activity_name: String?): KizzyRPC {
        this.activityName = activity_name
        return this
    }

    /**
     * Details of Rpc
     *
     * @param details
     * @return
     */
    fun setDetails(details: String?): KizzyRPC {
        this.details = details
        return this
    }

    /**
     * Rpc State
     *
     * @param state
     * @return
     */
    fun setState(state: String?): KizzyRPC {
        this.state = state
        return this
    }

    /**
     * Party of Rpc
     *
     * @param current Current party size
     * @param max Max party size
     * @return
     */
    fun setPartySize(current: Int?, max: Int?): KizzyRPC {
        if (current != null && max != null) {
            this.party = Party(
                id = "kizzy",
                size = arrayOf(current, max)
            )
        }
        return this
    }

    /**
     * Large image on rpc
     * How to get Image ?
     * Upload image to any discord chat and copy its media link it should look like "https://media.discordapp.net/attachments/90202992002/xyz.png" now just use the image link from attachments part
     * so it would look like: .setLargeImage("attachments/90202992002/xyz.png")
     * @param large_image
     * @return
     */
    fun setLargeImage(large_image: RpcImage?, large_text: String? = null): KizzyRPC {
        this.largeText = large_text.takeIf { !it.isNullOrEmpty() }
        this.largeImage = large_image
        return this
    }

    /**
     * Small image on Rpc
     *
     * @param small_image
     * @return
     */
    fun setSmallImage(small_image: RpcImage?, small_text: String? = null): KizzyRPC {
        this.smallText = small_text.takeIf { !it.isNullOrEmpty() }
        this.smallImage = small_image
        return this
    }

    /**
     * start timestamps
     *
     * @param start_timestamps
     * @return
     */
    fun setStartTimestamps(start_timestamps: Long?): KizzyRPC {
        this.startTimestamps = start_timestamps
        return this
    }

    /**
     * stop timestamps
     *
     * @param stop_timestamps
     * @return
     */
    fun setStopTimestamps(stop_timestamps: Long?): KizzyRPC {
        this.stopTimestamps = stop_timestamps
        return this
    }

    /**
     * Activity Types
     * 0: Playing
     * 1: Streaming
     * 2: Listening
     * 3: Watching
     * 5: Competing
     *
     * @param type
     * @return
     */

    fun setType(type: Int): KizzyRPC {
        if (type in 0..5)
            this.type = type
        else this.type = 0
        return this
    }

    /** Status type for profile online,idle,dnd
     *
     * @param status
     * @return
     */
    fun setStatus(status: String?): KizzyRPC {
        this.status = status
        return this
    }

    /** Platform type for profile xbox,playstation,pc,ios,android etc.
     *
     * @param platform
     * @return
     */
    fun setPlatform(platform: String?): KizzyRPC {
        this.platform = platform
        return this
    }

    fun clearButtons(): KizzyRPC {
        buttons.clear()
        buttonUrl.clear()
        return this
    }

    /**
     * Button1 text
     * @param button1_Text
     * @return
     */
    fun setButton1(button1_Text: String?): KizzyRPC {
        button1_Text?.let { buttons.add(it) }
        return this
    }

    /**
     * Button2 text
     * @param button2_text
     * @return
     */
    fun setButton2(button2_text: String?): KizzyRPC {
        button2_text?.let { buttons.add(it) }
        return this
    }

    /**
     * Button1 url
     * @param url
     * @return
     */
    fun setButton1URL(url: String?): KizzyRPC {
        url?.let { buttonUrl.add(it) }
        return this
    }

    /**
     * Button2 url
     * @param url
     * @return
     */
    fun setButton2URL(url: String?): KizzyRPC {
        url?.let { buttonUrl.add(it) }
        return this
    }
    /**
     * Streaming Url
     * @param url The streaming type currently only supports Twitch and YouTube.
     * Only https://twitch.tv/ and https://youtube.com/ urls will work
     */
    fun setStreamUrl(url: String?): KizzyRPC {
        this.url = url
        return this
    }

    private fun String.sanitize(): String {
        return if (this.length > 128) {
            this.substring(0, 128)
        } else {
            this
        }
    }

    suspend fun build() {
        val isGame = type == 0
        val resolvedLarge = try {
            largeImage?.resolveImage(kizzyRepository)
        } catch (e: Exception) {
            logger.e("KizzyRPC", "Large image resolution failed: ${e.message}")
            null
        }
        val resolvedSmall = try {
            smallImage?.resolveImage(kizzyRepository)
        } catch (e: Exception) {
            logger.e("KizzyRPC", "Small image resolution failed: ${e.message}")
            null
        }
        val assets = if (resolvedLarge != null || resolvedSmall != null) {
            Assets(
                largeImage = resolvedLarge,
                smallImage = resolvedSmall,
                largeText = if (resolvedLarge != null) largeText?.sanitize() else null,
                smallText = if (resolvedSmall != null) smallText?.sanitize() else null
            )
        } else null

        presence = Presence(
            activities = listOf(
                Activity(
                    name = activityName,
                    state = state?.sanitize(),
                    details = details?.sanitize(),
                    party = party.takeIf { isGame && party != null },
                    type = type,
                    timestamps = Timestamps(
                        start = startTimestamps,
                        end = stopTimestamps
                    ).takeIf { startTimestamps != null || stopTimestamps != null },
                    assets = assets,
                    buttons = buttons.takeIf { buttons.size > 0 },
                    metadata = Metadata(buttonUrls = buttonUrl).takeIf { buttonUrl.size > 0 },
                    applicationId = applicationIdNumber.takeIf { it.isNotEmpty() } ?: Constants.APPLICATION_ID,
                    url = url
                )
            ),
            afk = true,
            since = if (status == "idle") (startTimestamps ?: System.currentTimeMillis()) else 0L,
            status = status
        )
        connectToWebSocket()
    }
    private suspend fun connectToWebSocket() {
        if (!isUserTokenValid())
            logger.e(
                tag = "KizzyRPC",
                event = "Token Seems to be invalid, Please Login if you haven't"
            )
        discordWebSocket.connect()
        discordWebSocket.sendActivity(presence)
    }

    suspend fun updateRPC(commonRpc: CommonRpc, enableTimestamps: Boolean? = true) {
        if (!discordWebSocket.isActive) return
        var time = Timestamps(start = startTimestamps)
        if (commonRpc.time != null)
            Timestamps(end = commonRpc.time.end, start = commonRpc.time.start).also { time = it }
        if (commonRpc.partyCurrentSize != null && commonRpc.partyMaxSize != null)
            Party(id = "kizzy", size = arrayOf(commonRpc.partyCurrentSize, commonRpc.partyMaxSize)).also { party = it }
        val activityType = commonRpc.type ?: Prefs[CUSTOM_ACTIVITY_TYPE, 0]
        val isGame = activityType == 0

        val resolvedLarge = try {
            commonRpc.largeImage?.resolveImage(kizzyRepository)
        } catch (e: Exception) {
            logger.e("KizzyRPC", "Large image resolution failed: ${e.message}")
            null
        }
        val resolvedSmall = try {
            commonRpc.smallImage?.resolveImage(kizzyRepository)
        } catch (e: Exception) {
            logger.e("KizzyRPC", "Small image resolution failed: ${e.message}")
            null
        }
        val assets = if (resolvedLarge != null || resolvedSmall != null) {
            Assets(
                largeImage = resolvedLarge,
                smallImage = resolvedSmall,
                largeText = if (resolvedLarge != null) commonRpc.largeText?.sanitize() else null,
                smallText = if (resolvedSmall != null) commonRpc.smallText?.sanitize() else null
            )
        } else null

        val rpcButtons = ArrayList<String>()
        val rpcButtonUrls = ArrayList<String>()
        if (!commonRpc.button1.isNullOrEmpty() && !commonRpc.button1Url.isNullOrEmpty()) {
            rpcButtons.add(commonRpc.button1)
            rpcButtonUrls.add(commonRpc.button1Url)
        }
        if (!commonRpc.button2.isNullOrEmpty() && !commonRpc.button2Url.isNullOrEmpty()) {
            rpcButtons.add(commonRpc.button2)
            rpcButtonUrls.add(commonRpc.button2Url)
        }
        val finalButtons = if (rpcButtons.isNotEmpty()) rpcButtons else buttons.takeIf { it.isNotEmpty() && buttonUrl.size >= it.size }
        val finalButtonUrls = if (rpcButtonUrls.isNotEmpty()) rpcButtonUrls else buttonUrl.takeIf { it.isNotEmpty() && buttons.size >= it.size }

        discordWebSocket.sendActivity(
            Presence(
                activities = listOf(
                    Activity(
                        name = commonRpc.name,
                        details = commonRpc.details?.takeIf { it.isNotEmpty() }?.sanitize(),
                        state = commonRpc.state?.takeIf { it.isNotEmpty() }?.sanitize(),
                        type = activityType,
                        timestamps = time.takeIf { enableTimestamps == true },
                        assets = assets,
                        party = party.takeIf { isGame && party != null },
                        buttons = finalButtons,
                        metadata = finalButtonUrls?.let { Metadata(buttonUrls = it) },
                        applicationId = applicationIdNumber.takeIf { it.isNotEmpty() } ?: Constants.APPLICATION_ID
                    )
                ),
                afk = true,
                since = if (this.status == "idle") (startTimestamps ?: System.currentTimeMillis()) else 0L,
                status = this.status
            )
        )
    }
}
