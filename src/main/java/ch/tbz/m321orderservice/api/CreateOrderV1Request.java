package ch.tbz.m321orderservice.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateOrderV1Request(@NotBlank String product, @Min(1) int quantity) {
}
