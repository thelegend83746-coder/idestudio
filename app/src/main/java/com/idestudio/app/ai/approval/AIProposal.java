package com.idestudio.app.ai.approval;

import java.io.Serializable;
import java.util.UUID;

public class AIProposal implements Serializable {

    public enum OperationType {
        CREATE_FILE("Create File"),
        UPDATE_FILE("Update File"),
        CREATE_FOLDER("Create Folder"),
        RENAME_FOLDER("Rename Folder"),
        DELETE_FILE("Delete File"),
        DELETE_FOLDER("Delete Folder");

        private final String displayName;

        OperationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static OperationType fromString(String val) {
            if (val == null) return UPDATE_FILE;
            String normalized = val.trim().toLowerCase().replace("-", "_").replace(" ", "_");
            switch (normalized) {
                case "create_file": return CREATE_FILE;
                case "update_file": return UPDATE_FILE;
                case "create_folder": return CREATE_FOLDER;
                case "rename_folder": return RENAME_FOLDER;
                case "delete_file": return DELETE_FILE;
                case "delete_folder": return DELETE_FOLDER;
                default: return UPDATE_FILE;
            }
        }
    }

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED
    }

    private final String id;
    private final OperationType type;
    private final String targetPath;
    private final String content;
    private ApprovalStatus status;

    public AIProposal(OperationType type, String targetPath, String content) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.targetPath = targetPath;
        this.content = content != null ? content : "";
        this.status = ApprovalStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public OperationType getType() {
        return type;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getContent() {
        return content;
    }

    public ApprovalStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalStatus status) {
        this.status = status;
    }

    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }
}
