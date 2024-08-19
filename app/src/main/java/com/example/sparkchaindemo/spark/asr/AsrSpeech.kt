package com.example.sparkchaindemo.spark.asr

interface AsrSpeech {

    fun startSpeech()

    fun stopSpeech()

    fun cancel();
}