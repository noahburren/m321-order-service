package ch.tbz.m321orderservice.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderV2Request(@NotBlank String productCode, @Min(1) int amount) {
}
