package com.ssafy.zipdaum.recommendation.mapper;

import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationMapper {

  PropertyRecommendationCandidate selectPropertyRecommendationCandidate(
      @Param("propertyId") Long propertyId);

  List<PropertyRecommendationCandidate> selectPropertyRecommendationCandidates();
}
