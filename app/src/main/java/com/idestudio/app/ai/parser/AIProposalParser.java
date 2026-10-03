package com.idestudio.app.ai.parser;

import com.idestudio.app.ai.approval.AIProposal;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AIProposalParser {

    private static final Pattern PROPOSAL_PATTERN = Pattern.compile(
            "<proposal\\s+type=[\"']([^\"']+)[\"']\\s+path=[\"']([^\"']+)[\"']>(.*?)</proposal>",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    private AIProposalParser() {}

    public static List<AIProposal> parseProposals(String aiResponse) {
        List<AIProposal> list = new ArrayList<>();
        if (aiResponse == null || aiResponse.isEmpty()) {
            return list;
        }

        Matcher matcher = PROPOSAL_PATTERN.matcher(aiResponse);
        while (matcher.find()) {
            String typeStr = matcher.group(1);
            String path = matcher.group(2);
            String content = matcher.group(3);

            AIProposal.OperationType type = AIProposal.OperationType.fromString(typeStr);
            list.add(new AIProposal(type, path.trim(), content.trim()));
        }

        return list;
    }

    /**
     * Removes proposal tags to produce clean conversational text for the chat bubble.
     */
    public static String cleanConversationalText(String aiResponse) {
        if (aiResponse == null) return "";
        return PROPOSAL_PATTERN.matcher(aiResponse).replaceAll("").trim();
    }
}
