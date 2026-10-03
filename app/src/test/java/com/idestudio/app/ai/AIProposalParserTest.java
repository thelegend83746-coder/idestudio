package com.idestudio.app.ai;

import com.idestudio.app.ai.approval.AIProposal;
import com.idestudio.app.ai.parser.AIProposalParser;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AIProposalParserTest {

    @Test
    public void testParseSingleProposal() {
        String aiOutput = "Here is the new utility class:\n\n"
                + "<proposal type=\"create_file\" path=\"app/src/main/java/com/demo/MathUtil.java\">\n"
                + "package com.demo;\n"
                + "public class MathUtil {\n"
                + "    public static int add(int a, int b) { return a + b; }\n"
                + "}\n"
                + "</proposal>\n\n"
                + "Let me know if this works!";

        List<AIProposal> proposals = AIProposalParser.parseProposals(aiOutput);
        assertEquals(1, proposals.size());

        AIProposal prop = proposals.get(0);
        assertEquals(AIProposal.OperationType.CREATE_FILE, prop.getType());
        assertEquals("app/src/main/java/com/demo/MathUtil.java", prop.getTargetPath());
        assertTrue(prop.getContent().contains("public static int add"));
        assertTrue(prop.isPending());

        String cleanText = AIProposalParser.cleanConversationalText(aiOutput);
        assertFalse(cleanText.contains("<proposal"));
        assertTrue(cleanText.contains("Here is the new utility class"));
        assertTrue(cleanText.contains("Let me know if this works!"));
    }

    @Test
    public void testParseMultipleProposals() {
        String aiOutput = "I have updated the layout and java code:\n"
                + "<proposal type=\"update_file\" path=\"app/src/main/res/layout/activity_main.xml\">"
                + "<TextView android:text=\"Updated\"/>"
                + "</proposal>\n"
                + "<proposal type=\"delete_file\" path=\"app/src/main/java/OldActivity.java\">"
                + "</proposal>";

        List<AIProposal> proposals = AIProposalParser.parseProposals(aiOutput);
        assertEquals(2, proposals.size());

        assertEquals(AIProposal.OperationType.UPDATE_FILE, proposals.get(0).getType());
        assertEquals(AIProposal.OperationType.DELETE_FILE, proposals.get(1).getType());
    }
}
