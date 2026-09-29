package com.ide.studio.model;

import java.util.UUID;

public class AiMessage {
    private String id;
    private String role; // "user" or "assistant"
    private String content;
    private long timestamp;
    private String planText;
    private ActionType actionType;
    private String targetFilePath;
    private String proposedCode;
    private String newFilePath;
    private ApprovalStatus approvalStatus;
    private boolean isInvalidXml;

    public AiMessage(String role, String content) {
        this.id = UUID.randomUUID().toString();
        this.role = role;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.actionType = ActionType.NONE;
        this.approvalStatus = ApprovalStatus.PENDING;
    }

    public AiMessage(String id, String role, String content, long timestamp, String planText,
                     ActionType actionType, String targetFilePath, String proposedCode,
                     String newFilePath, ApprovalStatus approvalStatus, boolean isInvalidXml) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.role = role;
        this.content = content;
        this.timestamp = timestamp;
        this.planText = planText;
        this.actionType = actionType != null ? actionType : ActionType.NONE;
        this.targetFilePath = targetFilePath;
        this.proposedCode = proposedCode;
        this.newFilePath = newFilePath;
        this.approvalStatus = approvalStatus != null ? approvalStatus : ApprovalStatus.PENDING;
        this.isInvalidXml = isInvalidXml;
    }

    public String getId() { return id; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public String getPlanText() { return planText; }
    public void setPlanText(String planText) { this.planText = planText; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType actionType) { this.actionType = actionType; }
    public String getTargetFilePath() { return targetFilePath; }
    public void setTargetFilePath(String targetFilePath) { this.targetFilePath = targetFilePath; }
    public String getProposedCode() { return proposedCode; }
    public void setProposedCode(String proposedCode) { this.proposedCode = proposedCode; }
    public String getNewFilePath() { return newFilePath; }
    public void setNewFilePath(String newFilePath) { this.newFilePath = newFilePath; }
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    public boolean isInvalidXml() { return isInvalidXml; }
    public void setInvalidXml(boolean invalidXml) { isInvalidXml = invalidXml; }
}
