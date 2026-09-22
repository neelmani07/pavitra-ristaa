package com.pavitraristaa.media.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CompleteUploadRequest(
        @NotNull UUID mediaFileId,
        @Size(max = 128) String checksum
) {
}
