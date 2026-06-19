package com.ssafy.zipdaum.recent.dto;

import com.ssafy.zipdaum.property.domain.DealType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "최근 본 주택 저장 요청")
public class RecentPropertySaveRequest {

  @Schema(description = "주택 ID", example = "1")
  @NotNull
  @Positive
  private Long propertyId;

  @Schema(description = "마지막으로 조회한 거래 유형", example = "SALE")
  private DealType lastDealType;

  @Schema(description = "마지막으로 조회한 거래 ID", example = "1")
  @Positive
  private Long lastDealId;
}
