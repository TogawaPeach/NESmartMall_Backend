package com.nemall.product.dto;

public record ProductKnowledgeRequest(
        String productId,
        String name,
        String model,
        String coverUrl,
        Long price
) {}
