package com.idestudio.app.ui.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private ImageButton btnSend;
    private ProgressBar pbLoading;

    private final List<ChatMessage> messageList = new ArrayList<>();
    private OllamaCloudClient aiClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_chat);

        aiClient = OllamaCloudClient.getInstance(this);

        initViews();
        addWelcomeMessage();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar_ai_chat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Build AI Assistant");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvChat = findViewById(R.id.rv_chat_messages);
        etInput = findViewById(R.id.et_chat_input);
        btnSend = findViewById(R.id.btn_chat_send);
        pbLoading = findViewById(R.id.pb_chat_loading);

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
                ChatMessage errorMsg = new ChatMessage("assistant", "⚠️ Error: " + errorMessage + "\n\nTap the ⚙ icon at top right to check your API Key and server URL in Settings.");
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Settings")
                .setIcon(android.R.drawable.ic_menu_preferences)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 1) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
