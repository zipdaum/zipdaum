package com.ssafy.zipdaum.property.mapper;

import com.ssafy.zipdaum.property.dto.PropertySearchRequest;
import com.ssafy.zipdaum.property.dto.PropertySearchResponse;
import com.ssafy.zipdaum.property.dto.PropertySaveCommand;
import com.ssafy.zipdaum.property.dto.RentDealSaveCommand;
import com.ssafy.zipdaum.property.dto.SaleDealSaveCommand;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PropertyMapper {

  boolean existsPropertyById(Long propertyId);

  List<PropertySearchResponse> selectProperties(PropertySearchRequest request);

  PropertySaveCommand findProperty(PropertySaveCommand command);

  int insertProperty(PropertySaveCommand command);

  int updatePropertyCoordinate(PropertySaveCommand command);

  int insertSaleDeal(SaleDealSaveCommand command);

  int insertRentDeal(RentDealSaveCommand command);

  int updateLatestSalePrice(@Param("propertyId") Long propertyId,
      @Param("dealAmount") Long dealAmount,
      @Param("dealDate") LocalDate dealDate);

  int updateLatestRentPrice(@Param("propertyId") Long propertyId,
      @Param("deposit") Long deposit,
      @Param("monthlyRent") Long monthlyRent,
      @Param("dealDate") LocalDate dealDate);
}
