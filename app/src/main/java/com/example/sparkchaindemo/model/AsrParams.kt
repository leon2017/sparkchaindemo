package com.example.sparkchaindemo.model


import com.google.gson.annotations.SerializedName

enum class AsrAudioStatus {
    BEGIN,
    CONTINUE,
    END
}

data class AsrParams(
    @SerializedName("business")
    var business: Business? = null,
    @SerializedName("common")
    var common: Common? = null,
    @SerializedName("data")
    var audioData: Data? = null
) {
    data class Business(
        @SerializedName("accent")
        var accent: String? = "mandarin",
        @SerializedName("domain")
        var domain: String? = "iat",
        @SerializedName("language")
        var language: String? = "zh_cn",
        @SerializedName("dwa")
        var dwa: String? = "wpgs" //动态修正
    )

    data class Common(
        @SerializedName("app_id")
        var appId: String? = null
    )

    data class Data(
        @SerializedName("audio")
        var audio: String? = null,
        @SerializedName("encoding")
        var encoding: String? = "raw",
        @SerializedName("format")
        var format: String? = "audio/L16;rate=16000",
        @SerializedName("status")
        var status: Int? = null
    )
}