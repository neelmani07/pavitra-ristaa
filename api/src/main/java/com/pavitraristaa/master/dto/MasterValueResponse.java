package com.pavitraristaa.master.dto;

public record MasterValueResponse(
        Long id,
        String code,
        String name,
        int displayOrder
) {
}
