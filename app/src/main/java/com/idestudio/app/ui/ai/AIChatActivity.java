package com.idestudio.app.ui.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.idestudio.app.R;
import com.idestudio.app.ai.client.OllamaCloudClient;
import com.idestudio.app.data.models.ChatMessage;
import com.idestudio.app.ui.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Coding Assistant chat screen supporting both local Ollama and Cloud AI providers.
 */
public class AIChatActivity extends AppCompatActivity {

    private RecyclerView rvChat;
    private ChatMessageAdapter adapter;
    private EditText etInput;
    private FloatingActionButton btnSend;
    private ProgressBar pbLoading;
    private TextView tvActiveModel;

    private final List<ChatMessage> messageList = new ArrayList<>();
    private OllamaCloudClient aiClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        aiClient = OllamaCloudClient.getInstance(this);

        initViews();
        addWelcomeMessage();

        if (getIntent() != null && getIntent().hasExtra("initial_prompt")) {
            String initial = getIntent().getStringExtra("initial_prompt");
            if (initial != null && !initial.trim().isEmpty() && etInput != null) {
                etInput.setText(initial);
                etInput.setSelection(initial.length());
            }
        }
    }

    private void initViews() {
        View btnBack = findViewById(R.id.btn_back_ai);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvActiveModel = findViewById(R.id.tv_active_model_tag);
        if (tvActiveModel != null) {
            tvActiveModel.setText(aiClient.getModel());
            tvActiveModel.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        }

        rvChat = findViewById(R.id.recycler_chat_messages);
        etInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_send_chat);
        pbLoading = findViewById(R.id.progress_ai_generating);

        adapter = new ChatMessageAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvChat.setLayoutManager(layoutManager);
        rvChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void addWelcomeMessage() {
        if (messageList.isEmpty()) {
            messageList.add(new ChatMessage("assistant",
                    "Hello! I am your AI coding assistant. Ask me to generate Android layouts, activities, debug Java code, or build app features.\n\nTip: You can configure your Ollama endpoint or Cloud API Key in Settings (⚙)."));
            adapter.notifyItemInserted(0);
        }
    }

    private void sendMessage() {
        String text = etInput.getText().toString().trim();
        if (text.isEmpty()) return;

        ChatMessage userMsg = new ChatMessage("user", text);
        messageList.add(userMsg);
        adapter.notifyItemInserted(messageList.size() - 1);
        rvChat.smoothScrollToPosition(messageList.size() - 1);

        etInput.setText("");
        setInputEnabled(false);

        String systemPrompt = "You are an expert Android developer assistant inside IDE Studio. Help the user write clean Java and XML code for their Android applications. Always provide accurate code snippets.";

        aiClient.sendMessage(systemPrompt, text, new OllamaCloudClient.AIResponseCallback() {
            @Override
            public void onSuccess(String responseText) {
                setInputEnabled(true);
                ChatMessage assistantMsg = new ChatMessage("assistant", responseText);
                messageList.add(assistantMsg);
                adapter.notifyItemInserted(messageList.size() - 1);
                rvChat.smoothScrollToPosition(messageList.size() - 1);
            }

            @Override
            public void onError(String errorMessage) {
                setInputEnabled(true);
                ChatMessage errorMsg = new ChatMessage("assistant", "⚠️ Error: " + errorMessage + "\n\nTap the model badge at top right to check your API Key and server URL in Settings.");
                messageList.add(errorMsg);
                adapter.notifyItemInserted(messageList.size() - 1);
                rvChat.smoothScrollToPosition(messageList.size() - 1);
            }
        });
    }

    private void setInputEnabled(boolean enabled) {
        etInput.setEnabled(enabled);
        btnSend.setEnabled(enabled);
        if (pbLoading != null) {
            pbLoading.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }
    }
}
