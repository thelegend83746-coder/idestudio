package com.idestudio.app.data.models;

import com.idestudio.app.ai.approval.AIProposal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatMessage implements Serializable {

    private final String id;
    private final boolean isUser;
    private final String text;
    private final long timestamp;
    private final List<AIProposal> proposals;

    public ChatMessage(boolean isUser, String text) {
        this(isUser, text, new ArrayList<>());
    }

    public ChatMessage(boolean isUser, String text, List<AIProposal> proposals) {
        this.id = UUID.randomUUID().toString();
        this.isUser = isUser;
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

    public String getText() {
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
