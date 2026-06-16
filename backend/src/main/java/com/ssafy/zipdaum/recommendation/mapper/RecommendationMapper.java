package com.ssafy.zipdaum.recommendation.mapper;

import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationMapper {

  PropertyRecommendationCandidate selectPropertyRecommendationCandidate(
      @Param("propertyId") Long propertyId);
}
