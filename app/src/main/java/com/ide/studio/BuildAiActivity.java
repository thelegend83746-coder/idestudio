package com.ide.studio;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ide.studio.adapter.AiChatAdapter;
import com.ide.studio.core.BackupManager;
import com.ide.studio.core.FileUtils;
import com.ide.studio.core.PreferencesManager;
import com.ide.studio.model.ActionType;
import com.ide.studio.model.AiMessage;
import com.ide.studio.model.ApprovalStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Build AI Pair Programmer Activity.
 * Interacts with Ollama local and remote LLMs to generate complete Android applications
 * and components using pure Java and standard Android XML layouts.
 * Features Plan cards, Action cards with persistent Approve/Reject workflow,
 * automated backup snapshots, undo AI changes, and compiler error fixing.
 */
public class BuildAiActivity extends AppCompatActivity implements AiChatAdapter.OnActionCardClickListener {

    private ImageView mBtnBack;
    private TextView mTvModelStatus;
    private TextView mBtnUndoAi;
    private ImageView mBtnClearChat;
    private ImageView mBtnAiSettings;
    private RecyclerView mRvChat;
    private EditText mEtPrompt;
    private ImageButton mBtnSend;

    private AiChatAdapter mAdapter;
    private File mProjectRoot;
    private File mChatHistoryFile;
    private PreferencesManager mPrefs;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_ai);

        mPrefs = new PreferencesManager(this);

        String path = getIntent().getStringExtra("project_path");
        if (path != null) {
            mProjectRoot = new File(path);
            File metaDir = new File(mProjectRoot, ".build_studio");
            metaDir.mkdirs();
            mChatHistoryFile = new File(metaDir, "ai_chat_history.json");
        }

        initViews();
        loadChatHistory();

        // Check for automated fix build error flow
        String fixError = getIntent().getStringExtra("fix_build_error");
        if (fixError != null && !fixError.trim().isEmpty()) {
            handleFixBuildError(fixError.trim());
        }
    }

    private void initViews() {
        mBtnBack = findViewById(R.id.btn_back);
        mTvModelStatus = findViewById(R.id.tv_model_status);
        mBtnUndoAi = findViewById(R.id.btn_undo_ai);
        mBtnClearChat = findViewById(R.id.btn_clear_chat);
        mBtnAiSettings = findViewById(R.id.btn_ai_settings);
        mRvChat = findViewById(R.id.rv_chat);
        mEtPrompt = findViewById(R.id.et_prompt);
        mBtnSend = findViewById(R.id.btn_send);

        mBtnBack.setOnClickListener(v -> finish());
        mBtnAiSettings.setOnClickListener(v -> startActivity(new Intent(BuildAiActivity.this, OllamaSettingsActivity.class)));

        mBtnUndoAi.setOnClickListener(v -> showUndoAiDialog());

        mBtnClearChat.setOnClickListener(v -> {
            mAdapter.clearMessages();
            if (mChatHistoryFile != null) {
                mChatHistoryFile.delete();
            }
            addWelcomeMessage();
            Toast.makeText(this, "Chat history cleared", Toast.LENGTH_SHORT).show();
        });

        updateModelStatusHeader();

        mRvChat.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new AiChatAdapter(this, this);
        mRvChat.setAdapter(mAdapter);

        mBtnSend.setOnClickListener(v -> sendUserPrompt());
    }

    private void updateModelStatusHeader() {
        mTvModelStatus.setText("Ollama: " + mPrefs.getActiveAiModel() + " (" + mPrefs.getOllamaHost() + ":" + mPrefs.getOllamaPort() + ")");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateModelStatusHeader();
    }

    private void loadChatHistory() {
        if (mChatHistoryFile == null || !mChatHistoryFile.exists()) {
            addWelcomeMessage();
            return;
        }

        try {
            String jsonStr = FileUtils.readFile(mChatHistoryFile);
            if (jsonStr == null || jsonStr.trim().isEmpty() || jsonStr.trim().equals("[]")) {
                addWelcomeMessage();
                return;
            }

            JSONArray arr = new JSONArray(jsonStr);
            List<AiMessage> messages = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String id = obj.optString("id");
                String role = obj.optString("role", "assistant");
                String content = obj.optString("content", "");
                long timestamp = obj.optLong("timestamp", System.currentTimeMillis());
                String planText = obj.optString("planText", null);
                String actionTypeStr = obj.optString("actionType", "NONE");
                ActionType actionType = ActionType.valueOf(actionTypeStr);
                String targetPath = obj.optString("targetFilePath", null);
                String proposedCode = obj.optString("proposedCode", null);
                String newPath = obj.optString("newFilePath", null);
                String statusStr = obj.optString("approvalStatus", "PENDING");
                ApprovalStatus status = ApprovalStatus.valueOf(statusStr);
                boolean invalidXml = obj.optBoolean("isInvalidXml", false);

                messages.add(new AiMessage(id, role, content, timestamp, planText, actionType, targetPath, proposedCode, newPath, status, invalidXml));
            }

            mAdapter.setMessages(messages);
            scrollToBottom();
        } catch (Exception e) {
            addWelcomeMessage();
        }
    }

    private void addWelcomeMessage() {
        AiMessage welcome = new AiMessage("assistant",
                "Hello! I am Build AI. I can generate full Android applications and UI features using pure Java and Android XML.\n" +
                        "Ask me to create activities, add layouts, implement features, or fix compiler errors!");
        mAdapter.addMessage(welcome);
    }

    private void saveChatHistory() {
        if (mChatHistoryFile == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (AiMessage msg : mAdapter.getMessages()) {
                JSONObject obj = new JSONObject();
                obj.put("id", msg.getId());
                obj.put("role", msg.getRole());
                obj.put("content", msg.getContent());
                obj.put("timestamp", msg.getTimestamp());
                if (msg.getPlanText() != null) obj.put("planText", msg.getPlanText());
                obj.put("actionType", msg.getActionType().name());
                if (msg.getTargetFilePath() != null) obj.put("targetFilePath", msg.getTargetFilePath());
                if (msg.getProposedCode() != null) obj.put("proposedCode", msg.getProposedCode());
                if (msg.getNewFilePath() != null) obj.put("newFilePath", msg.getNewFilePath());
                obj.put("approvalStatus", msg.getApprovalStatus().name());
                obj.put("isInvalidXml", msg.isInvalidXml());
                arr.put(obj);
            }
            FileUtils.writeFile(mChatHistoryFile, arr.toString(2));
        } catch (Exception ignored) {}
    }

    private void handleFixBuildError(String errorLogs) {
        String prompt = "Fix this compiler/build error:\n\n" + errorLogs;
        mEtPrompt.setText(prompt);
        sendUserPrompt();
    }

    private void sendUserPrompt() {
        String prompt = mEtPrompt.getText().toString().trim();
        if (prompt.isEmpty()) return;

        mEtPrompt.setText("");

        // 1. Add user message
        AiMessage userMsg = new AiMessage("user", prompt);
        mAdapter.addMessage(userMsg);
        scrollToBottom();
        saveChatHistory();

        // 2. Call Ollama asynchronously with project context
        callOllamaAi(prompt);
    }

    private void callOllamaAi(String userPrompt) {
        new Thread(() -> {
            try {
                String host = mPrefs.getOllamaHost();
                int port = mPrefs.getOllamaPort();
                String model = mPrefs.getActiveAiModel();
                String apiKey = mPrefs.getOllamaApiKey();

                String endpoint;
                if (host.startsWith("http://") || host.startsWith("https://")) {
                    endpoint = host.contains(":") && !host.endsWith("://") && host.lastIndexOf(':') > 6
                            ? host + "/api/chat"
                            : host + ":" + port + "/api/chat";
                } else {
                    endpoint = "http://" + host + ":" + port + "/api/chat";
                }

                JSONObject reqJson = new JSONObject();
                reqJson.put("model", model);
                reqJson.put("stream", false);

                JSONArray messages = new JSONArray();

                // 1. System instructions
                JSONObject sysMsg = new JSONObject();
                sysMsg.put("role", "system");
                sysMsg.put("content", mPrefs.getSystemPrompt());
                messages.put(sysMsg);

                // 2. Provide project context if available
                if (mProjectRoot != null) {
                    JSONObject contextMsg = new JSONObject();
                    contextMsg.put("role", "system");
                    StringBuilder contextSb = new StringBuilder();
                    contextSb.append("Project Name: ").append(mProjectRoot.getName()).append("\n");
                    File appConfig = new File(mProjectRoot, "app/app_config.json");
                    if (appConfig.exists()) {
                        contextSb.append("Configuration: ").append(FileUtils.readFile(appConfig)).append("\n");
                    }
                    contextMsg.put("content", contextSb.toString());
                    messages.put(contextMsg);
                }

                // 3. Add recent history
                List<AiMessage> history = mAdapter.getMessages();
                int startIdx = Math.max(0, history.size() - 6);
                for (int i = startIdx; i < history.size(); i++) {
                    AiMessage m = history.get(i);
                    JSONObject msgObj = new JSONObject();
                    msgObj.put("role", m.getRole());
                    msgObj.put("content", m.getContent());
                    messages.put(msgObj);
                }

                reqJson.put("messages", messages);

                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                if (apiKey != null && !apiKey.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                }
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(60000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(reqJson.toString().getBytes("UTF-8"));
                }

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject respJson = new JSONObject(sb.toString());
                    JSONObject msgObj = respJson.getJSONObject("message");
                    String assistantReply = msgObj.getString("content");

                    mMainHandler.post(() -> processAssistantResponse(assistantReply));
                } else if (code == 401 || code == 403) {
                    mMainHandler.post(() -> {
                        AiMessage errorMsg = new AiMessage("assistant",
                                "Ollama Authentication Failed (HTTP " + code + "): Check your API Key in Settings -> AI Build.");
                        mAdapter.addMessage(errorMsg);
                        scrollToBottom();
                        saveChatHistory();
                    });
                } else {
                    mMainHandler.post(() -> {
                        AiMessage errorMsg = new AiMessage("assistant",
                                "Ollama Error (HTTP " + code + "): Ensure Ollama is running and model '" + model + "' is available.");
                        mAdapter.addMessage(errorMsg);
                        scrollToBottom();
                        saveChatHistory();
                    });
                }
            } catch (Exception e) {
                mMainHandler.post(() -> {
                    AiMessage errorMsg = new AiMessage("assistant",
                            "Could not connect to Ollama: " + e.getMessage() +
                                    "\nCheck your Ollama endpoint and model in Build AI Settings.");
                    mAdapter.addMessage(errorMsg);
                    scrollToBottom();
                    saveChatHistory();
                });
            }
        }).start();
    }

    private void processAssistantResponse(String rawText) {
        // Step 1: Check for Plan / Analysis section
        String planText = null;
        if (rawText.contains("ANALYSIS / PLAN:")) {
            int start = rawText.indexOf("ANALYSIS / PLAN:") + "ANALYSIS / PLAN:".length();
            int end = rawText.indexOf("[CREATE FILE:", start);
            if (end == -1) end = rawText.indexOf("[WRITE FILE:", start);
            if (end == -1) end = rawText.indexOf("[RENAME FILE:", start);
            if (end == -1) end = rawText.indexOf("[DELETE FILE:", start);
            if (end == -1) end = rawText.length();

            planText = rawText.substring(start, end).trim();
            if (!planText.isEmpty()) {
                AiMessage planMsg = new AiMessage("assistant", planText);
                planMsg.setPlanText(planText);
                mAdapter.addMessage(planMsg);
            }
        }

        // Step 2: Check for Action blocks
        boolean hasAction = false;

        // CREATE FILE
        Pattern createPat = Pattern.compile("\\[CREATE FILE:\\s*([^\\]]+)\\]([\\s\\S]*?)\\[/CREATE FILE\\]");
        Matcher mCreate = createPat.matcher(rawText);
        while (mCreate.find()) {
            hasAction = true;
            String path = mCreate.group(1).trim();
            String code = stripCodeFence(mCreate.group(2).trim());
            AiMessage msg = new AiMessage("assistant", "Create file: " + path);
            msg.setActionType(ActionType.CREATE_FILE);
            msg.setTargetFilePath(path);
            msg.setProposedCode(code);
            msg.setApprovalStatus(ApprovalStatus.PENDING);
            mAdapter.addMessage(msg);
        }

        // WRITE FILE
        Pattern writePat = Pattern.compile("\\[WRITE FILE:\\s*([^\\]]+)\\]([\\s\\S]*?)\\[/WRITE FILE\\]");
        Matcher mWrite = writePat.matcher(rawText);
        while (mWrite.find()) {
            hasAction = true;
            String path = mWrite.group(1).trim();
            String code = stripCodeFence(mWrite.group(2).trim());
            AiMessage msg = new AiMessage("assistant", "Modify file: " + path);
            msg.setActionType(ActionType.WRITE_FILE);
            msg.setTargetFilePath(path);
            msg.setProposedCode(code);
            msg.setApprovalStatus(ApprovalStatus.PENDING);
            mAdapter.addMessage(msg);
        }

        // RENAME FILE
        Pattern renamePat = Pattern.compile("\\[RENAME FILE:\\s*([^-]+)->\\s*([^\\]]+)\\]");
        Matcher mRename = renamePat.matcher(rawText);
        while (mRename.find()) {
            hasAction = true;
            String oldPath = mRename.group(1).trim();
            String newPath = mRename.group(2).trim();
            AiMessage msg = new AiMessage("assistant", "Rename file: " + oldPath + " -> " + newPath);
            msg.setActionType(ActionType.RENAME_FILE);
            msg.setTargetFilePath(oldPath);
            msg.setNewFilePath(newPath);
            msg.setApprovalStatus(ApprovalStatus.PENDING);
            mAdapter.addMessage(msg);
        }

        // DELETE FILE
        Pattern deletePat = Pattern.compile("\\[DELETE FILE:\\s*([^\\]]+)\\]");
        Matcher mDelete = deletePat.matcher(rawText);
        while (mDelete.find()) {
            hasAction = true;
            String path = mDelete.group(1).trim();
            AiMessage msg = new AiMessage("assistant", "Delete file: " + path);
            msg.setActionType(ActionType.DELETE_FILE);
            msg.setTargetFilePath(path);
            msg.setApprovalStatus(ApprovalStatus.PENDING);
            mAdapter.addMessage(msg);
        }

        if (planText == null && !hasAction) {
            AiMessage plainMsg = new AiMessage("assistant", rawText);
            mAdapter.addMessage(plainMsg);
        }

        scrollToBottom();
        saveChatHistory();
    }

    private String stripCodeFence(String code) {
        if (code.startsWith("```")) {
            int firstNewline = code.indexOf("\n");
            if (firstNewline != -1) {
                code = code.substring(firstNewline + 1);
            }
        }
        if (code.endsWith("```")) {
            code = code.substring(0, code.length() - 3);
        }
        return code.trim();
    }

    @Override
    public void onApprove(AiMessage message, int position) {
        if (mProjectRoot == null || !mProjectRoot.exists()) {
            Toast.makeText(this, "Project directory not found", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 1. Take snapshot backup before applying modification
            BackupManager.createSnapshot(mProjectRoot, "AI " + message.getActionType() + " " + message.getTargetFilePath());

            ActionType type = message.getActionType();
            String relPath = message.getTargetFilePath();
            File targetFile = new File(mProjectRoot, relPath);

            if (type == ActionType.CREATE_FILE || type == ActionType.WRITE_FILE) {
                if (targetFile.getParentFile() != null) {
                    targetFile.getParentFile().mkdirs();
                }
                FileUtils.writeFile(targetFile, message.getProposedCode());
            } else if (type == ActionType.RENAME_FILE) {
                File dest = new File(mProjectRoot, message.getNewFilePath());
                if (dest.getParentFile() != null) {
                    dest.getParentFile().mkdirs();
                }
                targetFile.renameTo(dest);
            } else if (type == ActionType.DELETE_FILE) {
                if (targetFile.exists()) {
                    targetFile.delete();
                }
            }

            // Permanently update message status to APPROVED
            message.setApprovalStatus(ApprovalStatus.APPROVED);
            mAdapter.notifyItemChanged(position);

            // Persist changes so user never has to approve again
            saveChatHistory();

            Toast.makeText(this, "Applied: " + relPath, Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Error applying action: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onReject(AiMessage message, int position) {
        message.setApprovalStatus(ApprovalStatus.REJECTED);
        mAdapter.notifyItemChanged(position);
        saveChatHistory();
        Toast.makeText(this, "Rejected changes", Toast.LENGTH_SHORT).show();
    }

    private void showUndoAiDialog() {
        if (mProjectRoot == null) return;
        List<File> snapshots = BackupManager.listSnapshots(mProjectRoot);
        if (snapshots.isEmpty()) {
            Toast.makeText(this, "No previous AI snapshots found", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Undo AI Changes?")
                .setMessage("Restore project to the snapshot before the last AI modifications? (" + snapshots.get(0).getName() + ")")
                .setPositiveButton("Restore", (dialog, which) -> {
                    boolean restored = BackupManager.restoreLatestSnapshot(mProjectRoot);
                    if (restored) {
                        Toast.makeText(this, "Project restored to previous version!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to restore snapshot", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void scrollToBottom() {
        if (mAdapter.getItemCount() > 0) {
            mRvChat.smoothScrollToPosition(mAdapter.getItemCount() - 1);
        }
    }
}
