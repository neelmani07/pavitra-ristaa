package com.pavitraristaa.support.service;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.support.dto.HelpResponse;
import com.pavitraristaa.support.dto.HelpResponse.HelpTopic;
import com.pavitraristaa.support.dto.LegalDocumentResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Static, versioned-in-code content, matching /safety-center - no help_content or legal_document table in the
 * approved schema, and this kind of copy does not need to be database-backed.
 *
 * IMPORTANT: the legal document bodies below are placeholders only ("draft pending legal review"), not real
 * Terms of Service / Privacy Policy text. Publishing them as-is would be legally meaningless at best and
 * misleading to users at worst - they exist so the endpoint has a real shape to build the FE against, and must
 * be replaced with counsel-reviewed text before this reaches real users.
 */
@Service
public class HelpContentService {

    private static final HelpResponse HELP = new HelpResponse(List.of(
            new HelpTopic("Getting started", "Complete your profile, add photos, and set your relationship mode to start discovering matches."),
            new HelpTopic("Managing your subscription", "You can view and manage your subscription from Settings once payments are available."),
            new HelpTopic("Privacy and safety", "Visit the Safety Center for tips, and use Report or Block if someone makes you uncomfortable."),
            new HelpTopic("Account issues", "If you're having trouble signing in or verifying your account, create a support ticket and we'll help.")
    ));

    private static final Map<String, String> DOCUMENT_TITLES = Map.of(
            "TERMS", "Terms of Service",
            "PRIVACY_POLICY", "Privacy Policy",
            "COMMUNITY_GUIDELINES", "Community Guidelines",
            "REFUND_POLICY", "Refund Policy",
            "COOKIE_POLICY", "Cookie Policy"
    );

    public HelpResponse help() {
        return HELP;
    }

    public LegalDocumentResponse legalDocument(String documentType) {
        String type = documentType == null ? "" : documentType.trim().toUpperCase(Locale.ROOT);
        String title = DOCUMENT_TITLES.get(type);
        if (title == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Unknown legal document type", Map.of("documentType", documentType));
        }
        return new LegalDocumentResponse(
                type, title,
                "This is a placeholder for the " + title + ". Final text is pending legal review and is not yet published.",
                "draft"
        );
    }
}
