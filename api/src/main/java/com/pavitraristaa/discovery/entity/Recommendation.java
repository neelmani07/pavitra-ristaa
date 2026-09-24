package com.pavitraristaa.discovery.entity;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.relationship.entity.RelationshipMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** status has no CHECK constraint in the schema (unlike most status columns here) - left as a plain string,
 *  same reasoning as notification.type: only "ACTIVE" is produced by this pass, contract leaves the rest open. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "recommendation")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommended_profile_id", nullable = false)
    private UserProfile recommendedProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "relationship_mode_id")
    private RelationshipMode relationshipMode;

    @Column(precision = 6, scale = 3)
    private BigDecimal score;

    @Column(length = 255)
    private String reason;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE";
}
