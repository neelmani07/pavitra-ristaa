package com.pavitraristaa.master.service;

import com.pavitraristaa.common.exception.ApiException;
import com.pavitraristaa.common.exception.ErrorCode;
import com.pavitraristaa.master.entity.MasterCategory;
import com.pavitraristaa.master.entity.MasterValue;
import com.pavitraristaa.master.repository.MasterCategoryRepository;
import com.pavitraristaa.master.repository.MasterValueRepository;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns a name a client sent as plain text (a country, a city, a spiritual community) into a master_value row,
 * reusing the one that already exists or creating it when it does not. This is what lets a frontend feed
 * dropdowns from an external source - an open geography API, say - without our table having to be pre-seeded with
 * that source's entire dataset first.
 *
 * <p>Matching is case-insensitive on the name (or the code), so "pune", "Pune" and "PUNE" are one row. It is not
 * accent- or spelling-insensitive: "Pondicherry" and "Puducherry" stay two rows, which is why a client should take
 * its strings from one consistent source rather than letting users type freely.
 */
@Component
public class MasterNameResolver {

    // Letters (any script), combining marks, digits, space and a little punctuation: enough for "Coeur d'Alene",
    // "St. John's", "Delhi (NCR)", "Xi'an", "Pema Chödrön". Not enough for markup or control characters.
    private static final Pattern ALLOWED_NAME = Pattern.compile("^[\\p{L}\\p{M}\\p{N} .,'’()/&-]+$");
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_CODE_LENGTH = 100;

    private final MasterCategoryRepository masterCategoryRepository;
    private final MasterValueRepository masterValueRepository;
    private final ObjectMapper objectMapper;

    public MasterNameResolver(
            MasterCategoryRepository masterCategoryRepository,
            MasterValueRepository masterValueRepository,
            ObjectMapper objectMapper
    ) {
        this.masterCategoryRepository = masterCategoryRepository;
        this.masterValueRepository = masterValueRepository;
        this.objectMapper = objectMapper;
    }

    /** Null for a null or blank name, so an optional field can simply be left out. */
    public MasterValue findOrCreate(
            String categoryCode, String rawName, String fieldName, boolean activeWhenCreated, Map<String, String> parent
    ) {
        String name = clean(rawName, fieldName);
        if (name == null) {
            return null;
        }
        MasterCategory category = masterCategoryRepository.findByCodeIgnoreCaseAndActiveTrue(categoryCode)
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Master category not found"));

        MasterValue existing = firstMatching(
                masterValueRepository.findByCategory_CodeIgnoreCaseAndNameIgnoreCase(categoryCode, name), parent);
        if (existing == null) {
            existing = firstMatching(
                    masterValueRepository.findByCategory_CodeIgnoreCaseAndCodeIgnoreCase(categoryCode, name), parent);
        }
        if (existing != null) {
            return existing;
        }

        String code = newCode(parent, name);
        Map<String, String> metadata = new LinkedHashMap<>(parent);
        metadata.put("source", "user");
        masterValueRepository.insertIfAbsent(category.getId(), code, name, activeWhenCreated, toJson(metadata));
        return masterValueRepository.findByCategory_CodeIgnoreCaseAndCodeIgnoreCase(categoryCode, code).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("master_value " + categoryCode + "/" + code + " vanished after insert"));
    }

    /** Collapses whitespace and validates; null for blank input. */
    static String clean(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim().replaceAll("\\s+", " ");
        boolean hasLetter = name.codePoints().anyMatch(Character::isLetter);
        if (name.length() > MAX_NAME_LENGTH || !hasLetter || !ALLOWED_NAME.matcher(name).matches()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    fieldName + " must be 1-" + MAX_NAME_LENGTH + " characters of letters, digits, spaces and basic punctuation",
                    Map.of("field", fieldName)
            );
        }
        return name;
    }

    /** "Pema Chödrön" -> "PEMA_CHODRON": accents dropped, everything non-alphanumeric collapsed to "_". */
    static String slug(String name) {
        String ascii = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return ascii.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    private String newCode(Map<String, String> parent, String name) {
        String prefix = parent.getOrDefault("stateCode", parent.getOrDefault("countryCode", ""));
        String slug = slug(name);
        if (slug.isEmpty()) {
            // Letters outside A-Z (e.g. a name written only in Devanagari) slug to nothing; fall back to the
            // code points so the row still gets a stable, unique code.
            slug = name.codePoints().mapToObj(Integer::toHexString).reduce("U", (a, b) -> a + "_" + b);
        }
        String code = prefix.isEmpty() ? slug : prefix + "-" + slug;
        return code.length() > MAX_CODE_LENGTH ? code.substring(0, MAX_CODE_LENGTH) : code;
    }

    /** A candidate matches unless it records a parent (countryCode/stateCode) that contradicts the one asked for. */
    private MasterValue firstMatching(List<MasterValue> candidates, Map<String, String> parent) {
        return candidates.stream().filter(candidate -> parentMatches(candidate, parent)).findFirst().orElse(null);
    }

    private boolean parentMatches(MasterValue candidate, Map<String, String> parent) {
        if (parent.isEmpty() || candidate.getMetadata() == null) {
            return true;
        }
        Object parsed = objectMapper.readValue(candidate.getMetadata(), Object.class);
        if (parsed instanceof String encodedTwice) {
            parsed = objectMapper.readValue(encodedTwice, Object.class);
        }
        if (!(parsed instanceof Map<?, ?> recorded)) {
            return true;
        }
        for (Map.Entry<String, String> wanted : parent.entrySet()) {
            Object actual = recorded.get(wanted.getKey());
            if (actual != null && !actual.toString().equalsIgnoreCase(wanted.getValue())) {
                return false;
            }
        }
        return true;
    }

    private String toJson(Map<String, String> metadata) {
        return objectMapper.writeValueAsString(metadata);
    }
}
