package com.pavitraristaa.discovery.entity;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.profile.entity.UserProfile;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Append-only view history, like login_history - a profile can be viewed by the same viewer more than once. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "profile_view")
public class ProfileView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viewer_user_id", nullable = false)
    private UserAccount viewer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "viewed_profile_id", nullable = false)
    private UserProfile viewedProfile;

    @jakarta.persistence.Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;
}
