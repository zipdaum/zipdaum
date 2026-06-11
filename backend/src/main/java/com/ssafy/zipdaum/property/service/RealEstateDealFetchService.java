package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.api.RealEstateDealApiClient;
import com.ssafy.zipdaum.property.config.PropertyApiProperties;
import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.domain.RegionCode;
import com.ssafy.zipdaum.property.dto.CoordinateDto;
import com.ssafy.zipdaum.property.dto.PropertySaveCommand;
import com.ssafy.zipdaum.property.dto.RealEstateDealSaveResult;
import com.ssafy.zipdaum.property.dto.RealEstateDealItem;
import com.ssafy.zipdaum.property.dto.RentDealSaveCommand;
import com.ssafy.zipdaum.property.dto.SaleDealSaveCommand;
import com.ssafy.zipdaum.property.mapper.PropertyDealMapper;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealEstateDealFetchService {

  private final RealEstateDealApiClient dealApiClient;
  private final KakaoGeocodeService kakaoGeocodeService;
  private final PropertyDealMapper propertyDealMapper;
  private final PropertyApiProperties properties;

  public RealEstateDealSaveResult fetchAndSaveDeals(DealApiType apiType, String lawdCd, String dealYmd) {
    List<RealEstateDealItem> realEstateDeals = fetchDeals(apiType, lawdCd, dealYmd);
    int savedDealCount = 0;

    for (RealEstateDealItem realEstateDeal : realEstateDeals) {
      Long propertyId = savePropertyWithCoordinate(realEstateDeal);
      savedDealCount += saveRealEstateDeal(apiType, propertyId, realEstateDeal);
    }

    return new RealEstateDealSaveResult(realEstateDeals.size(), savedDealCount);
  }

  private List<RealEstateDealItem> fetchDeals(DealApiType apiType, String lawdCd, String dealYmd) {
    validateFetchRequest(lawdCd, dealYmd);
    return dealApiClient.fetch(apiType, lawdCd, dealYmd);
  }

  private void validateFetchRequest(String lawdCd, String dealYmd) {
    if (!properties.hasServiceKey()) {
      throw new BusinessException(ErrorCode.REAL_ESTATE_API_KEY_NOT_FOUND);
    }
    if (lawdCd == null || !lawdCd.matches("\\d{5}")) {
      throw new BusinessException(ErrorCode.INVALID_LAWD_CODE);
    }
    if (dealYmd == null || !dealYmd.matches("\\d{6}")) {
      throw new BusinessException(ErrorCode.INVALID_DEAL_YMD);
    }
  }

  private Long savePropertyWithCoordinate(RealEstateDealItem realEstateDeal) {
    PropertySaveCommand propertyCommand = toPropertySaveCommand(realEstateDeal);
    CoordinateDto coordinate = kakaoGeocodeService.getCoordinate(toAddress(realEstateDeal));
    propertyCommand.setLatitude(coordinate.latitude());
    propertyCommand.setLongitude(coordinate.longitude());

    Long propertyId = propertyDealMapper.findPropertyId(propertyCommand);
    if (propertyId == null) {
      propertyDealMapper.insertProperty(propertyCommand);
      return propertyCommand.getId();
    }

    propertyCommand.setId(propertyId);
    propertyDealMapper.updatePropertyCoordinate(propertyCommand);
    return propertyId;
  }

  private PropertySaveCommand toPropertySaveCommand(RealEstateDealItem realEstateDeal) {
    PropertySaveCommand propertyCommand = new PropertySaveCommand();
    propertyCommand.setPropertyType(realEstateDeal.apiType().getPropertyType());
    propertyCommand.setName(defaultText(realEstateDeal.propertyName()));
    propertyCommand.setSggCd(defaultText(realEstateDeal.sggCd()));
    propertyCommand.setUmdNm(defaultText(realEstateDeal.umdNm()));
    propertyCommand.setJibun(defaultText(realEstateDeal.jibun()));
    propertyCommand.setBuildYear(defaultInt(realEstateDeal.buildYear()));
    return propertyCommand;
  }

  private int saveRealEstateDeal(DealApiType apiType, Long propertyId, RealEstateDealItem realEstateDeal) {
    if (apiType.isSale()) {
      return saveSaleDeal(propertyId, realEstateDeal);
    }
    return saveRentDeal(propertyId, realEstateDeal);
  }

  private int saveSaleDeal(Long propertyId, RealEstateDealItem realEstateDeal) {
    int insertedCount = propertyDealMapper.insertSaleDeal(new SaleDealSaveCommand(
        propertyId,
        defaultDecimal(realEstateDeal.exclusiveArea()),
        realEstateDeal.landArea(),
        defaultLong(realEstateDeal.dealAmount()),
        defaultInt(realEstateDeal.floor()),
        realEstateDeal.dealDate(),
        defaultText(realEstateDeal.buyerGbn()),
        defaultText(realEstateDeal.sellerGbn())
    ));

    propertyDealMapper.updateLatestSalePrice(
        propertyId,
        defaultLong(realEstateDeal.dealAmount()),
        realEstateDeal.dealDate()
    );
    return insertedCount;
  }

  private int saveRentDeal(Long propertyId, RealEstateDealItem realEstateDeal) {
    int insertedCount = propertyDealMapper.insertRentDeal(new RentDealSaveCommand(
        propertyId,
        defaultDecimal(realEstateDeal.exclusiveArea()),
        realEstateDeal.landArea(),
        defaultLong(realEstateDeal.deposit()),
        defaultLong(realEstateDeal.monthlyRent()),
        defaultInt(realEstateDeal.floor()),
        defaultText(realEstateDeal.contractTerm()),
        defaultText(realEstateDeal.contractType()),
        Boolean.TRUE.equals(realEstateDeal.useRrRight()),
        realEstateDeal.preDeposit(),
        realEstateDeal.preMonthlyRent(),
        realEstateDeal.dealDate()
    ));

    propertyDealMapper.updateLatestRentPrice(
        propertyId,
        defaultLong(realEstateDeal.deposit()),
        defaultLong(realEstateDeal.monthlyRent()),
        realEstateDeal.dealDate()
    );
    return insertedCount;
  }

  private String toAddress(RealEstateDealItem realEstateDeal) {
    String regionName = RegionCode.nameOf(realEstateDeal.sggCd());
    return String.join(" ", regionName, defaultText(realEstateDeal.umdNm()), defaultText(realEstateDeal.jibun()));
  }

  private String defaultText(String value) {
    return value == null || value.isBlank() ? "-" : value.trim();
  }

  private int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }

  private long defaultLong(Long value) {
    return value == null ? 0L : value;
  }

  private BigDecimal defaultDecimal(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

}
