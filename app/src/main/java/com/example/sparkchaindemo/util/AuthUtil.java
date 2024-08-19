package com.example.sparkchaindemo.util;

import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.HttpUrl;

public class AuthUtil {

    public static String SPARK_APPID = "xxx";
    public static String SPARK_APPKEY = "xxx";
    public static String SPARK_APPSECRET = "xxx";

    public static String getAuthUrl() throws Exception {
        String hostUrl = "https://iat-api.xfyun.cn/v2/iat";
        URL url = new URL(hostUrl);
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("GET ").append(url.getPath()).append(" HTTP/1.1");
        //System.out.println(builder);
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(SPARK_APPSECRET.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(builder.toString().getBytes(charset));
        String sha = android.util.Base64.encodeToString(hexDigits, android.util.Base64.NO_WRAP);
        //System.out.println(sha);
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", SPARK_APPKEY, "hmac-sha256", "host date request-line", sha);
        //System.out.println(authorization);
        HttpUrl httpUrl = HttpUrl.parse("https://" + url.getHost() + url.getPath()).newBuilder().//
                addQueryParameter("authorization", android.util.Base64.encodeToString(authorization.getBytes(charset), android.util.Base64.NO_WRAP)).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();
        return httpUrl.toString();
    }
}
