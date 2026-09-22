package com.pavitraristaa.connections.entity;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.relationship.entity.RelationshipMode;
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

/** userA/userB are stored with userA.id &lt; userB.id (DB constraint) - callers must not assume "A" means "self". */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "match")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_a_id", nullable = false)
    private UserAccount userA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_b_id", nullable = false)
    private UserAccount userB;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relationship_mode_id", nullable = false)
    private RelationshipMode relationshipMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_interest_id")
    private Interest sourceInterest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MatchStatus status;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt;

    @Column(name = "unmatched_at")
    private Instant unmatchedAt;

    public UserAccount other(UserAccount self) {
        return userA.getId().equals(self.getId()) ? userB : userA;
    }

    public boolean involves(UserAccount user) {
        return userA.getId().equals(user.getId()) || userB.getId().equals(user.getId());
    }
}
