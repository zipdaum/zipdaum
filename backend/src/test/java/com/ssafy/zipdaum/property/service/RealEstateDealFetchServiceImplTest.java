package com.ssafy.zipdaum.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.api.RealEstateDealApiClient;
import com.ssafy.zipdaum.property.config.PropertyApiProperties;
import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.CoordinateDto;
import com.ssafy.zipdaum.property.dto.PropertySaveCommand;
import com.ssafy.zipdaum.property.dto.RealEstateDealItem;
import com.ssafy.zipdaum.property.dto.RealEstateDealSaveResult;
import com.ssafy.zipdaum.property.dto.RentDealSaveCommand;
import com.ssafy.zipdaum.property.dto.SaleDealSaveCommand;
import com.ssafy.zipdaum.property.mapper.PropertyDealMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RealEstateDealFetchServiceImplTest {

  private final FakeRealEstateDealApiClient dealApiClient = new FakeRealEstateDealApiClient();
  private final FakeGeocodeService geocodeService = new FakeGeocodeService();
  private final FakePropertyDealMapper propertyDealMapper = new FakePropertyDealMapper();
  private final PropertyApiProperties properties = new PropertyApiProperties();

  private RealEstateDealFetchServiceImpl service;

  @BeforeEach
  void setUp() {
    properties.setServiceKey("test-key");
    service = new RealEstateDealFetchServiceImpl(
        dealApiClient,
        geocodeService,
        propertyDealMapper,
        properties
    );
  }

  @Test
  void fetchAndSaveDeals_이미_좌표가_있는_매물은_카카오_API를_호출하지_않고_거래만_저장한다() {
    propertyDealMapper.prepareExistingProperty(
        property("APARTMENT", "26350", "우동", "1484", "테스트아파트", 2020,
            new BigDecimal("35.1"), new BigDecimal("129.1"), 10L)
    );
    dealApiClient.items = List.of(saleDeal("우동", "1484", "테스트아파트", LocalDate.of(2025, 1, 10)));

    RealEstateDealSaveResult result = service.fetchAndSaveDeals(
        DealApiType.APARTMENT_SALE,
        "26350",
        "202501"
    );

    assertThat(result.fetchedCount()).isEqualTo(1);
    assertThat(result.savedCount()).isEqualTo(1);
    assertThat(geocodeService.callCount).isZero();
    assertThat(propertyDealMapper.insertSaleDealCount).isEqualTo(1);
    assertThat(propertyDealMapper.updateLatestSalePriceCount).isEqualTo(1);
    assertThat(propertyDealMapper.updatePropertyCoordinateCount).isZero();
  }

  @Test
  void fetchAndSaveDeals_새로운_같은_주소가_반복되면_카카오_API를_한_번만_호출한다() {
    dealApiClient.items = List.of(
        saleDeal("우동", "1484", "테스트아파트", LocalDate.of(2025, 1, 10)),
        saleDeal("우동", "1484", "테스트아파트", LocalDate.of(2025, 1, 20))
    );

    RealEstateDealSaveResult result = service.fetchAndSaveDeals(
        DealApiType.APARTMENT_SALE,
        "26350",
        "202501"
    );

    assertThat(result.fetchedCount()).isEqualTo(2);
    assertThat(result.savedCount()).isEqualTo(2);
    assertThat(geocodeService.callCount).isEqualTo(1);
    assertThat(geocodeService.requestAddresses).containsExactly("부산 해운대구 우동 1484");
    assertThat(propertyDealMapper.insertPropertyCount).isEqualTo(1);
    assertThat(propertyDealMapper.insertSaleDealCount).isEqualTo(2);
  }

  @Test
  void fetchAndSaveDeals_공공데이터_API_키가_없으면_예외가_발생한다() {
    properties.setServiceKey("");

    assertThatThrownBy(() -> service.fetchAndSaveDeals(
        DealApiType.APARTMENT_SALE,
        "26350",
        "202501"
    ))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REAL_ESTATE_API_KEY_NOT_FOUND)
        );
  }

  @Test
  void fetchAndSaveDeals_법정동_코드가_5자리_숫자가_아니면_예외가_발생한다() {
    assertThatThrownBy(() -> service.fetchAndSaveDeals(
        DealApiType.APARTMENT_SALE,
        "2635",
        "202501"
    ))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_LAWD_CODE)
        );
  }

  private RealEstateDealItem saleDeal(String umdNm, String jibun, String propertyName,
      LocalDate dealDate) {
    return new RealEstateDealItem(
        DealApiType.APARTMENT_SALE,
        "26350",
        umdNm,
        jibun,
        propertyName,
        2020,
        new BigDecimal("84.9"),
        null,
        100000L,
        null,
        null,
        10,
        null,
        null,
        null,
        null,
        null,
        null,
        "개인",
        "개인",
        null,
        null,
        null,
        false,
        null,
        null,
        dealDate
    );
  }

  private PropertySaveCommand property(String propertyType, String sggCd, String umdNm, String jibun,
      String name, Integer buildYear, BigDecimal latitude, BigDecimal longitude, Long id) {
    PropertySaveCommand command = new PropertySaveCommand();
    command.setId(id);
    command.setPropertyType(propertyType);
    command.setSggCd(sggCd);
    command.setUmdNm(umdNm);
    command.setJibun(jibun);
    command.setName(name);
    command.setBuildYear(buildYear);
    command.setLatitude(latitude);
    command.setLongitude(longitude);
    return command;
  }

  private static String propertyKey(PropertySaveCommand command) {
    return String.join("|",
        command.getPropertyType(),
        command.getSggCd(),
        command.getUmdNm(),
        command.getJibun(),
        command.getName(),
        String.valueOf(command.getBuildYear())
    );
  }

  private static class FakeRealEstateDealApiClient extends RealEstateDealApiClient {

    private List<RealEstateDealItem> items = List.of();

    FakeRealEstateDealApiClient() {
      super(null, null);
    }

    @Override
    public List<RealEstateDealItem> fetch(DealApiType apiType, String lawdCd, String dealYmd) {
      return items;
    }
  }

  private static class FakeGeocodeService implements GeocodeService {

    private int callCount;
    private final List<String> requestAddresses = new ArrayList<>();

    @Override
    public CoordinateDto getCoordinate(String address) {
      callCount++;
      requestAddresses.add(address);
      return new CoordinateDto(address, new BigDecimal("35.1"), new BigDecimal("129.1"));
    }
  }

  private static class FakePropertyDealMapper implements PropertyDealMapper {

    private final Map<String, PropertySaveCommand> properties = new HashMap<>();
    private long nextPropertyId = 1L;
    private int insertPropertyCount;
    private int updatePropertyCoordinateCount;
    private int insertSaleDealCount;
    private int updateLatestSalePriceCount;

    private void prepareExistingProperty(PropertySaveCommand command) {
      properties.put(propertyKey(command), command);
    }

    @Override
    public PropertySaveCommand findProperty(PropertySaveCommand command) {
      return properties.get(propertyKey(command));
    }

    @Override
    public int insertProperty(PropertySaveCommand command) {
      insertPropertyCount++;
      command.setId(nextPropertyId++);
      properties.put(propertyKey(command), copy(command));
      return 1;
    }

    @Override
    public int updatePropertyCoordinate(PropertySaveCommand command) {
      updatePropertyCoordinateCount++;
      properties.put(propertyKey(command), copy(command));
      return 1;
    }

    @Override
    public int insertSaleDeal(SaleDealSaveCommand command) {
      insertSaleDealCount++;
      return 1;
    }

    @Override
    public int insertRentDeal(RentDealSaveCommand command) {
      return 1;
    }

    @Override
    public int updateLatestSalePrice(Long propertyId, Long dealAmount, LocalDate dealDate) {
      updateLatestSalePriceCount++;
      return 1;
    }

    @Override
    public int updateLatestRentPrice(Long propertyId, Long deposit, Long monthlyRent,
        LocalDate dealDate) {
      return 1;
    }

    private PropertySaveCommand copy(PropertySaveCommand command) {
      PropertySaveCommand copy = new PropertySaveCommand();
      copy.setId(command.getId());
      copy.setPropertyType(command.getPropertyType());
      copy.setSggCd(command.getSggCd());
      copy.setUmdNm(command.getUmdNm());
      copy.setJibun(command.getJibun());
      copy.setName(command.getName());
      copy.setBuildYear(command.getBuildYear());
      copy.setLatitude(command.getLatitude());
      copy.setLongitude(command.getLongitude());
      return copy;
    }
  }
}
