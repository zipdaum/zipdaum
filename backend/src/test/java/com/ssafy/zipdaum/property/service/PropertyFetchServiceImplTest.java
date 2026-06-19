package com.ssafy.zipdaum.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.api.PropertyApiClient;
import com.ssafy.zipdaum.property.config.PropertyApiProperties;
import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.CoordinateDto;
import com.ssafy.zipdaum.property.dto.PropertySaveCommand;
import com.ssafy.zipdaum.property.dto.PropertyItem;
import com.ssafy.zipdaum.property.dto.PropertySaveResult;
import com.ssafy.zipdaum.property.dto.SaleDealSaveCommand;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropertyFetchServiceImplTest {

  private final PropertyApiClient propertyApiClient = mock(PropertyApiClient.class);
  private final GeocodeService geocodeService = mock(GeocodeService.class);
  private final PropertyMapper propertyMapper = mock(PropertyMapper.class);
  private final PropertyApiProperties properties = new PropertyApiProperties();

  private PropertyFetchServiceImpl service;

  @BeforeEach
  void setUp() {
    properties.setServiceKey("test-key");
    service = new PropertyFetchServiceImpl(
        propertyApiClient,
        geocodeService,
        propertyMapper,
        properties
    );
  }

  @Test
  void fetchAndSaveProperties_이미_좌표가_있는_매물은_카카오_API를_호출하지_않고_거래만_저장한다() {
    PropertySaveCommand savedProperty = property("APARTMENT", "26350", "우동", "1484", "테스트아파트", 2020,
        new BigDecimal("35.1"), new BigDecimal("129.1"), 10L);
    given(propertyApiClient.fetch(DealApiType.APARTMENT_SALE, "26350", "202501", 1))
        .willReturn(List.of(saleDeal("우동", "1484", "테스트아파트", LocalDate.of(2025, 1, 10))));
    given(propertyMapper.findProperty(any(PropertySaveCommand.class))).willReturn(savedProperty);
    given(propertyMapper.insertSaleDeal(any(SaleDealSaveCommand.class))).willReturn(1);

    PropertySaveResult result = service.fetchAndSaveProperties(
        DealApiType.APARTMENT_SALE,
        "26350",
        "202501"
    );

    assertThat(result.fetchedCount()).isEqualTo(1);
    assertThat(result.savedCount()).isEqualTo(1);
    then(geocodeService).should(never()).getCoordinate(anyString());
    then(propertyMapper).should().insertSaleDeal(any(SaleDealSaveCommand.class));
    then(propertyMapper).should().updateLatestSalePrice(10L, 100000L, LocalDate.of(2025, 1, 10));
    then(propertyMapper).should(never()).updatePropertyCoordinate(any(PropertySaveCommand.class));
  }

  @Test
  void fetchAndSaveProperties_새로운_같은_주소가_반복되면_카카오_API를_한_번만_호출한다() {
    PropertySaveCommand savedProperty = property("APARTMENT", "26350", "우동", "1484", "테스트아파트", 2020,
        new BigDecimal("35.1"), new BigDecimal("129.1"), 1L);
    given(propertyApiClient.fetch(DealApiType.APARTMENT_SALE, "26350", "202501", 1))
        .willReturn(List.of(
            saleDeal("우동", "1484", "테스트아파트", LocalDate.of(2025, 1, 10)),
            saleDeal("우동", "1484", "테스트아파트", LocalDate.of(2025, 1, 20))
        ));
    given(propertyMapper.findProperty(any(PropertySaveCommand.class))).willReturn(null, savedProperty);
    given(geocodeService.getCoordinate("부산 해운대구 우동 1484"))
        .willReturn(new CoordinateDto("부산 해운대구 우동 1484", new BigDecimal("35.1"), new BigDecimal("129.1")));
    given(propertyMapper.insertProperty(any(PropertySaveCommand.class))).willAnswer(invocation -> {
      PropertySaveCommand command = invocation.getArgument(0);
      command.setId(1L);
      return 1;
    });
    given(propertyMapper.insertSaleDeal(any(SaleDealSaveCommand.class))).willReturn(1);

    PropertySaveResult result = service.fetchAndSaveProperties(
        DealApiType.APARTMENT_SALE,
        "26350",
        "202501"
    );

    assertThat(result.fetchedCount()).isEqualTo(2);
    assertThat(result.savedCount()).isEqualTo(2);
    then(geocodeService).should(times(1)).getCoordinate("부산 해운대구 우동 1484");
    then(propertyMapper).should(times(1)).insertProperty(any(PropertySaveCommand.class));
    then(propertyMapper).should(times(2)).insertSaleDeal(any(SaleDealSaveCommand.class));
    then(propertyMapper).should(times(2)).updateLatestSalePrice(any(), any(), any());
  }

  @Test
  void fetchAndSaveProperties_공공데이터_API_키가_없으면_예외가_발생한다() {
    properties.setServiceKey("");

    assertThatThrownBy(() -> service.fetchAndSaveProperties(
        DealApiType.APARTMENT_SALE,
        "26350",
        "202501"
    ))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REAL_ESTATE_API_KEY_NOT_FOUND)
        );
  }

  @Test
  void fetchAndSaveProperties_법정동_코드가_5자리_숫자가_아니면_예외가_발생한다() {
    assertThatThrownBy(() -> service.fetchAndSaveProperties(
        DealApiType.APARTMENT_SALE,
        "2635",
        "202501"
    ))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_LAWD_CODE)
        );
  }

  @Test
  void fetchAndSaveProperties_공공데이터_API_응답이_없으면_타임아웃_예외가_발생하고_저장하지_않는다() {
    given(propertyApiClient.fetch(DealApiType.APARTMENT_SALE, "26350", "202501", 1))
        .willThrow(new BusinessException(ErrorCode.REAL_ESTATE_API_TIMEOUT));

    assertThatThrownBy(() -> service.fetchAndSaveProperties(
        DealApiType.APARTMENT_SALE,
        "26350",
        "202501"
    ))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REAL_ESTATE_API_TIMEOUT)
        );

    then(propertyMapper).should(never()).findProperty(any(PropertySaveCommand.class));
    then(geocodeService).should(never()).getCoordinate(anyString());
  }

  private PropertyItem saleDeal(String umdNm, String jibun, String propertyName,
      LocalDate dealDate) {
    return new PropertyItem(
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

}
