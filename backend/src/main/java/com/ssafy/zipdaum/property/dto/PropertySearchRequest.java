package com.ssafy.zipdaum.property.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertySearchRequest {

  @Pattern(regexp = "^$|\\d{5}")
  private String sggCd;
  private String umdNm;
  private String name;
  private String propertyType;
  @Pattern(regexp = "^$|SALE|JEONSE|MONTHLY_RENT", flags = Pattern.Flag.CASE_INSENSITIVE)
  private String dealType;
  @PositiveOrZero
  private Long minPrice;
  @PositiveOrZero
  private Long maxPrice;
  @Pattern(regexp = "^$|LATEST|PRICE|NAME", flags = Pattern.Flag.CASE_INSENSITIVE)
  private String sortBy;
  @Pattern(regexp = "^$|ASC|DESC", flags = Pattern.Flag.CASE_INSENSITIVE)
  private String sortDirection;

  private Integer page = 1;
  private Integer size = 10;

  public int getOffset() {
    return (Math.max(1, this.page) - 1) * Math.max(1, this.size);
  }
}
