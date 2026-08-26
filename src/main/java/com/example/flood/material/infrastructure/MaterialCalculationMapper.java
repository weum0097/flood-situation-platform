package com.example.flood.material.infrastructure;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MaterialCalculationMapper {
    int insert(MaterialCalculationRow row);
    int insertItem(MaterialDemandItemRow row);
}
