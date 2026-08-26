package com.example.flood.material.application;

import com.example.flood.material.domain.MaterialStandardDefinition;
import java.util.List;

public record SelectedMaterialStandards(long standardSetId, String version,
    List<MaterialStandardDefinition> definitions) {
    public SelectedMaterialStandards { definitions = List.copyOf(definitions); }
}
