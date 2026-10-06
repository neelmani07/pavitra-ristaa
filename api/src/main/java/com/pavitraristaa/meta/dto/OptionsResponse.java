package com.pavitraristaa.meta.dto;

import com.pavitraristaa.master.dto.MasterValueResponse;
import java.util.List;
import java.util.Map;

/**
 * Everything a client needs to render its dropdowns and send valid values, in one call.
 *
 * @param enums                 fixed value sets the API accepts as strings (e.g. otpPurposes), keyed by name
 * @param masterData            admin-managed lists (diet, interests, spiritual communities, ...) keyed by category
 *                              code; country/state/city are deliberately absent - send those as plain names
 * @param customValueCategories master-data categories where the user may send a value that is not in the list and
 *                              it is accepted (kept for review, not shown to other users' dropdowns)
 */
public record OptionsResponse(
        Map<String, List<String>> enums,
        Map<String, List<MasterValueResponse>> masterData,
        List<String> customValueCategories
) {
}
