package com.example.ai_powered_android_apps_with_gemini

sealed interface UiState {
    object Initial : UiState
    object Loading : UiState
    data class Success(val outputText: String) : UiState
    data class Error(val errorMessage: String) : UiState

}