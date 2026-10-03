package com.idestudio.app.ai.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.idestudio.app.data.models.ChatMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Universal, 100% real AI Client.
 * Supports:
 * - Local Ollama (http://localhost:11434 or LAN IP)
 * - Groq Cloud (Free ultra-fast Llama 3)
 * - OpenRouter (All open-source models)
 * - Google Gemini (OpenAI-compatible v1beta endpoint)
 * - OpenAI (GPT models)
 * - Custom self-hosted Ollama Cloud instances
 */
public class OllamaCloudClient {

    private static final String TAG = "OllamaCloudClient";
    public static final String PREFS_NAME = "ide_studio_ai_prefs";

    public static final String KEY_BASE_URL = "ai_base_url";
    public static final String KEY_API_KEY = "ai_api_key";
    public static final String KEY_MODEL = "ai_model";

    public static final String DEFAULT_LOCAL_URL = "http://localhost:11434";
    public static final String DEFAULT_MODEL = "llama3";

    private static OllamaCloudClient instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface AIResponseCallback {
        void onSuccess(String responseText);
        void onError(String errorMessage);
    }

    public interface TestCallback {
        void onResult(boolean success, String message);
    }

    private OllamaCloudClient(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized OllamaCloudClient getInstance(Context context) {
        if (instance == null) {
            instance = new OllamaCloudClient(context);
        }
        return instance;
    }

    public String getBaseUrl() {
        return prefs.getString(KEY_BASE_URL, "");
    }

    public void setBaseUrl(String url) {
        prefs.edit().putString(KEY_BASE_URL, url != null ? url.trim() : "").apply();
    }

    public String getApiKey() {
        return prefs.getString(KEY_API_KEY, "");
    }

    public void setApiKey(String key) {
        prefs.edit().putString(KEY_API_KEY, key != null ? key.trim() : "").apply();
    }

    public String getModel() {
        return prefs.getString(KEY_MODEL, DEFAULT_MODEL);
    }

    public void setModel(String model) {
        prefs.edit().putString(KEY_MODEL, model != null ? model.trim() : DEFAULT_MODEL).apply();
    }

    /**
     * Automatically resolves the endpoint URL and model based on the user's input and API Key provider.
     */
    public ResolvedConfig resolveConfig(String customUrl, String customKey, String customModel) {
        String key = customKey != null ? customKey.trim() : getApiKey();
        String url = customUrl != null ? customUrl.trim() : getBaseUrl();
        String model = customModel != null && !customModel.trim().isEmpty() ? customModel.trim() : getModel();

        // 1. Google Gemini Key detection (starts with AIzaSy)
        if (key.startsWith("AIzaSy")) {
            if (url.isEmpty() || url.contains("localhost")) {
                url = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
            }
            if (model.equals("llama3") || model.isEmpty()) {
                model = "gemini-1.5-flash";
            }
        }
        // 2. Groq Cloud Key detection (starts with gsk_)
        else if (key.startsWith("gsk_")) {
            if (url.isEmpty() || url.contains("localhost")) {
                url = "https://api.groq.com/openai/v1/chat/completions";
            }
            if (model.equals("llama3") || model.isEmpty()) {
                model = "llama-3.1-70b-versatile";
            }
        }
        // 3. OpenRouter Key detection (starts with sk-or-)
        else if (key.startsWith("sk-or-")) {
            if (url.isEmpty() || url.contains("localhost")) {
                url = "https://openrouter.ai/api/v1/chat/completions";
            }
            if (model.equals("llama3") || model.isEmpty()) {
                model = "meta-llama/llama-3-8b-instruct:free";
            }
        }
        // 4. OpenAI Key detection (starts with sk-)
        else if (key.startsWith("sk-")) {
            if (url.isEmpty() || url.contains("localhost")) {
                url = "https://api.openai.com/v1/chat/completions";
            }
            if (model.equals("llama3") || model.isEmpty()) {
                model = "gpt-3.5-turbo";
            }
        }
        // 5. Ollama / Self-hosted / Local
        else {
            if (url.isEmpty()) {
                url = DEFAULT_LOCAL_URL;
            }
        }

        // Clean trailing slashes
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        // Ensure path ends with chat endpoint
        if (!url.endsWith("/chat/completions") && !url.endsWith("/api/chat")) {
            if (url.endsWith("/v1")) {
                url = url + "/chat/completions";
            } else if (url.contains(":11434")) {
                url = url + "/api/chat";
            } else {
                url = url + "/api/chat";
            }
        }

        return new ResolvedConfig(url, key, model);
    }

    public static class ResolvedConfig {
        public final String endpointUrl;
        public final String apiKey;
        public final String model;

        public ResolvedConfig(String endpointUrl, String apiKey, String model) {
            this.endpointUrl = endpointUrl;
            this.apiKey = apiKey;
            this.model = model;
        }
    }

    public void sendMessage(String systemPrompt, String userMessage, AIResponseCallback callback) {
        List<ChatMessage> list = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            list.add(new ChatMessage("system", systemPrompt));
        }
        list.add(new ChatMessage("user", userMessage));
        sendConversation(list, callback);
    }

    public void sendConversation(List<ChatMessage> messages, AIResponseCallback callback) {
        ResolvedConfig config = resolveConfig(getBaseUrl(), getApiKey(), getModel());

        executor.execute(() -> {
            try {
                String result = executeChatRequest(config.endpointUrl, config.apiKey, config.model, messages);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Chat request error: " + e.getMessage(), e);

                // If /api/chat returned 404, fallback to /v1/chat/completions
                if (config.endpointUrl.endsWith("/api/chat")) {
                    String fallbackUrl = config.endpointUrl.replace("/api/chat", "/v1/chat/completions");
                    try {
                        String result = executeChatRequest(fallbackUrl, config.apiKey, config.model, messages);
                        mainHandler.post(() -> callback.onSuccess(result));
                        return;
                    } catch (Exception ignored) {}
                }

                String friendlyMsg = formatFriendlyError(e, config.endpointUrl, config.apiKey);
                mainHandler.post(() -> callback.onError(friendlyMsg));
            }
        });
    }

    public void testConnection(String testUrl, String testKey, String testModel, TestCallback callback) {
        executor.execute(() -> {
            try {
                ResolvedConfig config = resolveConfig(testUrl, testKey, testModel);
                List<ChatMessage> testMsgs = new ArrayList<>();
                testMsgs.add(new ChatMessage("user", "Hello, please reply with OK"));

                String result = executeChatRequest(config.endpointUrl, config.apiKey, config.model, testMsgs);
                mainHandler.post(() -> callback.onResult(true, "Connected successfully to " + config.model + "!\nResponse: " + result.trim()));
            } catch (Exception e) {
                ResolvedConfig config = resolveConfig(testUrl, testKey, testModel);
                String friendlyMsg = formatFriendlyError(e, config.endpointUrl, config.apiKey);
                mainHandler.post(() -> callback.onResult(false, friendlyMsg));
            }
        });
    }

    private String executeChatRequest(String endpoint, String apiKey, String model, List<ChatMessage> messages) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(30000); // 30 sec
        conn.setReadTimeout(60000);    // 60 sec
        conn.setDoOutput(true);
        conn.setDoInput(true);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            conn.setRequestProperty("x-api-key", apiKey.trim());
        }

        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("stream", false);

        JSONArray msgsArr = new JSONArray();
        for (ChatMessage m : messages) {
            JSONObject mObj = new JSONObject();
            mObj.put("role", m.getRole());
            mObj.put("content", m.getContent());
            msgsArr.put(mObj);
        }
        payload.put("messages", msgsArr);

        byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
            os.flush();
        }

        int responseCode = conn.getResponseCode();

        if (responseCode >= 200 && responseCode < 300) {
            String responseStr = readStream(conn.getInputStream());
            return parseSuccessResponse(responseStr);
        } else {
            String errorStr = "";
            if (conn.getErrorStream() != null) {
                errorStr = readStream(conn.getErrorStream());
            }
            throw new ApiException(responseCode, errorStr);
        }
    }

    private String parseSuccessResponse(String raw) throws Exception {
        raw = raw.trim();

        // 1. Single JSON Object
        if (raw.startsWith("{")) {
            JSONObject root = new JSONObject(raw);

            // Ollama /api/chat: { "message": { "content": "..." } }
            if (root.has("message")) {
                JSONObject msg = root.getJSONObject("message");
                if (msg.has("content")) {
                    return msg.getString("content");
                }
            }

            // OpenAI / Groq / OpenRouter / Gemini format: { "choices": [ { "message": { "content": "..." } } ] }
            if (root.has("choices")) {
                JSONArray choices = root.getJSONArray("choices");
                if (choices.length() > 0) {
                    JSONObject first = choices.getJSONObject(0);
                    if (first.has("message")) {
                        return first.getJSONObject("message").getString("content");
                    } else if (first.has("text")) {
                        return first.getString("text");
                    }
                }
            }

            // Ollama /api/generate format: { "response": "..." }
            if (root.has("response")) {
                return root.getString("response");
            }
        }

        // 2. Line-by-line streaming NDJSON fallback
        StringBuilder sb = new StringBuilder();
        String[] lines = raw.split("\n");
        boolean parsedAny = false;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            try {
                JSONObject obj = new JSONObject(line);
                if (obj.has("message") && obj.getJSONObject("message").has("content")) {
                    sb.append(obj.getJSONObject("message").getString("content"));
                    parsedAny = true;
                } else if (obj.has("response")) {
                    sb.append(obj.getString("response"));
                    parsedAny = true;
                }
            } catch (Exception ignored) {}
        }

        if (parsedAny) {
            return sb.toString();
        }

        return raw;
    }

    private String readStream(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private String formatFriendlyError(Exception e, String url, String apiKey) {
        if (e instanceof ApiException) {
            ApiException ae = (ApiException) e;
            int code = ae.statusCode;
            String body = ae.errorBody;

            String detail = "";
            try {
                if (body != null && body.trim().startsWith("{")) {
                    JSONObject errObj = new JSONObject(body);
                    if (errObj.has("error")) {
                        Object err = errObj.get("error");
                        if (err instanceof JSONObject) {
                            detail = ((JSONObject) err).optString("message", err.toString());
                        } else {
                            detail = err.toString();
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (code == 401) {
                return "Authentication Error (401): Invalid API Key. Please verify your API Key in Settings." + (!detail.isEmpty() ? "\n" + detail : "");
            } else if (code == 404) {
                return "Not Found (404): Endpoint or model not found on server (" + url + "). Check model name in Settings." + (!detail.isEmpty() ? "\n" + detail : "");
            } else if (code == 403) {
                return "Access Forbidden (403): Your API Key does not have permission to access this model." + (!detail.isEmpty() ? "\n" + detail : "");
            } else if (code == 429) {
                return "Rate Limit Exceeded (429): Quota exhausted or too many requests." + (!detail.isEmpty() ? "\n" + detail : "");
            } else {
                return "Server returned HTTP " + code + (detail.isEmpty() ? "" : ": " + detail);
            }
        }

        String msg = e.getMessage() != null ? e.getMessage() : e.toString();
        if (msg.contains("ECONNREFUSED") || msg.contains("Connection refused")) {
            if (url.contains("localhost")) {
                return "Connection refused to localhost:11434. Mobile phones cannot connect to your PC via 'localhost'. Use your PC's Wi-Fi IP (e.g. http://192.168.1.X:11434) or use a Cloud provider like Groq or OpenRouter.";
            }
            return "Connection refused (" + url + "). Server is offline or unreachable from this device.";
        }
        if (msg.contains("Cleartext HTTP traffic")) {
            return "Cleartext HTTP traffic error. Enable HTTP in app permissions or use https://";
        }
        if (msg.contains("timeout") || msg.contains("Timeout")) {
            return "Connection timed out. Server took longer than 60 seconds to respond.";
        }
        return "Network Error: " + msg;
    }

    public static class ApiException extends Exception {
        public final int statusCode;
        public final String errorBody;

        public ApiException(int statusCode, String errorBody) {
            super("HTTP " + statusCode + ": " + errorBody);
            this.statusCode = statusCode;
            this.errorBody = errorBody;
        }
    }
}
