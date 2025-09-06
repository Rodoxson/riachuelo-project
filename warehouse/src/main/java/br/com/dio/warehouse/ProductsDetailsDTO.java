package br.com.dio.warehouse;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductsDetailsDTO(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name,
        @JsonProperty("price") BigDecimal price
) {}
