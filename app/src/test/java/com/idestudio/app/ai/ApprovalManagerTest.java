package com.idestudio.app.ai;

import com.idestudio.app.ai.approval.AIProposal;
import com.idestudio.app.ai.approval.ApprovalManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApprovalManagerTest {

    private File testRoot;

    @Before
    public void setUp() {
        testRoot = new File("/storage/emulated/0/test-folder/ide-studio/projects/AIApprovalTest");
        if (testRoot.exists()) deleteRecursive(testRoot);
        testRoot.mkdirs();
    }

    @After
    public void tearDown() {
        if (testRoot != null && testRoot.exists()) deleteRecursive(testRoot);
    }

    @Test
    public void testApplyCreateFileProposal() throws IOException {
        AIProposal proposal = new AIProposal(
                AIProposal.OperationType.CREATE_FILE,
                "app/src/main/java/Helper.java",
                "public class Helper {}"
        );

        assertTrue(proposal.isPending());
        ApprovalManager.applyProposal(testRoot, proposal);

        assertEquals(AIProposal.ApprovalStatus.APPROVED, proposal.getStatus());
        File created = new File(testRoot, "app/src/main/java/Helper.java");
        assertTrue(created.exists());
    }

    @Test
    public void testRejectProposalDoesNotTouchDisk() {
        AIProposal proposal = new AIProposal(
                AIProposal.OperationType.CREATE_FILE,
                "app/src/main/java/Unapproved.java",
                "public class Unapproved {}"
        );

        ApprovalManager.rejectProposal(proposal);

        assertEquals(AIProposal.ApprovalStatus.REJECTED, proposal.getStatus());
        File file = new File(testRoot, "app/src/main/java/Unapproved.java");
        assertFalse(file.exists());
    }

    @Test(expected = SecurityException.class)
    public void testPathTraversalInProposalBlocked() throws IOException {
        AIProposal maliciousProposal = new AIProposal(
                AIProposal.OperationType.CREATE_FILE,
                "../../outside.txt",
                "malicious code"
        );

        ApprovalManager.applyProposal(testRoot, maliciousProposal);
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
