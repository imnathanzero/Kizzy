package com.my.kizzy.feature_profile.ui.login

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.my.kizzy.feature_profile.getUserInfo
import com.my.kizzy.feature_profile.ui.component.DiscordLoginButton
import com.my.kizzy.feature_profile.ui.component.DiscordLoginWebView
import com.my.kizzy.feature_profile.ui.component.QrLoginSheet
import com.my.kizzy.feature_profile.ui.component.TokenLoginDialog
import com.my.kizzy.preference.Prefs
import com.my.kizzy.preference.Prefs.TOKEN
import com.my.kizzy.resources.R
import com.my.kizzy.ui.components.BackButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    onCompleted: () -> Unit,
    onBackPressed: () -> Unit
) {
    var buttonEnabledState by remember { mutableStateOf(true) }
    var uiState: LoginUiState by remember { mutableStateOf(LoginUiState.InitialState) }
    val scope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    fun handleSuccessfulToken(token: String) {
        Prefs[TOKEN] = token
        buttonEnabledState = false
        uiState = LoginUiState.OnLoginCompleted
        scope.launch {
            getUserInfo(token, onInfoSaved = {
                onCompleted()
            })
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = { BackButton { onBackPressed() } }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                LoginUiState.InitialState -> {}

                LoginUiState.OnWebLoginClicked -> {
                    ModalBottomSheet(
                        onDismissRequest = {
                            uiState = LoginUiState.InitialState
                        },
                        sheetState = modalBottomSheetState,
                        dragHandle = {
                            BottomSheetDefaults.DragHandle()
                        },
                    ) {
                        DiscordLoginWebView {
                            scope.launch {
                                modalBottomSheetState.hide()
                                handleSuccessfulToken(it)
                            }
                        }
                    }
                }

                LoginUiState.OnQrLoginClicked -> {
                    QrLoginSheet(
                        onDismissRequest = {
                            uiState = LoginUiState.InitialState
                        },
                        onTokenReceived = {
                            handleSuccessfulToken(it)
                        }
                    )
                }

                LoginUiState.OnTokenLoginClicked -> {
                    TokenLoginDialog(
                        onDismissRequest = {
                            uiState = LoginUiState.InitialState
                        },
                        onTokenSubmit = {
                            handleSuccessfulToken(it)
                        }
                    )
                }

                LoginUiState.OnLoginCompleted -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading profile...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState != LoginUiState.OnLoginCompleted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_discord),
                        tint = Color.Unspecified,
                        contentDescription = "Discord Logo",
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.login_with_discord),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Choose your preferred login method",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    DiscordLoginButton(
                        onClick = { uiState = LoginUiState.OnWebLoginClicked },
                        enabled = buttonEnabledState
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FilledTonalButton(
                        onClick = { uiState = LoginUiState.OnQrLoginClicked },
                        enabled = buttonEnabledState,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Login with QR Code")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { uiState = LoginUiState.OnTokenLoginClicked },
                        enabled = buttonEnabledState,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Login with Token")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onCompleted = {},
        onBackPressed = {}
    )
}
