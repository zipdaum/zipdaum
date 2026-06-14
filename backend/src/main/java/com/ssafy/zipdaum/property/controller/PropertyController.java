package com.ssafy.zipdaum.property.controller;

import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.PropertyDealHistoryResponse;
import com.ssafy.zipdaum.property.dto.PropertyDetailResponse;
import com.ssafy.zipdaum.property.dto.PropertySearchRequest;
import com.ssafy.zipdaum.property.dto.PropertySearchResponse;
import com.ssafy.zipdaum.property.dto.PropertySaveResult;
import com.ssafy.zipdaum.property.dto.SurroundingRequest;
import com.ssafy.zipdaum.property.dto.SurroundingResponse;
import com.ssafy.zipdaum.property.service.PropertyFetchService;
import com.ssafy.zipdaum.property.service.PropertyService;
import com.ssafy.zipdaum.property.service.SurroundingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/properties")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "실거래가", description = "공공데이터 실거래가 조회 및 저장 API")
public class PropertyController {

  private final PropertyFetchService fetchService;
  private final PropertyService propertyService;
  private final SurroundingService surroundingService;

  @GetMapping
  @Operation(
      summary = "주택 실거래가 조회",
      description = "지역, 주택명, 주택 유형, 거래 유형, 가격 조건으로 주택 실거래가 목록을 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "주택 실거래가 조회 성공",
          content = @Content(schema = @Schema(implementation = PropertySearchResponse.class))
      ),
      @ApiResponse(responseCode = "400", description = "요청 파라미터 오류", content = @Content)
  })
  public ResponseEntity<List<PropertySearchResponse>> searchProperties(
      @Valid @ModelAttribute PropertySearchRequest request
  ) {
    log.info("GET /properties 요청 sggCd={}, dealType={}, minPrice={}, maxPrice={}, sortBy={}, sortDirection={}",
        request.getSggCd(), request.getDealType(), request.getMinPrice(), request.getMaxPrice(),
        request.getSortBy(), request.getSortDirection());

    return ResponseEntity.ok(propertyService.searchProperties(request));
  }

  @GetMapping("/{propertyId}")
  @Operation(
      summary = "주택 상세 조회",
      description = "주택 기본 정보와 최신 거래 요약 정보를 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "주택 상세 조회 성공",
          content = @Content(schema = @Schema(implementation = PropertyDetailResponse.class))
      ),
      @ApiResponse(responseCode = "404", description = "주택 정보 없음", content = @Content)
  })
  public ResponseEntity<PropertyDetailResponse> getPropertyDetail(
      @Parameter(description = "주택 ID", example = "1", required = true)
      @PathVariable Long propertyId
  ) {
    log.info("GET /properties/{} 요청", propertyId);
    return ResponseEntity.ok(propertyService.findPropertyDetail(propertyId));
  }

  @GetMapping("/{propertyId}/histories")
  @Operation(
      summary = "거래 이력 조회",
      description = "주택의 전체 매매/전월세 거래 이력을 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "거래 이력 조회 성공",
          content = @Content(schema = @Schema(implementation = PropertyDealHistoryResponse.class))
      ),
      @ApiResponse(responseCode = "404", description = "주택 정보 없음", content = @Content)
  })
  public ResponseEntity<PropertyDealHistoryResponse> getPropertyDealHistories(
      @Parameter(description = "주택 ID", example = "1", required = true)
      @PathVariable Long propertyId
  ) {
    log.info("GET /properties/{}/histories 요청", propertyId);
    return ResponseEntity.ok(propertyService.findPropertyDealHistories(propertyId));
  }

  @GetMapping("/{propertyId}/surroundings")
  @Operation(
      summary = "주택 주변 편의시설 조회",
      description = "주택 좌표 기준 반경 내 대중교통, 병원, CCTV, 공원을 조회합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "주택 주변 편의시설 조회 성공",
          content = @Content(schema = @Schema(implementation = SurroundingResponse.class))
      ),
      @ApiResponse(responseCode = "400", description = "요청 파라미터 오류", content = @Content),
      @ApiResponse(responseCode = "404", description = "주택 정보 또는 좌표 정보 없음", content = @Content)
  })
  public ResponseEntity<SurroundingResponse> getPropertySurroundings(
      @Parameter(description = "주택 ID", example = "1", required = true)
      @PathVariable @Positive Long propertyId,
      @Valid @ModelAttribute SurroundingRequest request
  ) {
    log.info("GET /properties/{}/surroundings 요청 radiusMeters={}",
        propertyId, request.getRadiusMeters());
    return ResponseEntity.ok(
        surroundingService.findPropertySurroundings(propertyId, request.getRadiusMeters())
    );
  }

  @PostMapping
  @Operation(
      summary = "실거래가 저장",
      description = "공공데이터 실거래가 API에서 거래 데이터를 조회하고, 카카오 API로 좌표를 가져와 DB에 저장합니다."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "실거래가 저장 성공",
          content = @Content(schema = @Schema(implementation = PropertySaveResult.class))
      ),
      @ApiResponse(responseCode = "400", description = "요청 파라미터 오류", content = @Content),
      @ApiResponse(responseCode = "404", description = "주소 좌표 정보 없음", content = @Content),
      @ApiResponse(responseCode = "500", description = "외부 API 키 누락 또는 외부 API 연동 오류", content = @Content)
  })
  public ResponseEntity<PropertySaveResult> saveProperties(
      @Parameter(description = "실거래가 API 유형", example = "APARTMENT_SALE", required = true)
      @RequestParam DealApiType type,
      @Parameter(description = "법정동 코드 앞 5자리", example = "26350", required = true)
      @RequestParam @Pattern(regexp = "\\d{5}") String lawdCd,
      @Parameter(description = "계약년월 6자리", example = "202501", required = true)
      @RequestParam @Pattern(regexp = "\\d{6}") String dealYmd
  ) {
    log.info("POST /properties 요청 type={}, lawdCd={}, dealYmd={}", type, lawdCd, dealYmd);

    return ResponseEntity.ok(fetchService.fetchAndSaveProperties(type, lawdCd, dealYmd));
  }
}
