package com.pavitraristaa.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReactionRequest(@NotBlank @Size(max = 30) String reactionCode) {
}
