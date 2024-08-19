package com.example.sparkchaindemo.util;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;


public class AudioRecorder {

    private final String TAG = "AudioRecorder";

    private final AudioHandler mAudioReadHandler;

    private final RecorderListener mRecorderListener;

    public interface RecorderListener {
        void onRecordStart();

        void onRecord(byte[] data);

        void onRecordStop();

        void onRecordError();
    }


    public AudioRecorder(RecorderListener recorderListener) {
        mRecorderListener = recorderListener;
        HandlerThread audioThread = new HandlerThread("AudioRecorderThread", -16);
        audioThread.start();
        mAudioReadHandler = new AudioHandler(audioThread.getLooper());
    }

    public void startReadAudio() {
        mAudioReadHandler.obtainMessage(AudioHandler.AUDIO_START).sendToTarget();
    }

    public void stopReadAudio() {
        mAudioReadHandler.sendEmptyMessage(AudioHandler.AUDIO_STOP);
    }

    public class AudioHandler extends Handler {

        public static final int AUDIO_START = 0;
        public static final int AUDIO_READ = 1;
        public static final int AUDIO_STOP = 3;
        public static final int SAMPLE_RATE = 16000;
        private static final int FRAME_SIZE = 1280;
        private AudioRecord mAudioRecord;

        private AtomicBoolean mAudioStart = new AtomicBoolean(false);

        private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;

        public AudioHandler(Looper looper) {
            super(looper);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case AUDIO_START:
                    Log.w(TAG, "麦克风=>初始化");
                    try {
                        if (mAudioStart.get()) {
                            break;
                        }
                        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AUDIO_CHANNEL, AudioFormat.ENCODING_PCM_16BIT);
                        mAudioRecord = new AudioRecord(
                                MediaRecorder.AudioSource.MIC,
                                SAMPLE_RATE,
                                AUDIO_CHANNEL,
                                AudioFormat.ENCODING_PCM_16BIT,
                                minBufferSize
                        );
                        mAudioRecord.startRecording();
                        mAudioStart.set(true);
                        mRecorderListener.onRecordStart();
                        mAudioReadHandler.sendEmptyMessage(AUDIO_READ);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.w(TAG, "麦克风=>初始化失败");
                        mAudioStart.set(false);
                        mRecorderListener.onRecordError();
                    }
                    break;
                case AUDIO_READ:
                    if (mAudioStart.get()) {
                        Log.w(TAG, "麦克风=>读取消息");
                        byte[] data = new byte[FRAME_SIZE];
                        try {
                            mAudioRecord.read(data, 0, data.length);
                            mRecorderListener.onRecord(data);
                            mAudioReadHandler.sendEmptyMessage(AUDIO_READ);
                        } catch (Exception e) {
                            e.printStackTrace();
                            mAudioStart.set(false);
                            Log.w(TAG, "麦克风=>读取消息失败");
                            mRecorderListener.onRecordError();
                        }
                    }
                    break;
                case AUDIO_STOP:
                    Log.w(TAG, "麦克风=>停止");
                    try {
                        if (mAudioStart.get()) {
                            mAudioRecord.stop();
                            mAudioRecord.release();
                            mAudioRecord = null;
                            mAudioStart.set(false);
                            Log.w(TAG, "麦克风=>停止失败0");
                            mRecorderListener.onRecordStop();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        mAudioStart.set(false);
                        Log.w(TAG, "麦克风=>停止失败1");
                        mRecorderListener.onRecordError();
                    }
                    break;
            }

        }
    }


    public void destroy() {
        stopReadAudio();
        if (mAudioReadHandler != null) {
            mAudioReadHandler.removeCallbacksAndMessages(null);
            mAudioReadHandler.getLooper().quitSafely();
        }
    }

}
