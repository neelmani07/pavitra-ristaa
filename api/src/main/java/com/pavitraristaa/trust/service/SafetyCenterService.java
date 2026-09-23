package com.pavitraristaa.trust.service;

import com.pavitraristaa.trust.dto.SafetyCenterResponse;
import com.pavitraristaa.trust.dto.SafetyCenterResponse.SafetyTip;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Static, versioned-in-code content - matching /help and /legal/{documentType} (Support module), there is no
 * safety_center table in the approved schema, and this kind of copy does not need to be database-backed.
 */
@Service
public class SafetyCenterService {

    private static final SafetyCenterResponse CONTENT = new SafetyCenterResponse(List.of(
            new SafetyTip("Meet in a public place", "For your first few meetings, choose a public place with other people around."),
            new SafetyTip("Tell a friend", "Share your plans, location and who you're meeting with a friend or family member."),
            new SafetyTip("Video chat first", "Consider a video call before meeting in person to confirm who you're talking to."),
            new SafetyTip("Never send money", "Pavitra Ristaa will never ask you to send money to another member. Don't send money to anyone you haven't met in person."),
            new SafetyTip("Report and block", "If someone makes you uncomfortable, use Report or Block - you don't need their permission or an explanation."),
            new SafetyTip("Keep personal details private", "Avoid sharing your home address, workplace or financial details until you trust someone.")
    ));

    public SafetyCenterResponse content() {
        return CONTENT;
    }
}
