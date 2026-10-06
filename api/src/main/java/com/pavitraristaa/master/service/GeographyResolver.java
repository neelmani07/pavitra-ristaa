package com.pavitraristaa.master.service;

import com.pavitraristaa.master.entity.MasterValue;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Country / state / city from the plain strings a frontend sends (its dropdowns come from an external open API,
 * not from our master data). Each level is looked up under its parent - a "Springfield" in one state is a
 * different row from a "Springfield" in another - and created on first use, so a new value from the client's
 * source never has to wait for a data load on our side. Geography rows are created active: they are facts, not
 * moderated content.
 */
@Component
public class GeographyResolver {

    private final MasterNameResolver masterNameResolver;

    public GeographyResolver(MasterNameResolver masterNameResolver) {
        this.masterNameResolver = masterNameResolver;
    }

    public MasterValue country(String name) {
        return masterNameResolver.findOrCreate("COUNTRY", name, "country", true, Map.of());
    }

    public MasterValue state(String name, MasterValue country) {
        Map<String, String> parent = new LinkedHashMap<>();
        if (country != null) {
            parent.put("countryCode", country.getCode());
        }
        return masterNameResolver.findOrCreate("STATE", name, "state", true, parent);
    }

    public MasterValue city(String name, MasterValue state, MasterValue country) {
        Map<String, String> parent = new LinkedHashMap<>();
        if (country != null) {
            parent.put("countryCode", country.getCode());
        }
        if (state != null) {
            parent.put("stateCode", state.getCode());
        }
        return masterNameResolver.findOrCreate("CITY", name, "city", true, parent);
    }
}
