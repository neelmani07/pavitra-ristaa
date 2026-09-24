package com.pavitraristaa.preference.service;

import com.pavitraristaa.common.util.AgeCalculator;
import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.preference.entity.PartnerPreference;
import com.pavitraristaa.preference.entity.PartnerPreferenceValue;
import com.pavitraristaa.preference.entity.PreferenceType;
import com.pavitraristaa.preference.repository.PartnerPreferenceRepository;
import com.pavitraristaa.profile.entity.UserProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * A first-pass heuristic (0-100), not a machine-learned model. Every criterion is skipped, not penalized, when
 * either side's relevant data is missing, and deliberately does not consider height, religion or marital
 * status: the schema records those only as partner PREFERENCES (partner_preference.min/max_height_cm,
 * partner_preference_value RELIGION/MARITAL_STATUS), never as a value on the person's own profile, so there is
 * nothing on either side to compare a preference against. Uses the same criteria regardless of relationship
 * mode: Dating, Friendship and Marriage share one compatibility model.
 *
 * Shared by connections.MatchService (matched-pair compatibility) and discovery's recommendation ranking -
 * living in preference rather than either of those keeps it reachable from both without either depending on the
 * other: preference already sits below both in the module graph (profile, master, auth).
 */
@Service
public class CompatibilityScorer {

    private final PartnerPreferenceRepository partnerPreferenceRepository;

    public CompatibilityScorer(PartnerPreferenceRepository partnerPreferenceRepository) {
        this.partnerPreferenceRepository = partnerPreferenceRepository;
    }

    public record Factor(String key, String label, boolean matched, String detail) {
    }

    public record Score(int score, List<Factor> factors) {
    }

    public Score score(UserProfile a, UserProfile b) {
        List<Factor> factors = new ArrayList<>();
        int earned = 0;
        int possible = 0;

        Optional<PartnerPreference> prefA = partnerPreferenceRepository.findByProfile(a);
        Optional<PartnerPreference> prefB = partnerPreferenceRepository.findByProfile(b);

        Integer ageA = AgeCalculator.fromDateOfBirth(a.getDateOfBirth());
        Integer ageB = AgeCalculator.fromDateOfBirth(b.getDateOfBirth());
        if (prefA.isPresent() && ageB != null) {
            possible += 10;
            boolean inRange = inRange(ageB, prefA.get().getMinAge(), prefA.get().getMaxAge());
            earned += inRange ? 10 : 0;
            factors.add(new Factor("AGE_A_TO_B", "Their age fits your preference", inRange, ageB + " years"));
        }
        if (prefB.isPresent() && ageA != null) {
            possible += 10;
            boolean inRange = inRange(ageA, prefB.get().getMinAge(), prefB.get().getMaxAge());
            earned += inRange ? 10 : 0;
            factors.add(new Factor("AGE_B_TO_A", "Your age fits their preference", inRange, ageA + " years"));
        }

        earned += lifestyleFactor(prefA, prefB, factors, PreferenceType.DIET,
                a.getLifestyle() == null ? null : a.getLifestyle().getDiet(),
                b.getLifestyle() == null ? null : b.getLifestyle().getDiet(), "DIET");
        possible += lifestylePossible(prefA, prefB,
                a.getLifestyle() == null ? null : a.getLifestyle().getDiet(),
                b.getLifestyle() == null ? null : b.getLifestyle().getDiet());
        earned += lifestyleFactor(prefA, prefB, factors, PreferenceType.SMOKING,
                a.getLifestyle() == null ? null : a.getLifestyle().getSmoking(),
                b.getLifestyle() == null ? null : b.getLifestyle().getSmoking(), "SMOKING");
        possible += lifestylePossible(prefA, prefB,
                a.getLifestyle() == null ? null : a.getLifestyle().getSmoking(),
                b.getLifestyle() == null ? null : b.getLifestyle().getSmoking());
        earned += lifestyleFactor(prefA, prefB, factors, PreferenceType.DRINKING,
                a.getLifestyle() == null ? null : a.getLifestyle().getDrinking(),
                b.getLifestyle() == null ? null : b.getLifestyle().getDrinking(), "DRINKING");
        possible += lifestylePossible(prefA, prefB,
                a.getLifestyle() == null ? null : a.getLifestyle().getDrinking(),
                b.getLifestyle() == null ? null : b.getLifestyle().getDrinking());

        Set<Long> interestsA = idsOf(a.getInterests().stream().map(i -> i.getInterest()).toList());
        Set<Long> interestsB = idsOf(b.getInterests().stream().map(i -> i.getInterest()).toList());
        if (!interestsA.isEmpty() && !interestsB.isEmpty()) {
            possible += 20;
            long shared = interestsA.stream().filter(interestsB::contains).count();
            int points = (int) Math.min(20, shared * 4);
            earned += points;
            factors.add(new Factor("SHARED_INTERESTS", "Shared interests", shared > 0, shared + " in common"));
        }

        Set<Long> hobbiesA = idsOf(a.getHobbies().stream().map(h -> h.getHobby()).toList());
        Set<Long> hobbiesB = idsOf(b.getHobbies().stream().map(h -> h.getHobby()).toList());
        if (!hobbiesA.isEmpty() && !hobbiesB.isEmpty()) {
            possible += 20;
            long shared = hobbiesA.stream().filter(hobbiesB::contains).count();
            int points = (int) Math.min(20, shared * 4);
            earned += points;
            factors.add(new Factor("SHARED_HOBBIES", "Shared hobbies", shared > 0, shared + " in common"));
        }

        if (a.getCity() != null && b.getCity() != null) {
            possible += 15;
            boolean sameCity = a.getCity().getId().equals(b.getCity().getId());
            earned += sameCity ? 15 : 0;
            factors.add(new Factor("LOCATION", "Same city", sameCity,
                    sameCity ? "Both in " + a.getCity().getName() : "Different cities"));
        }

        int score = possible == 0 ? 50 : Math.round(100f * earned / possible);
        return new Score(score, factors);
    }

    private int lifestylePossible(Optional<PartnerPreference> prefA, Optional<PartnerPreference> prefB,
            MasterValue actualA, MasterValue actualB) {
        int possible = 0;
        if (prefA.isPresent() && actualB != null) {
            possible += 5;
        }
        if (prefB.isPresent() && actualA != null) {
            possible += 5;
        }
        return possible;
    }

    private int lifestyleFactor(
            Optional<PartnerPreference> prefA, Optional<PartnerPreference> prefB,
            List<Factor> factors, PreferenceType type,
            MasterValue actualA, MasterValue actualB, String label
    ) {
        int earned = 0;
        if (prefA.isPresent() && actualB != null) {
            boolean matched = preferredValues(prefA.get(), type).contains(actualB.getId());
            earned += matched ? 5 : 0;
            factors.add(new Factor(label + "_A_TO_B", "Their " + label.toLowerCase() + " fits your preference",
                    matched, actualB.getName()));
        }
        if (prefB.isPresent() && actualA != null) {
            boolean matched = preferredValues(prefB.get(), type).contains(actualA.getId());
            earned += matched ? 5 : 0;
            factors.add(new Factor(label + "_B_TO_A", "Your " + label.toLowerCase() + " fits their preference",
                    matched, actualA.getName()));
        }
        return earned;
    }

    private Set<Long> preferredValues(PartnerPreference preference, PreferenceType type) {
        return preference.getValues().stream()
                .filter(value -> value.getPreferenceType() == type)
                .map(PartnerPreferenceValue::getMasterValue)
                .map(MasterValue::getId)
                .collect(Collectors.toSet());
    }

    private boolean inRange(int value, Short min, Short max) {
        if (min != null && value < min) {
            return false;
        }
        return max == null || value <= max;
    }

    private Set<Long> idsOf(List<MasterValue> values) {
        return values.stream().map(MasterValue::getId).collect(Collectors.toSet());
    }
}
