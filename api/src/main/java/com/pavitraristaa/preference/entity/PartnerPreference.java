package com.pavitraristaa.preference.entity;

import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.profile.entity.UserProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "partner_preference")
public class PartnerPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false, unique = true)
    private UserProfile profile;

    @Column(name = "min_age")
    private Short minAge;

    @Column(name = "max_age")
    private Short maxAge;

    @Column(name = "min_height_cm")
    private Short minHeightCm;

    @Column(name = "max_height_cm")
    private Short maxHeightCm;

    @Column(name = "preferred_gender", length = 30)
    private String preferredGender;

    @Column(name = "min_income", precision = 14, scale = 2)
    private BigDecimal minIncome;

    @Column(name = "max_income", precision = 14, scale = 2)
    private BigDecimal maxIncome;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_marital_status_id")
    private MasterValue preferredMaritalStatus;

    @Column(name = "location_radius_km")
    private Integer locationRadiusKm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "preference", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartnerPreferenceValue> values = new ArrayList<>();
}
