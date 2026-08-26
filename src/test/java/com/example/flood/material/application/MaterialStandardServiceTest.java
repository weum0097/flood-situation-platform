package com.example.flood.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.flood.common.api.ApiException;
import com.example.flood.common.api.ErrorCode;
import com.example.flood.material.domain.MaterialStandardDefinition;
import com.example.flood.material.domain.PopulationBasis;
import com.example.flood.region.application.RegionResolver;
import com.example.flood.region.domain.RegionStatus;
import com.example.flood.region.domain.ResolvedRegion;
import com.example.flood.situation.domain.SituationLevel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MaterialStandardServiceTest {
    private final ResolvedRegion region = new ResolvedRegion(
        3, "320111", "浦口区", 2L, "DISTRICT", RegionStatus.ACTIVE);
    private final Instant now = Instant.parse("2026-08-21T00:00:00Z");

    @Test void selectsMostSpecificDefinitionForEachMaterial() {
        MaterialStandardLookup lookup = mock(MaterialStandardLookup.class);
        RegionResolver resolver = mock(RegionResolver.class);
        when(resolver.specificToGlobalScopeIds(region)).thenReturn(List.of(3L, 2L, 1L, 0L));
        when(lookup.findActiveSet(now)).thenReturn(Optional.of(
            new MaterialStandardLookup.StandardSet(9, "v1")));
        when(lookup.findDefinitions(9, SituationLevel.HIGH, List.of(3L, 2L, 1L, 0L)))
            .thenReturn(List.of(definition(0, "WATER", "3"),
                definition(3, "WATER", "4"), definition(0, "FOOD", "2")));

        SelectedMaterialStandards selected = new MaterialStandardService(lookup, resolver)
            .select(region, SituationLevel.HIGH, now);

        assertThat(selected.definitions()).extracting(MaterialStandardDefinition::materialCode)
            .containsExactly("WATER", "FOOD");
        assertThat(selected.definitions().getFirst().perPersonPerDay())
            .isEqualByComparingTo("4");
    }

    @Test void reportsMissingActiveSet() {
        MaterialStandardLookup lookup = mock(MaterialStandardLookup.class);
        when(lookup.findActiveSet(now)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new MaterialStandardService(lookup, mock(RegionResolver.class))
            .select(region, SituationLevel.LOW, now))
            .isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.errorCode())
                    .isEqualTo(ErrorCode.NO_ACTIVE_MATERIAL_STANDARD));
    }

    private MaterialStandardDefinition definition(long scope, String code, String rate) {
        return new MaterialStandardDefinition(scope + 10, scope, code, code, "UNIT",
            PopulationBasis.AFFECTED, new BigDecimal(rate), null, BigDecimal.ONE,
            BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO);
    }
}
