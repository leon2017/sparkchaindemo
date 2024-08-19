package com.example.sparkchaindemo.spark.sparkchain

import android.util.Log
import com.example.sparkchaindemo.MyApp
import com.example.sparkchaindemo.util.AudioPlayer
import com.example.sparkchaindemo.util.AudioPlayerResultListener
import com.example.sparkchaindemo.util.AuthUtil
import com.example.sparkchaindemo.util.TtsEventStatus
import com.iflytek.sparkchain.core.LLM
import com.iflytek.sparkchain.core.LLMCallbacks
import com.iflytek.sparkchain.core.LLMConfig
import com.iflytek.sparkchain.core.LLMError
import com.iflytek.sparkchain.core.LLMEvent
import com.iflytek.sparkchain.core.LLMResult
import com.iflytek.sparkchain.core.SparkChain
import com.iflytek.sparkchain.core.SparkChainConfig
import com.iflytek.sparkchain.core.tts.PersonateTTS
import com.iflytek.sparkchain.core.tts.TTS
import com.iflytek.sparkchain.core.tts.TTSCallbacks


class SparkChainHelper(private val audioPlayerCallback: AudioPlayerResultListener) {


    companion object {
        private const val TAG = "SparkChainHelper"
    }

    @Volatile
    private var isAuth = false

    private lateinit var audioPlayer: AudioPlayer

    private var personateTTS: PersonateTTS? = null

    init {
        initAudioPlayer()
    }

    fun initialize() {
        val config = SparkChainConfig.builder()
            .appID(AuthUtil.SPARK_APPID)
            .apiKey(AuthUtil.SPARK_APPKEY)
            .apiSecret(AuthUtil.SPARK_APPSECRET)
            .workDir(MyApp.CONTEXT.externalCacheDir?.absolutePath ?: "")
            .logLevel(0)
        val ret = SparkChain.getInst().init(MyApp.CONTEXT, config)
        isAuth = ret == 0
    }

    private fun initAudioPlayer() {
        audioPlayer = AudioPlayer().apply {
            setAudioPlayerCallback(audioPlayerCallback)
        }
    }

    fun genSparkChain(text: String) {
        val llmConfig = LLMConfig.builder()
        llmConfig.domain("general")
//        llmConfig.url("wss://spark-api.xf-yun.com/v2.1/chat") //如果使用generalv2，domain和url都可缺省，SDK默认；如果使用general，url可缺省，SDK会自动补充；如果是其他，则需要设置domain和url。
        val llm = LLM(llmConfig)
        val messageBuilder = StringBuilder()
        //异步调用
        val llmCallbacks: LLMCallbacks = object : LLMCallbacks {

            override fun onLLMResult(llmResult: LLMResult, usrContext: Any) {
                Log.d(
                    TAG,
                    "异步调用：" + "onLLMResult:" + " " +  llmResult.status + " " + llmResult.role + " " + llmResult.content
                )
                messageBuilder.append(llmResult.content?:"")
                if ( llmResult.status== 2) {
                    text2Speech(messageBuilder.toString())
                }
            }

            override fun onLLMEvent(event: LLMEvent, usrContext: Any) {
                Log.w(TAG, "onLLMEvent:" + " " + event.eventID + " " + event.eventMsg)
            }

            override fun onLLMError(error: LLMError, usrContext: Any) {
                Log.e(TAG, "onLLMError:" + " " + error.errCode + " " + error.errMsg)
            }
        }
        llm.registerLLMCallbacks(llmCallbacks)
        val myContext = "myContext"
        val ret = llm.arun(text, myContext)
    }

    private fun text2Speech(text: String) {
        personateTTS = PersonateTTS("x4_lingxiaoxuan_oral").apply {
            speed(50)
            pitch(50)
            volume(50)
            sparkAssist(true)
            oralLevel("high")
            registerCallbacks(ttsCallbacks())
        }
        personateTTS?.aRun(text)
    }


    fun stopTts() {
        audioPlayer.stop()
        personateTTS?.stop()
    }

    private fun ttsCallbacks() = object : TTSCallbacks {

        override fun onResult(result: TTS.TTSResult?, o: Any?) {
            if (result?.status == 0) {
                audioPlayer.changeTtsEventStatus(TtsEventStatus.START)
            } else if (result?.status == 2) {
                audioPlayer.changeTtsEventStatus(TtsEventStatus.END)
            }
            result?.data?.let {
                audioPlayer.playAudio(it)
            }
        }

        override fun onError(ttsError: TTS.TTSError?, o: Any?) {
            audioPlayerCallback.onPlay(false)
        }
    }

    fun destroy() {
        SparkChain.getInst().unInit()
        audioPlayer.destroy()
    }
}