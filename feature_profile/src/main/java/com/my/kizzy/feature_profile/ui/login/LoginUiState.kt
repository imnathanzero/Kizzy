package com.my.kizzy.feature_profile.ui.login

sealed interface LoginUiState {
    object InitialState: LoginUiState
    object OnWebLoginClicked: LoginUiState
    object OnQrLoginClicked: LoginUiState
    object OnTokenLoginClicked: LoginUiState
    object OnLoginCompleted: LoginUiState
}
