package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.api.PropertyApiClient;
import com.ssafy.zipdaum.property.config.PropertyApiProperties;
import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.domain.RegionCode;
import com.ssafy.zipdaum.property.dto.CoordinateDto;
import com.ssafy.zipdaum.property.dto.PropertySaveCommand;
import com.ssafy.zipdaum.property.dto.PropertySaveResult;
import com.ssafy.zipdaum.property.dto.PropertyItem;
import com.ssafy.zipdaum.property.dto.RentDealSaveCommand;
import com.ssafy.zipdaum.property.dto.SaleDealSaveCommand;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyFetchServiceImpl implements PropertyFetchService {

  private final PropertyApiClient propertyApiClient;
  private final GeocodeService geocodeService;
  private final PropertyMapper propertyMapper;
  private final PropertyApiProperties properties;

  @Override
  public PropertySaveResult fetchAndSaveProperties(DealApiType apiType, String lawdCd, String dealYmd) {
    List<PropertyItem> propertyItems = fetchDeals(apiType, lawdCd, dealYmd);
    Map<String, CoordinateDto> coordinateCache = new HashMap<>();
    int savedDealCount = 0;

    for (PropertyItem propertyItem : propertyItems) {
      Long propertyId = savePropertyWithCoordinate(propertyItem, coordinateCache);
      savedDealCount += savePropertyDeal(apiType, propertyId, propertyItem);
    }

    log.info("실거래가 저장 완료 type={}, lawdCd={}, dealYmd={}, fetchedCount={}, savedCount={}",
        apiType, lawdCd, dealYmd, propertyItems.size(), savedDealCount);
    return new PropertySaveResult(propertyItems.size(), savedDealCount);
  }

  private List<PropertyItem> fetchDeals(DealApiType apiType, String lawdCd, String dealYmd) {
    validateFetchRequest(lawdCd, dealYmd);
    return propertyApiClient.fetch(apiType, lawdCd, dealYmd, 1);
  }

  private void validateFetchRequest(String lawdCd, String dealYmd) {
    if (!properties.hasServiceKey()) {
      log.warn("실거래가 저장 실패 - 공공데이터 API 키 누락");
      throw new BusinessException(ErrorCode.REAL_ESTATE_API_KEY_NOT_FOUND);
    }
    if (lawdCd == null || !lawdCd.matches("\\d{5}")) {
      log.warn("실거래가 저장 실패 - 잘못된 법정동 코드 lawdCd={}", lawdCd);
      throw new BusinessException(ErrorCode.INVALID_LAWD_CODE);
    }
    if (dealYmd == null || !dealYmd.matches("\\d{6}")) {
      log.warn("실거래가 저장 실패 - 잘못된 계약년월 dealYmd={}", dealYmd);
      throw new BusinessException(ErrorCode.INVALID_DEAL_YMD);
    }
  }

  private Long savePropertyWithCoordinate(PropertyItem propertyItem,
      Map<String, CoordinateDto> coordinateCache) {
    PropertySaveCommand propertyCommand = toPropertySaveCommand(propertyItem);
    PropertySaveCommand savedProperty = propertyMapper.findProperty(propertyCommand);

    if (savedProperty == null) {
      setCoordinate(propertyCommand, propertyItem, coordinateCache);
      propertyMapper.insertProperty(propertyCommand);
      return propertyCommand.getId();
    }

    Long propertyId = savedProperty.getId();
    propertyCommand.setId(propertyId);
    if (!hasCoordinate(savedProperty)) {
      setCoordinate(propertyCommand, propertyItem, coordinateCache);
      propertyMapper.updatePropertyCoordinate(propertyCommand);
    }
    return propertyId;
  }

  private void setCoordinate(PropertySaveCommand propertyCommand, PropertyItem propertyItem,
      Map<String, CoordinateDto> coordinateCache) {
    String address = toAddress(propertyItem);
    CoordinateDto coordinate = coordinateCache.computeIfAbsent(address, geocodeService::getCoordinate);
    propertyCommand.setLatitude(coordinate.latitude());
    propertyCommand.setLongitude(coordinate.longitude());
  }

  private boolean hasCoordinate(PropertySaveCommand propertyCommand) {
    return propertyCommand.getLatitude() != null && propertyCommand.getLongitude() != null;
  }

  private PropertySaveCommand toPropertySaveCommand(PropertyItem propertyItem) {
    PropertySaveCommand propertyCommand = new PropertySaveCommand();
    propertyCommand.setPropertyType(propertyItem.apiType().getPropertyType());
    propertyCommand.setName(defaultText(propertyItem.propertyName()));
    propertyCommand.setSggCd(defaultText(propertyItem.sggCd()));
    propertyCommand.setUmdNm(defaultText(propertyItem.umdNm()));
    propertyCommand.setJibun(defaultText(propertyItem.jibun()));
    propertyCommand.setBuildYear(defaultInt(propertyItem.buildYear()));
    return propertyCommand;
  }

  private int savePropertyDeal(DealApiType apiType, Long propertyId, PropertyItem propertyItem) {
    if (apiType.isSale()) {
      return saveSaleDeal(propertyId, propertyItem);
    }
    return saveRentDeal(propertyId, propertyItem);
  }

  private int saveSaleDeal(Long propertyId, PropertyItem propertyItem) {
    int insertedCount = propertyMapper.insertSaleDeal(new SaleDealSaveCommand(
        propertyId,
        defaultDecimal(propertyItem.exclusiveArea()),
        propertyItem.landArea(),
        defaultLong(propertyItem.dealAmount()),
        defaultInt(propertyItem.floor()),
        propertyItem.dealDate(),
        defaultText(propertyItem.buyerGbn()),
        defaultText(propertyItem.sellerGbn())
    ));

    propertyMapper.updateLatestSalePrice(
        propertyId,
        defaultLong(propertyItem.dealAmount()),
        propertyItem.dealDate()
    );
    return insertedCount;
  }

  private int saveRentDeal(Long propertyId, PropertyItem propertyItem) {
    int insertedCount = propertyMapper.insertRentDeal(new RentDealSaveCommand(
        propertyId,
        defaultDecimal(propertyItem.exclusiveArea()),
        propertyItem.landArea(),
        defaultLong(propertyItem.deposit()),
        defaultLong(propertyItem.monthlyRent()),
        defaultInt(propertyItem.floor()),
        defaultText(propertyItem.contractTerm()),
        defaultText(propertyItem.contractType()),
        Boolean.TRUE.equals(propertyItem.useRrRight()),
        propertyItem.preDeposit(),
        propertyItem.preMonthlyRent(),
        propertyItem.dealDate()
    ));

    propertyMapper.updateLatestRentPrice(
        propertyId,
        defaultLong(propertyItem.deposit()),
        defaultLong(propertyItem.monthlyRent()),
        propertyItem.dealDate()
    );
    return insertedCount;
  }

  private String toAddress(PropertyItem propertyItem) {
    String regionName = RegionCode.nameOf(propertyItem.sggCd());
    return String.join(" ", regionName, defaultText(propertyItem.umdNm()), defaultText(propertyItem.jibun()));
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
