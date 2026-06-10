package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.api.RealEstateDealApiClient;
import com.ssafy.zipdaum.property.config.PropertyApiProperties;
import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.CoordinateDto;
import com.ssafy.zipdaum.property.dto.PropertySaveCommand;
import com.ssafy.zipdaum.property.dto.RealEstateDealSaveResult;
import com.ssafy.zipdaum.property.dto.RealEstateDealItem;
import com.ssafy.zipdaum.property.dto.RentDealSaveCommand;
import com.ssafy.zipdaum.property.dto.SaleDealSaveCommand;
import com.ssafy.zipdaum.property.mapper.PropertyDealMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealEstateDealFetchService {

  private final RealEstateDealApiClient dealApiClient;
  private final KakaoGeocodeService kakaoGeocodeService;
  private final PropertyDealMapper propertyDealSaveMapper;
  private final PropertyApiProperties properties;

  public List<RealEstateDealItem> fetchDeals(DealApiType apiType, String lawdCd, String dealYmd) {
    if (!properties.hasServiceKey()) {
      throw new BusinessException(ErrorCode.REAL_ESTATE_API_KEY_NOT_FOUND);
    }
    if (lawdCd == null || !lawdCd.matches("\\d{5}")) {
      throw new BusinessException(ErrorCode.INVALID_LAWD_CODE);
    }
    if (dealYmd == null || !dealYmd.matches("\\d{6}")) {
      throw new BusinessException(ErrorCode.INVALID_DEAL_YMD);
    }
    return dealApiClient.fetch(apiType, lawdCd, dealYmd);
  }

  public RealEstateDealSaveResult fetchAndSaveDeals(DealApiType apiType, String lawdCd, String dealYmd) {
    List<RealEstateDealItem> deals = fetchDeals(apiType, lawdCd, dealYmd);
    int savedCount = 0;
    for (RealEstateDealItem deal : deals) {
      Long propertyId = saveProperty(deal);
      savedCount += saveDeal(apiType, propertyId, deal);
    }
    return new RealEstateDealSaveResult(deals.size(), savedCount);
  }

  private Long saveProperty(RealEstateDealItem deal) {
    PropertySaveCommand command = new PropertySaveCommand();
    command.setPropertyType(deal.apiType().getPropertyType());
    command.setName(defaultText(deal.propertyName()));
    command.setSggCd(defaultText(deal.sggCd()));
    command.setUmdNm(defaultText(deal.umdNm()));
    command.setJibun(defaultText(deal.jibun()));
    command.setBuildYear(defaultInt(deal.buildYear()));

    CoordinateDto coordinate = kakaoGeocodeService.getCoordinate(toAddress(deal));
    command.setLatitude(coordinate.latitude());
    command.setLongitude(coordinate.longitude());

    Long propertyId = propertyDealSaveMapper.findPropertyId(command);
    if (propertyId == null) {
      propertyDealSaveMapper.insertProperty(command);
      return command.getId();
    }

    command.setId(propertyId);
    propertyDealSaveMapper.updatePropertyCoordinate(command);
    return propertyId;
  }

  private int saveDeal(DealApiType apiType, Long propertyId, RealEstateDealItem deal) {
    if (apiType.isSale()) {
      int inserted = propertyDealSaveMapper.insertSaleDeal(new SaleDealSaveCommand(
          propertyId,
          defaultDecimal(deal.exclusiveArea()),
          deal.landArea(),
          defaultLong(deal.dealAmount()),
          defaultInt(deal.floor()),
          deal.dealDate(),
          defaultText(deal.buyerGbn()),
          defaultText(deal.sellerGbn())
      ));
      propertyDealSaveMapper.updateLatestSalePrice(propertyId, defaultLong(deal.dealAmount()), deal.dealDate());
      return inserted;
    }

    int inserted = propertyDealSaveMapper.insertRentDeal(new RentDealSaveCommand(
        propertyId,
        defaultDecimal(deal.exclusiveArea()),
        deal.landArea(),
        defaultLong(deal.deposit()),
        defaultLong(deal.monthlyRent()),
        defaultInt(deal.floor()),
        defaultText(deal.contractTerm()),
        defaultText(deal.contractType()),
        Boolean.TRUE.equals(deal.useRrRight()),
        deal.preDeposit(),
        deal.preMonthlyRent(),
        deal.dealDate()
    ));
    propertyDealSaveMapper.updateLatestRentPrice(
        propertyId,
        defaultLong(deal.deposit()),
        defaultLong(deal.monthlyRent()),
        deal.dealDate()
    );
    return inserted;
  }

  private String toAddress(RealEstateDealItem deal) {
    String regionName = regionName(deal.sggCd());
    return String.join(" ", regionName, defaultText(deal.umdNm()), defaultText(deal.jibun()));
  }

  private String regionName(String sggCd) {
    return BUSAN_REGIONS.getOrDefault(sggCd, "부산");
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

  private static final Map<String, String> BUSAN_REGIONS = Map.ofEntries(
      Map.entry("26110", "부산 중구"),
      Map.entry("26140", "부산 서구"),
      Map.entry("26170", "부산 동구"),
      Map.entry("26200", "부산 영도구"),
      Map.entry("26230", "부산진구"),
      Map.entry("26260", "부산 동래구"),
      Map.entry("26290", "부산 남구"),
      Map.entry("26320", "부산 북구"),
      Map.entry("26350", "부산 해운대구"),
      Map.entry("26380", "부산 사하구"),
      Map.entry("26410", "부산 금정구"),
      Map.entry("26440", "부산 강서구"),
      Map.entry("26470", "부산 연제구"),
      Map.entry("26500", "부산 수영구"),
      Map.entry("26530", "부산 사상구"),
      Map.entry("26710", "부산 기장군")
  );
}
