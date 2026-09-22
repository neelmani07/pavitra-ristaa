package com.pavitraristaa.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateUploadUrlRequest(
        @NotBlank @Size(max = 255) String filename,
        @NotBlank @Size(max = 100) String mimeType,
        @NotNull @Positive Long fileSizeBytes,
        @Size(max = 50) String purpose
) {
}
