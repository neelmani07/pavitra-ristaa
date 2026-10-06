package com.pavitraristaa.meta.service;

import com.pavitraristaa.auth.entity.DevicePlatform;
import com.pavitraristaa.auth.entity.OtpPurpose;
import com.pavitraristaa.master.service.MasterDataService;
import com.pavitraristaa.meta.dto.OptionsResponse;
import com.pavitraristaa.profile.entity.PhotoType;
import com.pavitraristaa.profile.entity.PhotoVisibility;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class OptionsService {

    // Large and open-ended (a client fills these from an external source and sends names), so they are not
    // enumerated here - and listing every user-created city would grow this response without bound.
    private static final Set<String> GEOGRAPHY_CATEGORIES = Set.of("COUNTRY", "STATE", "CITY");
    private static final List<String> CUSTOM_VALUE_CATEGORIES = List.of("SPIRITUAL_COMMUNITY");

    private final MasterDataService masterDataService;

    public OptionsService(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    public OptionsResponse options() {
        Map<String, List<String>> enums = new LinkedHashMap<>();
        enums.put("otpPurposes", names(OtpPurpose.values()));
        enums.put("devicePlatforms", names(DevicePlatform.values()));
        enums.put("photoTypes", names(PhotoType.values()));
        enums.put("photoVisibilities", names(PhotoVisibility.values()));
        return new OptionsResponse(enums, masterDataService.allValuesByCategory(GEOGRAPHY_CATEGORIES), CUSTOM_VALUE_CATEGORIES);
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }
}
