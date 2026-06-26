package com.kollectivemobile.euki.cycleig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;

import org.junit.Assume;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CycleIgTest {
    private static final String CYCLE = "https://cycle.fhir.me/CodeSystem/cycle";

    @Test
    public void createsScopedSampleSnapshot() {
        CycleIgSnapshot snapshot = sampleSnapshot();
        CycleIgSnapshot.Preview preview = snapshot.getPreview();

        assertEquals("2026-01-02", snapshot.getStartDateString());
        assertEquals("2026-06-22", snapshot.getEndDateString());
        assertEquals(49, preview.dayCount);
        assertEquals(28, preview.menstrualBleedingFacts);
        assertEquals(3, preview.nonMenstrualBleedingFacts);
        assertEquals(28, preview.flowFacts);
        assertEquals(31, preview.productFacts);
        assertEquals(7, preview.clotFacts);
        assertEquals(49, preview.emotionFacts);
        assertEquals(49, preview.bodyFacts);
        assertEquals(17, preview.noteFacts);
    }

    @Test
    public void buildsPeriodTrackingBundleFromStoredFactsOnly() {
        String bundleJson = new CycleIgBundleBuilder().build(sampleSnapshot());
        JsonObject bundle = JsonParser.parseString(bundleJson).getAsJsonObject();
        JsonArray entries = bundle.getAsJsonArray("entry");

        assertEquals("Bundle", bundle.get("resourceType").getAsString());
        assertEquals("collection", bundle.get("type").getAsString());
        assertEquals(
                "https://cycle.fhir.me/StructureDefinition/period-tracking-bundle",
                bundle.getAsJsonObject("meta").getAsJsonArray("profile").get(0).getAsString()
        );
        assertEquals(212, entries.size());
        assertEquals(31, countByCycleCode(entries, "menstrual-bleeding"));
        assertEquals(28, countByCycleCode(entries, "menstrual-flow"));
        assertEquals(28, countBleeding(entries, true));
        assertEquals(3, countBleeding(entries, false));
        assertTrue(bundleJson.contains("Synthetic Euki sample cycle 1 day 1."));
        assertFalse(bundleJson.contains("contraception_pill"));
        assertFalse(bundleJson.contains("protection_pregnancy"));
    }

    @Test
    public void encryptsAndParsesViewerPrefixedShlink() {
        String bundleJson = new CycleIgBundleBuilder().build(sampleSnapshot());
        ShlinkCrypto crypto = new ShlinkCrypto();
        ShlinkCrypto.EncryptedPayload encrypted = crypto.encrypt(bundleJson);
        String viewerLink = SmartHealthLink.composeViewerLink(
                CycleIgShareClient.VIEWER_URL,
                "https://shlep.exe.xyz/shl/test",
                encrypted.key,
                1782993600L,
                "Euki SMART Link"
        );

        JsonObject payload = SmartHealthLink.parse(viewerLink);
        assertTrue(viewerLink.startsWith("https://cycle.fhir.me/view#shlink:/"));
        assertEquals("https://shlep.exe.xyz/shl/test", payload.get("url").getAsString());
        assertEquals("U", payload.get("flag").getAsString());
        assertEquals(bundleJson, crypto.decryptCompact(encrypted.jwe, payload.get("key").getAsString()));
        assertFalse(encrypted.jwe.contains("Synthetic Euki sample cycle 1 day 1."));
    }

    @Test
    public void uploadsOnlyCiphertextToShlep() throws IOException {
        FakeTransport transport = new FakeTransport();
        CycleIgShare share = new CycleIgShareClient(
                transport,
                new ShlinkCrypto(),
                new CycleIgBundleBuilder()
        ).createShare(sampleSnapshot());

        assertEquals("https://shlep.exe.xyz/shares", transport.postUrl);
        assertTrue(transport.postBody.contains("\"contentType\":\"application/fhir+json\""));
        assertTrue(transport.postBody.contains("\"maxUses\":5"));
        assertFalse(transport.postBody.contains("Synthetic Euki sample cycle 1 day 1."));
        assertTrue(share.viewerLink.startsWith("https://cycle.fhir.me/view#shlink:/"));
        assertEquals("https://shlep.exe.xyz/shl/share-id", SmartHealthLink.parse(share.viewerLink).get("url").getAsString());
    }

    @Test
    public void liveShlepCreateResolveRevokeAndMaxUse() throws IOException {
        Assume.assumeTrue("LIVE_CYCLE_IG=1 is required for live shlep smoke tests.", "1".equals(System.getenv("LIVE_CYCLE_IG")));

        CycleIgShareClient client = new CycleIgShareClient();
        CycleIgShare share = client.createShare(sampleSnapshot());
        try {
            CycleIgShareClient.Response fetched = fetch(share.fileUrl, "euki-live-revoke-test");
            assertEquals(200, fetched.code);
            assertFalse(fetched.body.contains("Synthetic Euki sample cycle 1 day 1."));
            assertFalse(share.fileUrl.contains(share.key));

            String decrypted = new ShlinkCrypto().decryptCompact(
                    fetched.body,
                    SmartHealthLink.parse(share.viewerLink).get("key").getAsString()
            );
            JsonObject bundle = JsonParser.parseString(decrypted).getAsJsonObject();
            assertEquals("Bundle", bundle.get("resourceType").getAsString());
            assertEquals(212, bundle.getAsJsonArray("entry").size());

            client.revoke(share);
            assertEquals(404, fetch(share.fileUrl, "euki-live-revoke-after-delete").code);
        } finally {
            try {
                client.revoke(share);
            } catch (IOException ignored) {
                // The share may already be revoked by the assertion path above.
            }
        }

        CycleIgShare oneUseShare = client.createShare(sampleSnapshot(), 1);
        try {
            assertEquals(200, fetch(oneUseShare.fileUrl, "euki-live-max-use-first").code);
            assertEquals(404, fetch(oneUseShare.fileUrl, "euki-live-max-use-second").code);
        } finally {
            try {
                client.revoke(oneUseShare);
            } catch (IOException ignored) {
                // Exhausted shares may already be unavailable.
            }
        }
    }

    private CycleIgSnapshot sampleSnapshot() {
        List<CalendarItem> items = CycleIgSampleData.create();
        return new CycleIgSnapshotFactory().create(items, "", "", CycleIgScope.all());
    }

    private int countByCycleCode(JsonArray entries, String code) {
        int total = 0;
        for (int index = 0; index < entries.size(); index++) {
            JsonObject observation = entries.get(index).getAsJsonObject().getAsJsonObject("resource");
            if (hasCycleCode(observation, code)) {
                total++;
            }
        }
        return total;
    }

    private int countBleeding(JsonArray entries, boolean value) {
        int total = 0;
        for (int index = 0; index < entries.size(); index++) {
            JsonObject observation = entries.get(index).getAsJsonObject().getAsJsonObject("resource");
            if (hasCycleCode(observation, "menstrual-bleeding") && observation.get("valueBoolean").getAsBoolean() == value) {
                total++;
            }
        }
        return total;
    }

    private boolean hasCycleCode(JsonObject observation, String code) {
        JsonArray coding = observation
                .getAsJsonObject("code")
                .getAsJsonArray("coding");
        for (int index = 0; index < coding.size(); index++) {
            JsonObject item = coding.get(index).getAsJsonObject();
            if (CYCLE.equals(item.get("system").getAsString()) && code.equals(item.get("code").getAsString())) {
                return true;
            }
        }
        return false;
    }

    private CycleIgShareClient.Response fetch(String fileUrl, String recipient) throws IOException {
        String separator = fileUrl.contains("?") ? "&" : "?";
        HttpURLConnection connection = (HttpURLConnection) new URL(fileUrl + separator + "recipient=" + recipient).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("accept", "application/jose, application/json");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        return new CycleIgShareClient.Response(code, read(stream), connection.getContentType());
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

    private static class FakeTransport implements CycleIgShareClient.Transport {
        String postUrl;
        String postBody;

        @Override
        public CycleIgShareClient.Response post(String url, String body, String authorization) {
            postUrl = url;
            postBody = body;
            return new CycleIgShareClient.Response(
                    201,
                    "{\"id\":\"share-id\",\"fileUrl\":\"https://shlep.exe.xyz/shl/share-id\",\"manageToken\":\"manage-token\"}",
                    "application/json"
            );
        }

        @Override
        public CycleIgShareClient.Response delete(String url, String authorization) {
            return new CycleIgShareClient.Response(200, "{}", "application/json");
        }
    }
}
