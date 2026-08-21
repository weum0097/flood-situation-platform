package com.example.flood.material.domain;

public record PopulationSnapshot(long affected, long trapped, long evacuated, long vulnerable) {
    public PopulationSnapshot {
        if (affected < 0 || trapped < 0 || evacuated < 0 || vulnerable < 0)
            throw new IllegalArgumentException("Population must not be negative");
    }
    public long forBasis(PopulationBasis basis) {
        return switch (basis) {
            case AFFECTED -> affected;
            case TRAPPED -> trapped;
            case EVACUATED -> evacuated;
            case VULNERABLE -> vulnerable;
            case FIXED -> 0;
        };
    }
}
