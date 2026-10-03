package com.idestudio.app.ai.approval;

import com.idestudio.app.core.utils.PathSanitizer;
import com.idestudio.app.domain.filesystem.SafeFileOperations;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public final class ApprovalManager {

    private ApprovalManager() {}

    /**
     * Authoritatively executes an approved proposal on disk.
     */
    public static void applyProposal(File projectRoot, AIProposal proposal) throws IOException {
        if (proposal.getStatus() != AIProposal.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Proposal has already been processed with status: " + proposal.getStatus());
        }

        File target = new File(projectRoot, proposal.getTargetPath());
        if (!PathSanitizer.isPathSafe(projectRoot, target)) {
            throw new SecurityException("Path traversal attempt blocked in proposal execution: " + proposal.getTargetPath());
        }

        switch (proposal.getType()) {
            case CREATE_FILE:
                File parent = target.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                writeFile(target, proposal.getContent());
                break;

            case UPDATE_FILE:
                if (!target.exists()) {
                    target.getParentFile().mkdirs();
                }
                writeFile(target, proposal.getContent());
                break;

            case CREATE_FOLDER:
                if (!target.exists()) {
                    target.mkdirs();
                }
                break;

            case RENAME_FOLDER:
                SafeFileOperations.rename(projectRoot, target, proposal.getContent());
                break;

            case DELETE_FILE:
            case DELETE_FOLDER:
                if (target.exists()) {
                    SafeFileOperations.delete(projectRoot, target);
                }
                break;
        }

        proposal.setStatus(AIProposal.ApprovalStatus.APPROVED);
    }

    /**
     * Rejects a proposal, ensuring no file operations touch disk.
     */
    public static void rejectProposal(AIProposal proposal) {
        proposal.setStatus(AIProposal.ApprovalStatus.REJECTED);
    }

    private static void writeFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(content);
            writer.flush();
        }
    }
}
