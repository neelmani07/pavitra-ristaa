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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "profile_family")
public class ProfileFamily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private UserProfile profile;

    @Column(name = "family_type", length = 50)
    private String familyType;

    @Column(name = "parents_status", length = 50)
    private String parentsStatus;

    @Column(name = "siblings_count")
    private Short siblingsCount;

    @Column(name = "family_description")
    private String familyDescription;

    @Column(name = "family_values", length = 100)
    private String familyValues;
}
