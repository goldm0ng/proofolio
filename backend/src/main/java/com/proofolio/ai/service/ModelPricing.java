package com.proofolio.ai.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** USD per 1M tokens. Cache reads are billed at 10% of input. */
public final class ModelPricing {
    private ModelPricing() {}

    private record Price(double input, double output) {}

    private static Price priceFor(String model) {
        String m = model == null ? "" : model.toLowerCase();
        if (m.contains("opus-5")) return new Price(5.0, 25.0);
        if (m.contains("sonnet-5")) return new Price(2.0, 10.0);
        if (m.contains("haiku-4-5")) return new Price(1.0, 5.0);
        if (m.contains("opus")) return new Price(5.0, 25.0);
        if (m.contains("sonnet")) return new Price(3.0, 15.0);
        if (m.contains("haiku")) return new Price(1.0, 5.0);
        return new Price(5.0, 25.0);
    }

    public static BigDecimal cost(String model, long inputTokens, long outputTokens, long cacheReadTokens) {
        Price p = priceFor(model);
        double usd = (inputTokens * p.input()
                + cacheReadTokens * p.input() * 0.1
                + outputTokens * p.output()) / 1_000_000.0;
        return BigDecimal.valueOf(usd).setScale(6, RoundingMode.HALF_UP);
    }
}
