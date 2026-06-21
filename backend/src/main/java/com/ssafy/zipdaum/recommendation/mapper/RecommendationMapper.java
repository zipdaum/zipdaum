package com.ssafy.zipdaum.recommendation.mapper;

import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidateFilter;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecommendationMapper {

  PropertyRecommendationCandidate selectPropertyRecommendationCandidate(
      @Param("userId") Long userId,
      @Param("propertyId") Long propertyId);

  List<PropertyRecommendationCandidate> selectPropertyRecommendationCandidates(
      @Param("filter") PropertyRecommendationCandidateFilter filter);
}
