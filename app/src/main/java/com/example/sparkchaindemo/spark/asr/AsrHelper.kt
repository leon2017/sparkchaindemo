package com.example.sparkchaindemo.spark.asr

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.example.sparkchaindemo.model.AsrAudioStatus
import com.example.sparkchaindemo.model.AsrDecoder
import com.example.sparkchaindemo.model.AsrParams
import com.example.sparkchaindemo.model.AsrResponseData
import com.example.sparkchaindemo.util.AudioRecorder
import com.example.sparkchaindemo.util.AuthUtil
import com.example.sparkchaindemo.util.GsonUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit


class AsrHelper : AsrSpeech, AudioRecorder.RecorderListener {

    companion object {
        private const val TIME_OUT = 15_1000L
        private const val TAG = "AsrHelper"

        //实现一个okhttpclient
        private val okhttpClient = OkHttpClient.Builder()
            .readTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
            .writeTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
            .connectTimeout(TIME_OUT, TimeUnit.MILLISECONDS)
            .build()
    }

    private var webSocket: WebSocket? = null

    private var asrSpeechRecognizer: AsrSpeechRecognizer? = null

    private var recorder: AudioRecorder = AudioRecorder(this)

    private var decoder: AsrDecoder = AsrDecoder()

    @Volatile
    private var audioStatus: AsrAudioStatus = AsrAudioStatus.BEGIN

    private fun getHttpClient(): OkHttpClient {
        return okhttpClient
    }


    fun setAsrSpeechRecognizer(asrSpeechRecognizer: AsrSpeechRecognizer) {
        this.asrSpeechRecognizer = asrSpeechRecognizer
    }


    override fun startSpeech() {
        val authUrl = AuthUtil.getAuthUrl()
        val url = authUrl.replace("http://", "ws://")
            .replace("https://", "wss://")
        val request = Request.Builder().url(url).build()
        webSocket = getHttpClient().newWebSocket(request, webSocketListener())
    }

    override fun stopSpeech() {
        recorder.stopReadAudio()
    }

    override fun cancel() {
        webSocket?.cancel()
        webSocket = null
    }

    private fun webSocketListener() = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.w(TAG, "听写wss=====>onOpen")
            //连接成功，开启麦克风
            recorder.startReadAudio()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            Log.w(TAG, "听写wss=====>onMessage: $text")
            handleWssMessage(text)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.w(TAG, "听写wss=====>onClosed")
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosing(webSocket, code, reason)
            Log.w(TAG, "听写wss=====>onClosing")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            Log.w(TAG, "听写wss=====>onFailure")
            cancel()
            stopSpeech()
        }
    }

    private fun handleWssMessage(text: String) {
        if (TextUtils.isEmpty(text)) return
        val resp = GsonUtil.fromJson(text, AsrResponseData::class.java) ?: return
        if (resp.code != 0) {
            audioStatus = AsrAudioStatus.END
            asrSpeechRecognizer?.onFailure()
            stopSpeech()
            return
        }
        val text1 = resp.data?.result?.text
        try {
            if (text1 != null) {
                decoder.decode(text1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val resultText = decoder.toString()
        var isLast = false
        if (resp.data?.status == 2) {
            recorder.stopReadAudio()
            audioStatus = AsrAudioStatus.END
            //说明数据全部返回完毕，可以关闭连接，释放资源
            isLast = true
            decoder.discard()
            webSocket?.close(1000, "")
        }
        asrSpeechRecognizer?.onResult(resultText, isLast)
    }

    private fun sendMessage(data: ByteArray) {
        if (webSocket == null) return
        val params = AsrParams()
        when (audioStatus) {
            AsrAudioStatus.BEGIN -> {
                params.common = AsrParams.Common().apply {
                    appId = AuthUtil.SPARK_APPID
                }
                params.business = AsrParams.Business()
            }

            AsrAudioStatus.CONTINUE -> {}
            AsrAudioStatus.END -> {}
        }
        params.audioData = AsrParams.Data().apply {
            status = audioStatus.ordinal
            audio = Base64.encodeToString(data, Base64.NO_WRAP)
        }
        val json = GsonUtil.toJson(params)
        Log.w(TAG, "发送的消息==> $json")
        webSocket?.send(json)
    }

    override fun onRecordStart() {
        audioStatus = AsrAudioStatus.BEGIN
        asrSpeechRecognizer?.onStart()
    }

    override fun onRecord(data: ByteArray?) {
        if (data == null) return
        sendMessage(data)
        audioStatus = AsrAudioStatus.CONTINUE
    }

    override fun onRecordStop() {
        if (audioStatus == AsrAudioStatus.END) return
        audioStatus = AsrAudioStatus.END
        sendMessage(ByteArray(0))
    }

    override fun onRecordError() {
        cancel()
        audioStatus = AsrAudioStatus.END
        asrSpeechRecognizer?.onFailure()
    }

    fun destroy() {
        asrSpeechRecognizer = null
        recorder.destroy()
        cancel()
    }
}

interface AsrSpeechRecognizer {
    fun onStart()
    fun onResult(text: String, last: Boolean)
    fun onFailure()
}