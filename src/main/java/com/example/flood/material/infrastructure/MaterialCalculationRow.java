package com.example.flood.material.infrastructure;

import java.math.BigDecimal;

public class MaterialCalculationRow {
    private long id;
    private final String publicId;
    private final long regionId;
    private final Long assessmentId;
    private final String sourceType;
    private final String situationLevel;
    private final BigDecimal supplyDurationHours;
    private final int supplyDays;
    private final long affectedPopulation;
    private final long trappedPopulation;
    private final long evacuatedPopulation;
    private final long vulnerablePopulation;
    private final long standardSetId;
    private final String standardVersion;
    private final String inputSnapshot;
    private final String warnings;
    private final long createdByClientId;

    public MaterialCalculationRow(String publicId, long regionId, Long assessmentId,
        String sourceType, String situationLevel, BigDecimal supplyDurationHours, int supplyDays,
        long affectedPopulation, long trappedPopulation, long evacuatedPopulation,
        long vulnerablePopulation, long standardSetId, String standardVersion,
        String inputSnapshot, String warnings, long createdByClientId) {
        this.publicId = publicId; this.regionId = regionId; this.assessmentId = assessmentId;
        this.sourceType = sourceType; this.situationLevel = situationLevel;
        this.supplyDurationHours = supplyDurationHours; this.supplyDays = supplyDays;
        this.affectedPopulation = affectedPopulation; this.trappedPopulation = trappedPopulation;
        this.evacuatedPopulation = evacuatedPopulation; this.vulnerablePopulation = vulnerablePopulation;
        this.standardSetId = standardSetId; this.standardVersion = standardVersion;
        this.inputSnapshot = inputSnapshot; this.warnings = warnings;
        this.createdByClientId = createdByClientId;
    }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public long getRegionId() { return regionId; }
    public Long getAssessmentId() { return assessmentId; }
    public String getSourceType() { return sourceType; }
    public String getSituationLevel() { return situationLevel; }
    public BigDecimal getSupplyDurationHours() { return supplyDurationHours; }
    public int getSupplyDays() { return supplyDays; }
    public long getAffectedPopulation() { return affectedPopulation; }
    public long getTrappedPopulation() { return trappedPopulation; }
    public long getEvacuatedPopulation() { return evacuatedPopulation; }
    public long getVulnerablePopulation() { return vulnerablePopulation; }
    public long getStandardSetId() { return standardSetId; }
    public String getStandardVersion() { return standardVersion; }
    public String getInputSnapshot() { return inputSnapshot; }
    public String getWarnings() { return warnings; }
    public long getCreatedByClientId() { return createdByClientId; }
}
