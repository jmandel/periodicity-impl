package com.kollectivemobile.euki.cycleig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SmartHealthLink {
    private static final Base64.Encoder B64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DECODER = Base64.getUrlDecoder();

    private SmartHealthLink() {
    }

    public static String composeViewerLink(String viewerBaseUrl, String fileUrl, String key, long exp, String label) {
        JsonObject payload = new JsonObject();
        payload.addProperty("url", fileUrl);
        payload.addProperty("key", key);
        payload.addProperty("flag", "U");
        payload.addProperty("label", label);
        payload.addProperty("exp", exp);
        payload.addProperty("v", 1);
        String shlink = "shlink:/" + b64Url(payload.toString().getBytes(StandardCharsets.UTF_8));
        return viewerBaseUrl.replaceAll("/+$", "") + "#" + shlink;
    }

    public static JsonObject parse(String input) {
        int index = input.indexOf("shlink:/");
        if (index < 0) {
            throw new IllegalArgumentException("no shlink:/ found");
        }
        String payload = input.substring(index + "shlink:/".length());
        return JsonParser.parseString(new String(b64UrlDecode(payload), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    public static String b64Url(byte[] bytes) {
        return B64_URL_ENCODER.encodeToString(bytes);
    }

    public static byte[] b64UrlDecode(String value) {
        return B64_URL_DECODER.decode(value);
    }
}
