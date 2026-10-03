package com.idestudio.app.data.models;

import com.idestudio.app.ai.approval.AIProposal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatMessage implements Serializable {

    private final String id;
    private final boolean isUser;
    private final String role;
    private final String text;
    private final long timestamp;
    private final List<AIProposal> proposals;

    public ChatMessage(boolean isUser, String text) {
        this(isUser ? "user" : "assistant", text);
    }

    public ChatMessage(String role, String text) {
        this(role, text, new ArrayList<>());
    }

    public ChatMessage(boolean isUser, String text, List<AIProposal> proposals) {
        this(isUser ? "user" : "assistant", text, proposals);
    }

    public ChatMessage(String role, String text, List<AIProposal> proposals) {
        this.id = UUID.randomUUID().toString();
        this.role = role != null ? role : "user";
        this.isUser = "user".equalsIgnoreCase(this.role);
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.proposals = proposals != null ? proposals : new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public boolean isUser() {
        return isUser;
    }

    public String getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public String getContent() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public List<AIProposal> getProposals() {
        return proposals;
    }

    public boolean hasProposals() {
        return proposals != null && !proposals.isEmpty();
    }
}
