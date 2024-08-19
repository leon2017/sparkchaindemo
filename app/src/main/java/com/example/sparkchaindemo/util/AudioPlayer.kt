package com.example.sparkchaindemo.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * @Desc: 简易PCM播放器
 */
/**
 * 合成事务状态
 */
enum class TtsEventStatus {
    START,
    END
}

interface AudioPlayerResultListener {
    fun onPlay(boolean: Boolean)
}


class AudioPlayer {

    companion object {
        private const val TAG = "AudioPlayer"
        private const val AUDIO_PLAYER_INIT = -1
        private const val AUDIO_PLAYER_PLAYER = 0
        private const val AUDIO_PLAYER_STOP = 1

        private const val SAMPLE_RATE = 16000
        private const val BUFFER_SIZE = 1280
        private const val FRAME_SIZE = 1280
    }

    private var audioTrack: AudioTrack? = null

    //声道，表示音频的声道数，有单声道和立体声两种，单声道用AudioFormat.CHANNEL_OUT_MONO表示，立体声用AudioFormat.CHANNEL_OUT_STEREO表示
    private val channelConfig =
        AudioFormat.CHANNEL_OUT_MONO

    //格式，表示音频的数据格式，有8位和16位两种，8位用AudioFormat.ENCODING_PCM_8BIT表示，16位用AudioFormat.ENCODING_PCM_16BIT表示
    private val audioFormat =
        AudioFormat.ENCODING_PCM_16BIT

    private var mainHandler = Handler(Looper.getMainLooper())

    private val loopAudio: AtomicBoolean = AtomicBoolean(false)

    private val stopAudio: AtomicBoolean = AtomicBoolean(false)

    //音频缓存队列
    private val audioByteCache: ConcurrentLinkedQueue<ByteArray> = ConcurrentLinkedQueue()

    private var audioPlayerCallback: AudioPlayerResultListener? = null

    //合成事务状态
    private var ttsEventStatus = TtsEventStatus.START

    private lateinit var speechHandler: Handler

    private var startCountDownLatch: CountDownLatch? = null

    private var stopCountDownLatch: CountDownLatch? = null

    init {
        val handlerThread = HandlerThread("tts_audio_player", -16)
        handlerThread.start()
        speechHandler = Handler(handlerThread.looper) {
            when (it.what) {
                AUDIO_PLAYER_INIT -> {
                    if (audioTrack != null) {
                        audioTrack?.release()
                        audioTrack = null
                    }
                    val minBufferSize =
                        AudioTrack.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
                    val buffer = Math.max(minBufferSize, BUFFER_SIZE)
                    audioTrack = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA) //设置音频的用途，有媒体、闹钟、通知等多种选项，这里使用媒体用途
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC) //设置音频的内容类型，有音乐、语音、电影等多种选项，这里使用音乐类型
                                .build()
                        ) //构建一个AudioAttributes对象
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(audioFormat) //设置音频的数据格式
                                .setSampleRate(SAMPLE_RATE) //设置音频的采样率
                                .setChannelMask(channelConfig) //设置音频的声道
                                .build()
                        ) //构建一个AudioFormat对象
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .setBufferSizeInBytes(buffer) //设置缓冲区大小
                        .build()
                    startCountDownLatch?.countDown()
                    Log.w(TAG, "=======> AudioTrack init success")
                }

                AUDIO_PLAYER_PLAYER -> {
                    while (loopAudio.get()) {
                        val audioBytes = audioByteCache.poll()
                        if (audioBytes != null) {
                            //将audioBytes按照1920的长度进行分割，并写入audioTrack中
                            val audioByteList = ArrayList<ByteArray>()
                            val buffer = ByteBuffer.wrap(audioBytes)
                            while (buffer.hasRemaining()) {
                                val end = minOf(buffer.position() + FRAME_SIZE, buffer.limit())
                                val audioByte = ByteArray(end - buffer.position())
                                buffer.get(audioByte)
                                audioByteList.add(audioByte)
                            }
                            audioByteList.forEach { byte ->
                                try {
                                    if (!stopAudio.get()) {
                                        audioTrack?.write(byte, 0, byte.size)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            if (ttsEventStatus == TtsEventStatus.END) {
                                ttsEventStatus = TtsEventStatus.START
                                loopAudio.set(false)
                                speechHandler.obtainMessage(AUDIO_PLAYER_STOP).sendToTarget()
                            }
                        }
                    }
                }

                AUDIO_PLAYER_STOP -> {
                    audioByteCache.clear()
                    try {
                        if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                            audioTrack?.stop()
                            audioTrack?.flush()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.e(TAG, "audioTrack is exception: ${e.message}")
                    }
                    mainHandler.post {
                        audioPlayerCallback?.onPlay(false)
                    }
                    stopCountDownLatch?.countDown()
                }
            }
            return@Handler true
        }
        ttsEventStatus = TtsEventStatus.START
        mainHandler.removeCallbacksAndMessages(null)
        speechHandler.removeCallbacksAndMessages(null)
        startCountDownLatch = CountDownLatch(1)
        speechHandler.obtainMessage(AUDIO_PLAYER_INIT).sendToTarget()
        try {
            startCountDownLatch?.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setAudioPlayerCallback(audioPlayerCallback: AudioPlayerResultListener) {
        this.audioPlayerCallback = audioPlayerCallback
    }

    @Synchronized
    fun playAudio(audioData: ByteArray) {
        kotlin.runCatching {
            if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                loopAudio.set(false)
                stopAudio.set(true)
                speechHandler.removeCallbacksAndMessages(null)
                startCountDownLatch = CountDownLatch(1)
                speechHandler.obtainMessage(AUDIO_PLAYER_INIT).sendToTarget()
                try {
                    startCountDownLatch?.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (!loopAudio.get()) {
                loopAudio.set(true)
                stopAudio.set(false)
                if (audioTrack?.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack?.play()
                    speechHandler.obtainMessage(AUDIO_PLAYER_PLAYER).sendToTarget()
                }
            }
            audioByteCache.offer(audioData)
        }
    }

    /**
     * 合成的音频数据下发状态
     * @param status [TtsEventStatus.START] 合成开始， [TtsEventStatus.END] 合成结束
     */
    fun changeTtsEventStatus(status: TtsEventStatus) {
        if (status == TtsEventStatus.START) {
            mainHandler.post {
                audioPlayerCallback?.onPlay(true)
            }
        }
        ttsEventStatus = status
    }

    fun stop() {
        speechHandler.removeCallbacksAndMessages(null)
        loopAudio.set(false)
        audioByteCache.clear()
        stopAudio.set(true)
        mainHandler.removeCallbacksAndMessages(null)
    }

    fun isPlaying(): Boolean {
        return audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    fun destroy() {
        loopAudio.set(false)
        audioByteCache.clear()
        audioTrack?.pause()
        audioTrack?.release()
        audioTrack = null
        mainHandler.removeCallbacksAndMessages(null)
        speechHandler.removeCallbacksAndMessages(null)
    }

}