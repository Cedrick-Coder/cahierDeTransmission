package com.zkteco.biometric;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class BackendService {
    private final String backendDir;
    private final String apiUrl;
    private final HttpClient client;

    /**
     * Create a BackendService.
     * @param backendDir path to the backend on disk (for informational checks). May be null.
     * @param apiUrl full URL to the POST endpoint accepting JSON {nom,poste,biomData}
     */
    public BackendService(String backendDir, String apiUrl) {
        this.backendDir = backendDir;
        this.apiUrl = apiUrl;
        this.client = HttpClient.newHttpClient();
    }

    public boolean backendDirectoryExists() {
        if (backendDir == null) return false;
        try {
            return Files.exists(Path.of(backendDir));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Send the person record to backend. Returns true on 2xx response.
     */
    public HttpResponse<String> sendPerson(String nom, String poste, String biomBase64) throws IOException, InterruptedException {
        String body = "{"
                + "\"nom\":\"" + escapeJson(nom) + "\"," 
                + "\"poste\":\"" + escapeJson(poste) + "\"," 
                + "\"biomData\":\"" + escapeJson(biomBase64) + "\""
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
