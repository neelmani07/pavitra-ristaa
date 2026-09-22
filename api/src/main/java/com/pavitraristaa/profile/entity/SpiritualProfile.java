package com.pavitraristaa.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "spiritual_profile")
public class SpiritualProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private UserProfile profile;

    @Column(name = "spiritual_community")
    private String spiritualCommunity;

    @Column(name = "spiritual_interests")
    private String spiritualInterests;

    private String practices;

    @Column(name = "any_spiritual_profession")
    private String anySpiritualProfession;

    @Column(name = "dream_spiritual_pilgrimage_destination")
    private String dreamSpiritualPilgrimageDestination;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
