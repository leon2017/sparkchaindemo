package com.example.sparkchaindemo

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.sparkchaindemo.spark.asr.AsrHelper
import com.example.sparkchaindemo.spark.asr.AsrSpeechRecognizer
import com.example.sparkchaindemo.spark.sparkchain.SparkChainHelper
import com.example.sparkchaindemo.util.AudioPlayerResultListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel(), AudioPlayerResultListener {

    private val _uiState = MutableStateFlow(MainUiState.default())

    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var state: MainUiState
        get() = _uiState.value
        set(value) {
            _uiState.update { value }
        }

    private var asrHelper = AsrHelper()
    private var sparkChainHelper = SparkChainHelper(this)

    private var audioToggle: Boolean = false

    init {
        asrHelper.setAsrSpeechRecognizer(asrSpeechRecognizer())
        sparkChainHelper.initialize()
    }

    private fun asrSpeechRecognizer() = object : AsrSpeechRecognizer {

        override fun onStart() {
        }

        override fun onResult(text: String, last: Boolean) {
            state = state.copy(
                asrText = text
            )
            if (last) {
                state = state.copy(
                    ttsStatus = TtsStatus.TTS
                )
                sparkChainHelper.genSparkChain(text)
            }
        }

        override fun onFailure() {
            state = state.copy(
                ttsStatus = TtsStatus.IDLE
            )
        }

    }

    override fun onPlay(boolean: Boolean) {
        Log.d("MainViewModel", "onPlay: $boolean")
        state = state.copy(
            ttsStatus = if (boolean) TtsStatus.TTS else TtsStatus.AUDIO_RECORD
        )
        if (!boolean && audioToggle){
            asrHelper.startSpeech()
        }
    }

    fun asrToggle() {
        if (audioToggle) {
            asrHelper.stopSpeech()
            sparkChainHelper.stopTts()
            state = state.copy(
                ttsStatus = TtsStatus.IDLE
            )
        } else {
            asrHelper.startSpeech()
            state = state.copy(
                ttsStatus = TtsStatus.AUDIO_RECORD
            )
        }
        audioToggle = !audioToggle
    }

    override fun onCleared() {
        super.onCleared()
        asrHelper.destroy()
        sparkChainHelper.destroy()
    }


}