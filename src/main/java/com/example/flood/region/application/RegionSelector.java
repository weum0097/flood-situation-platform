package com.example.flood.region.application;

public record RegionSelector(String regionId, String regionName) {
    public RegionSelector {
        regionId = blankToNull(regionId);
        regionName = blankToNull(regionName);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.strip();
    }
}
