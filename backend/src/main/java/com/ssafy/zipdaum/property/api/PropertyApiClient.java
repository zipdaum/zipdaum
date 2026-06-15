package com.ssafy.zipdaum.property.api;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.config.PropertyApiProperties;
import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.PropertyItem;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
@RequiredArgsConstructor
@Slf4j
public class PropertyApiClient {

  private static final int NUM_OF_ROWS = 1000;

  private final RestClient restClient;
  private final PropertyApiProperties properties;

  public List<PropertyItem> fetch(DealApiType apiType, String lawdCd, String dealYmd) {
    String response;
    try {
      response = restClient.get()
          .uri(URI.create(apiType.getUrl()
              + "?serviceKey=" + properties.getServiceKey()
              + "&LAWD_CD=" + lawdCd
              + "&DEAL_YMD=" + dealYmd
              + "&numOfRows=" + NUM_OF_ROWS
              + "&pageNo=1"))
          .retrieve()
          .body(String.class);
    } catch (ResourceAccessException e) {
      log.warn("공공데이터 API 응답 지연 또는 연결 실패 type={}, lawdCd={}, dealYmd={}",
          apiType, lawdCd, dealYmd);
      throw new BusinessException(ErrorCode.REAL_ESTATE_API_TIMEOUT);
    } catch (RestClientException e) {
      log.warn("공공데이터 API 호출 실패 type={}, lawdCd={}, dealYmd={}",
          apiType, lawdCd, dealYmd);
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }

    return parse(apiType, lawdCd, response);
  }

  private List<PropertyItem> parse(DealApiType apiType, String lawdCd, String xml) {
    if (xml == null || xml.isBlank()) {
      return List.of();
    }

    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      Element root = factory.newDocumentBuilder()
          .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
          .getDocumentElement();

      NodeList items = root.getElementsByTagName("item");
      List<PropertyItem> results = new ArrayList<>(items.getLength());
      for (int i = 0; i < items.getLength(); i++) {
        Element item = (Element) items.item(i);
        results.add(toDealItem(apiType, lawdCd, item));
      }
      return results;
    } catch (Exception e) {
      throw new IllegalStateException("실거래가 API XML 응답을 파싱하지 못했습니다.", e);
    }
  }

  private PropertyItem toDealItem(DealApiType apiType, String lawdCd, Element item) {
    LocalDate dealDate = LocalDate.of(
        parseInt(firstText(item, "dealYear", "년"), 1),
        parseInt(firstText(item, "dealMonth", "월"), 1),
        parseInt(firstText(item, "dealDay", "일"), 1)
    );

    return new PropertyItem(
        apiType,
        firstTextOrDefault(item, lawdCd, "sggCd", "지역코드"),
        firstText(item, "umdNm", "법정동"),
        firstText(item, "jibun", "지번"),
        firstText(item, "aptNm", "mhouseNm", "houseNm", "아파트", "연립다세대", "단지명"),
        parseInt(firstText(item, "buildYear", "건축년도", "건축년")),
        parseDecimal(firstText(item, "excluUseAr", "전용면적")),
        parseDecimal(firstText(item, "landAr", "plottageAr", "대지권면적", "토지면적")),
        parseLong(firstText(item, "dealAmount", "거래금액")),
        parseLong(firstText(item, "deposit", "depositAmount", "보증금액")),
        parseLong(firstText(item, "monthlyRent", "월세금액")),
        parseInt(firstText(item, "floor", "층")),
        firstText(item, "cdealType"),
        firstText(item, "cdealDay"),
        firstText(item, "dealingGbn"),
        firstText(item, "estateAgentSggNm"),
        firstText(item, "rgstDate"),
        firstText(item, "aptDong"),
        firstText(item, "buyerGbn", "매수자"),
        firstText(item, "slerGbn", "sellerGbn", "매도자"),
        firstText(item, "landLeaseholdGbn"),
        firstText(item, "contractTerm", "계약기간"),
        firstText(item, "contractType", "계약구분"),
        parseUseRrRight(firstText(item, "useRRRight", "useRrRight", "갱신요구권사용")),
        parseLong(firstText(item, "preDeposit", "종전계약보증금")),
        parseLong(firstText(item, "preMonthlyRent", "종전계약월세")),
        dealDate
    );
  }

  private String firstText(Element item, String... names) {
    for (String name : names) {
      String value = text(item, name);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String firstTextOrDefault(Element item, String defaultValue, String... names) {
    String value = firstText(item, names);
    return value == null ? defaultValue : value;
  }

  private String text(Element item, String name) {
    NodeList nodes = item.getElementsByTagName(name);
    if (nodes.getLength() == 0) {
      return null;
    }
    String value = nodes.item(0).getTextContent();
    if (value == null) {
      return null;
    }
    value = value.trim();
    return value.isBlank() ? null : value;
  }

  private Integer parseInt(String value) {
    if (value == null) {
      return null;
    }
    return Integer.parseInt(cleanNumber(value));
  }

  private int parseInt(String value, int defaultValue) {
    Integer parsed = parseInt(value);
    return parsed == null ? defaultValue : parsed;
  }

  private Long parseLong(String value) {
    if (value == null || "-".equals(value.trim())) {
      return null;
    }
    return Long.parseLong(cleanNumber(value));
  }

  private BigDecimal parseDecimal(String value) {
    if (value == null) {
      return null;
    }
    return new BigDecimal(cleanNumber(value));
  }

  private Boolean parseUseRrRight(String value) {
    if (value == null || "-".equals(value)) {
      return false;
    }
    return "사용".equals(value) || "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
  }

  private String cleanNumber(String value) {
    return value.replace(",", "").replace(" ", "").trim();
  }
}
