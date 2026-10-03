package com.idestudio.app.security;

import com.idestudio.app.ai.approval.AIProposal;
import com.idestudio.app.ai.approval.ApprovalManager;
import com.idestudio.app.core.constants.AppConstants;
import com.idestudio.app.core.utils.PathSanitizer;
import com.idestudio.app.domain.filesystem.SafeFileOperations;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecurityAuditTest {

    private File testRoot;

    @Before
    public void setUp() {
        testRoot = new File(AppConstants.PROJECTS_DIR, "SecurityAuditSandbox");
        if (testRoot.exists()) deleteRecursive(testRoot);
        testRoot.mkdirs();
    }

    @After
    public void tearDown() {
        if (testRoot != null && testRoot.exists()) {
            deleteRecursive(testRoot);
        }
    }

    @Test
    public void testStrictPathTraversalSanitization() {
        File safeFile = new File(testRoot, "Safe.java");
        assertTrue(PathSanitizer.isPathSafe(testRoot, safeFile));

        File attack1 = new File(testRoot, "../hacked.txt");
        assertFalse(PathSanitizer.isPathSafe(testRoot, attack1));

        File attack2 = new File(testRoot, "sub/../../outside.txt");
        assertFalse(PathSanitizer.isPathSafe(testRoot, attack2));
    }

    @Test(expected = SecurityException.class)
    public void testSafeFileOperationsRejectsTraversal() throws IOException {
        File outside = new File(testRoot, "../../evil.txt");
        SafeFileOperations.createFile(testRoot, outside.getParentFile(), "evil.txt");
    }

    @Test(expected = SecurityException.class)
    public void testApprovalManagerRejectsMaliciousProposalPath() throws IOException {
        AIProposal malicious = new AIProposal(
                AIProposal.OperationType.CREATE_FILE,
                "../../../data/data/com.termux/files/evil.sh",
                "echo hacked"
        );
        ApprovalManager.applyProposal(testRoot, malicious);
    }

    @Test
    public void testAuthoritativeApprovalGateIntegrity() {
        // Pending proposal must never exist on disk
        AIProposal pending = new AIProposal(
                AIProposal.OperationType.CREATE_FILE,
                "app/src/main/java/Pending.java",
                "content"
        );
        assertTrue(pending.isPending());
        File pendingFile = new File(testRoot, "app/src/main/java/Pending.java");
        assertFalse(pendingFile.exists());

        // Reject proposal -> still does not exist on disk
        ApprovalManager.rejectProposal(pending);
        assertEquals(AIProposal.ApprovalStatus.REJECTED, pending.getStatus());
        assertFalse(pendingFile.exists());
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                for (File child : files) deleteRecursive(child);
            }
        }
        f.delete();
    }
}
