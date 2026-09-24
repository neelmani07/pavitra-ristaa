package com.pavitraristaa.admin.entity;

import com.pavitraristaa.auth.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A moderation action taken against a user (and optionally a specific profile/media/message they own).
 * target_profile_id / target_media_id / target_message_id are kept as plain nullable Long columns rather than
 * @ManyToOne relationships - admin already depends on profile and trust, but not on media or messaging, and a
 * moderation row referencing a specific message would otherwise pull messaging into admin's dependency graph.
 * The report that triggered this action (if any) is likewise a plain Long, since Report lives in trust and this
 * keeps Moderation from needing a bidirectional link.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "moderation")
public class Moderation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private UserAccount targetUser;

    @Column(name = "target_profile_id")
    private Long targetProfileId;

    @Column(name = "target_media_id")
    private Long targetMediaId;

    @Column(name = "target_message_id")
    private Long targetMessageId;

    @Column(name = "source_report_id")
    private Long sourceReportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ModerationAction action;

    @Column(columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModerationStatus status = ModerationStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_user_id")
    private UserAccount moderator;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
