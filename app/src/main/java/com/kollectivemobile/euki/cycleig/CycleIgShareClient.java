package com.kollectivemobile.euki.cycleig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CycleIgShareClient {
    public static final String SHLEP_BASE_URL = "https://shlep.exe.xyz";
    public static final String VIEWER_URL = "https://cycle.fhir.me/view";
    public static final int MAX_USES = 5;
    public static final int SHARE_DAYS = 7;

    private final Transport transport;
    private final ShlinkCrypto crypto;
    private final CycleIgBundleBuilder bundleBuilder;

    public CycleIgShareClient() {
        this(new HttpTransport(), new ShlinkCrypto(), new CycleIgBundleBuilder());
    }

    public CycleIgShareClient(Transport transport, ShlinkCrypto crypto, CycleIgBundleBuilder bundleBuilder) {
        this.transport = transport;
        this.crypto = crypto;
        this.bundleBuilder = bundleBuilder;
    }

    public CycleIgShare createShare(CycleIgSnapshot snapshot) throws IOException {
        return createShare(snapshot, MAX_USES);
    }

    public CycleIgShare createShare(CycleIgSnapshot snapshot, int maxUses) throws IOException {
        String bundleJson = bundleBuilder.build(snapshot);
        ShlinkCrypto.EncryptedPayload encrypted = crypto.encrypt(bundleJson);
        long exp = System.currentTimeMillis() / 1000L + SHARE_DAYS * 24L * 60L * 60L;

        JsonObject policy = new JsonObject();
        policy.addProperty("exp", exp);
        policy.addProperty("maxUses", maxUses);
        policy.addProperty("audit", true);

        JsonObject request = new JsonObject();
        request.addProperty("ciphertext", encrypted.jwe);
        request.addProperty("contentType", "application/fhir+json");
        request.add("policy", policy);

        Response response = transport.post(SHLEP_BASE_URL + "/shares", request.toString(), null);
        if (response.code < 200 || response.code >= 300) {
            throw new IOException("shlep share creation failed: " + response.code);
        }

        JsonObject share = JsonParser.parseString(response.body).getAsJsonObject();
        String id = share.get("id").getAsString();
        String fileUrl = share.get("fileUrl").getAsString();
        String manageToken = share.get("manageToken").getAsString();
        String viewerLink = SmartHealthLink.composeViewerLink(VIEWER_URL, fileUrl, encrypted.key, exp, "Euki SMART Link");

        return new CycleIgShare(id, fileUrl, manageToken, viewerLink, encrypted.key, exp, maxUses, bundleJson, encrypted.jwe);
    }

    public void revoke(CycleIgShare share) throws IOException {
        Response response = transport.delete(SHLEP_BASE_URL + "/shares/" + share.id, "Bearer " + share.manageToken);
        if (response.code < 200 || response.code >= 300) {
            throw new IOException("shlep share revocation failed: " + response.code);
        }
    }

    public interface Transport {
        Response post(String url, String body, String authorization) throws IOException;
        Response delete(String url, String authorization) throws IOException;
    }

    public static class Response {
        public final int code;
        public final String body;
        public final String contentType;

        public Response(int code, String body, String contentType) {
            this.code = code;
            this.body = body;
            this.contentType = contentType;
        }
    }

    public static class HttpTransport implements Transport {
        @Override
        public Response post(String url, String body, String authorization) throws IOException {
            HttpURLConnection connection = open(url, "POST", authorization);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            connection.setRequestProperty("content-type", "application/json");
            connection.setDoOutput(true);
            try (OutputStream stream = connection.getOutputStream()) {
                stream.write(bytes);
            }
            return response(connection);
        }

        @Override
        public Response delete(String url, String authorization) throws IOException {
            HttpURLConnection connection = open(url, "DELETE", authorization);
            return response(connection);
        }

        private HttpURLConnection open(String url, String method, String authorization) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("accept", "application/json");
            if (authorization != null) {
                connection.setRequestProperty("authorization", authorization);
            }
            return connection;
        }

        private Response response(HttpURLConnection connection) throws IOException {
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String body = read(stream);
            return new Response(code, body, connection.getContentType());
        }

        private String read(InputStream stream) throws IOException {
            if (stream == null) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
            return builder.toString();
        }
    }
}
