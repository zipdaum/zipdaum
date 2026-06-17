package com.ssafy.zipdaum.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.SurroundingType;
import com.ssafy.zipdaum.property.dto.PropertyDetailResponse;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SurroundingServiceImplTest {

  private final PropertyMapper propertyMapper = mock(PropertyMapper.class);
  private final FacilitySourceLoader facilitySourceLoader = new FacilitySourceLoader();
  private final SurroundingServiceImpl service =
      new SurroundingServiceImpl(propertyMapper, facilitySourceLoader);

  @Test
  void findPropertySurroundings_주택ID로_주택좌표를_조회해_주변시설을_조회한다() {
    service.loadFacilities();
    PropertyDetailResponse property = new PropertyDetailResponse();
    property.setId(1L);
    property.setLatitude(new BigDecimal("35.1709598"));
    property.setLongitude(new BigDecimal("129.125307"));
    given(propertyMapper.selectPropertyById(1L)).willReturn(property);

    var result = service.findPropertySurroundings(1L, 3000);

    assertThat(result.getLatitude()).isEqualByComparingTo("35.1709598");
    assertThat(result.getLongitude()).isEqualByComparingTo("129.125307");
    assertThat(result.getFacilities()).isNotEmpty();
  }

  @Test
  void findPropertySurroundings_주택좌표가_없으면_예외가_발생한다() {
    PropertyDetailResponse property = new PropertyDetailResponse();
    property.setId(1L);
    given(propertyMapper.selectPropertyById(1L)).willReturn(property);

    assertThatThrownBy(() -> service.findPropertySurroundings(1L, 1000))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COORDINATE_NOT_FOUND)
        );
  }

  @Test
  void findSurroundings_로컬_시설데이터에서_반경내_시설을_조회한다() {
    service.loadFacilities();

    var result = service.findSurroundings(
        new BigDecimal("35.1709598"),
        new BigDecimal("129.125307"),
        3000
    );

    assertThat(result.getFacilities())
        .anySatisfy(facility -> {
          assertThat(facility.getName()).contains("APEC");
          assertThat(facility.getType()).isEqualTo(SurroundingType.PARK);
          assertThat(facility.getDistanceMeters()).isLessThanOrEqualTo(3000);
        });
    assertThat(result.getSummary().getBusCount() + result.getSummary().getSubwayCount())
        .isGreaterThan(0);
    assertThat(result.getSummary().getParkCount()).isGreaterThan(0);
  }

  @Test
  void findSurroundings_응답목록은_최대개수로_제한하고_요약은_반경내_전체개수를_사용한다() {
    service.loadFacilities();

    var result = service.findSurroundings(
        new BigDecimal("35.1709598"),
        new BigDecimal("129.125307"),
        3000
    );

    int summaryTotalCount = result.getSummary().getBusCount()
        + result.getSummary().getSubwayCount()
        + result.getSummary().getHospitalCount()
        + result.getSummary().getCctvCount()
        + result.getSummary().getParkCount();

    assertThat(result.getFacilities()).hasSizeLessThanOrEqualTo(80);
    assertThat(summaryTotalCount).isGreaterThan(result.getFacilities().size());
  }

  @Test
  void findSurroundingSummary_상세목록없이_반경내_시설개수만_반환한다() {
    service.loadFacilities();

    var detailResult = service.findSurroundings(
        new BigDecimal("35.1709598"),
        new BigDecimal("129.125307"),
        3000
    );
    var summary = service.findSurroundingSummary(
        new BigDecimal("35.1709598"),
        new BigDecimal("129.125307"),
        3000
    );

    assertThat(summary.getBusCount()).isEqualTo(detailResult.getSummary().getBusCount());
    assertThat(summary.getSubwayCount()).isEqualTo(detailResult.getSummary().getSubwayCount());
    assertThat(summary.getHospitalCount()).isEqualTo(detailResult.getSummary().getHospitalCount());
    assertThat(summary.getCctvCount()).isEqualTo(detailResult.getSummary().getCctvCount());
    assertThat(summary.getParkCount()).isEqualTo(detailResult.getSummary().getParkCount());
  }

  @Test
  void findSurroundings_좌표가_없으면_예외가_발생한다() {
    assertThatThrownBy(() -> service.findSurroundings(null, new BigDecimal("129.1"), 1000))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void findSurroundings_반경이_허용범위를_벗어나면_예외가_발생한다() {
    assertThatThrownBy(() -> service.findSurroundings(
        new BigDecimal("35.1"),
        new BigDecimal("129.1"),
        0
    )).isInstanceOf(BusinessException.class);
  }
}
