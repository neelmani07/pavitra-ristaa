package com.pavitraristaa.discovery.repository;

import com.pavitraristaa.auth.entity.UserAccount;
import com.pavitraristaa.profile.entity.ProfileStatus;
import com.pavitraristaa.profile.entity.SpiritualProfile;
import com.pavitraristaa.profile.entity.UserProfile;
import com.pavitraristaa.relationship.entity.UserRelationshipMode;
import com.pavitraristaa.trust.entity.Block;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Builds the browse/search predicate one clause at a time, adding a clause only when its filter is actually
 * given. This is what lets every filter be optional without ever binding an untyped SQL null: an unused filter
 * contributes no predicate and no parameter at all, rather than a "(:param is null or ...)" branch - which is
 * also what triggered a real Hibernate 6 + pgjdbc bug here ("could not determine data type of parameter", then
 * "cannot cast type bytea to date" once worked around with an explicit JPQL cast). Specifications sidestep the
 * whole class of problem.
 */
public final class DiscoverySpecifications {

    private DiscoverySpecifications() {
    }

    public static Specification<UserProfile> discoverableBy(UserAccount self) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("profileStatus"), ProfileStatus.ACTIVE),
                cb.isFalse(root.get("deleted")),
                cb.notEqual(root.get("user"), self)
        );
    }

    public static Specification<UserProfile> hasAnyRelationshipMode(Collection<String> modeCodes) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var urm = subquery.from(UserRelationshipMode.class);
            subquery.select(urm.get("id"))
                    .where(cb.equal(urm.get("user"), root.get("user")), urm.get("relationshipMode").get("code").in(modeCodes));
            return cb.exists(subquery);
        };
    }

    public static Specification<UserProfile> notBlockedEitherWayWith(UserAccount self) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            var block = subquery.from(Block.class);
            subquery.select(block.get("id")).where(cb.or(
                    cb.and(cb.equal(block.get("blocker"), self), cb.equal(block.get("blocked"), root.get("user"))),
                    cb.and(cb.equal(block.get("blocker"), root.get("user")), cb.equal(block.get("blocked"), self))
            ));
            return cb.not(cb.exists(subquery));
        };
    }

    public static Specification<UserProfile> countryIs(Long countryId) {
        return countryId == null ? null : (root, query, cb) -> cb.equal(root.get("country").get("id"), countryId);
    }

    public static Specification<UserProfile> stateIs(Long stateId) {
        return stateId == null ? null : (root, query, cb) -> cb.equal(root.get("state").get("id"), stateId);
    }

    public static Specification<UserProfile> cityIs(Long cityId) {
        return cityId == null ? null : (root, query, cb) -> cb.equal(root.get("city").get("id"), cityId);
    }

    public static Specification<UserProfile> bornOnOrBefore(LocalDate date) {
        return date == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dateOfBirth"), date);
    }

    public static Specification<UserProfile> bornOnOrAfter(LocalDate date) {
        return date == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dateOfBirth"), date);
    }

    public static Specification<UserProfile> textMatches(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String pattern = likePattern(query);
        return (root, cq, cb) -> cb.or(
                cb.like(cb.lower(root.get("displayName")), pattern),
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("headline")), pattern)
        );
    }

    public static Specification<UserProfile> spiritualFieldContains(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String pattern = likePattern(value);
        return (root, cq, cb) -> {
            var spiritualProfile = root.join("spiritualProfile", jakarta.persistence.criteria.JoinType.LEFT);
            return cb.like(cb.lower(spiritualProfile.<String>get(field)), pattern);
        };
    }

    private static String likePattern(String text) {
        return "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
