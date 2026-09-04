package com.example.inventory.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementCheckout(String result) {
        meterRegistry.counter("checkout.requests", "result", result).increment();
    }
    
    public void recordCheckoutDuration(long durationMs) {
        Timer.builder("checkout.duration")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void incrementInventoryTransfer(String result) {
        meterRegistry.counter("inventory.transfers", "result", result).increment();
    }

    public void incrementConcurrencyConflict() {
        meterRegistry.counter("inventory.concurrency.conflicts").increment();
    }
}
