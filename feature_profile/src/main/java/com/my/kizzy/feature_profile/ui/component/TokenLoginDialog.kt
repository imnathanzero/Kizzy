package com.my.kizzy.feature_profile.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.my.kizzy.resources.R

@Composable
fun TokenLoginDialog(
    onDismissRequest: () -> Unit,
    onTokenSubmit: (String) -> Unit
) {
    var tokenText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Login with Token")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Paste your Discord account token below to authenticate directly without browser login.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = tokenText,
                    onValueChange = {
                        tokenText = it
                        if (isError && it.isNotBlank()) isError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Account Token") },
                    singleLine = false,
                    maxLines = 3,
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Token cannot be empty") }
                    } else null,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Hide token" else "Show token"
                                )
                            }
                            IconButton(onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    tokenText = clip.trim().removeSurrounding("\"").removeSurrounding("'")
                                    isError = false
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste from clipboard"
                                )
                            }
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sanitized = tokenText.trim().removeSurrounding("\"").removeSurrounding("'")
                    if (sanitized.isNotBlank()) {
                        onTokenSubmit(sanitized)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text("Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
