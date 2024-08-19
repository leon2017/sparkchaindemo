package com.example.sparkchaindemo


enum class TtsStatus {
    IDLE,
    AUDIO_RECORD,
    TTS
}

data class MainUiState(
    val ttsStatus: TtsStatus,
    val asrText: String
) {
    companion object {

        fun default(): MainUiState {
            return MainUiState(
                ttsStatus = TtsStatus.IDLE,
                asrText = ""
            )
        }
    }
}