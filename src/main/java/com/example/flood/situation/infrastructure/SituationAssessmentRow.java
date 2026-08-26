package com.example.flood.situation.infrastructure;

import java.time.Instant;

public class SituationAssessmentRow {
    private long id;
    private final String publicId;
    private final long regionId;
    private final Instant assessmentTime;
    private final String situationLevel;
    private final int activeEventCount;
    private final long affectedPopulation;
    private final long trappedPopulation;
    private final long evacuatedPopulation;
    private final long vulnerablePopulation;
    private final long ruleSetId;
    private final String ruleVersion;
    private final String sourceType;
    private final String inputSnapshot;
    private final String warnings;
    private final long createdByClientId;

    public SituationAssessmentRow(String publicId, long regionId, Instant assessmentTime,
        String situationLevel, int activeEventCount, long affectedPopulation,
        long trappedPopulation, long evacuatedPopulation, long vulnerablePopulation,
        long ruleSetId, String ruleVersion, String sourceType, String inputSnapshot,
        String warnings, long createdByClientId) {
        this.publicId = publicId;
        this.regionId = regionId;
        this.assessmentTime = assessmentTime;
        this.situationLevel = situationLevel;
        this.activeEventCount = activeEventCount;
        this.affectedPopulation = affectedPopulation;
        this.trappedPopulation = trappedPopulation;
        this.evacuatedPopulation = evacuatedPopulation;
        this.vulnerablePopulation = vulnerablePopulation;
        this.ruleSetId = ruleSetId;
        this.ruleVersion = ruleVersion;
        this.sourceType = sourceType;
        this.inputSnapshot = inputSnapshot;
        this.warnings = warnings;
        this.createdByClientId = createdByClientId;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getPublicId() { return publicId; }
    public long getRegionId() { return regionId; }
    public Instant getAssessmentTime() { return assessmentTime; }
    public String getSituationLevel() { return situationLevel; }
    public int getActiveEventCount() { return activeEventCount; }
    public long getAffectedPopulation() { return affectedPopulation; }
    public long getTrappedPopulation() { return trappedPopulation; }
    public long getEvacuatedPopulation() { return evacuatedPopulation; }
    public long getVulnerablePopulation() { return vulnerablePopulation; }
    public long getRuleSetId() { return ruleSetId; }
    public String getRuleVersion() { return ruleVersion; }
    public String getSourceType() { return sourceType; }
    public String getInputSnapshot() { return inputSnapshot; }
    public String getWarnings() { return warnings; }
    public long getCreatedByClientId() { return createdByClientId; }
}
