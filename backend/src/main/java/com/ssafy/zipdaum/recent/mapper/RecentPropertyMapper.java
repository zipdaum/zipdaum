package com.ssafy.zipdaum.recent.mapper;

import com.ssafy.zipdaum.property.domain.DealType;
import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecentPropertyMapper {

  boolean existsDealForProperty(
      @Param("propertyId") Long propertyId,
      @Param("dealType") DealType dealType,
      @Param("dealId") Long dealId);

  List<RecentPropertyResponse> selectRecentProperties(@Param("userId") Long userId);

  void upsertRecentProperty(
      @Param("userId") Long userId,
      @Param("propertyId") Long propertyId,
      @Param("lastDealType") DealType lastDealType,
      @Param("lastDealId") Long lastDealId);

  List<Long> selectRecentPropertyIdsOverLimit(
      @Param("userId") Long userId,
      @Param("limit") int limit);

  int deleteRecentPropertiesByIds(@Param("ids") List<Long> ids);
}
