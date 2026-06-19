package com.ssafy.zipdaum.property.batch.scheduler;

import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.domain.RegionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Component
@Slf4j
@RequiredArgsConstructor
public class PropertyDataBatchScheduler {

    private final JobOperator jobOperator;

    // 매월 5일 새벽 3시에 실행
    @Scheduled(cron = "0 0 3 5 * *")
    public void runMonthlyPropertyDataJob() {
        String runDate = LocalDate.now().toString();
        log.info(">>>> [월간 실거래가 배치 시작] 최근 3개월 데이터 갱신");

        // 1개월 전, 2개월 전, 3개월 전 데이터 호출
        for (int i = 1; i <= 3; i++) {
            // 이번 루프에서 조회할 타겟 월 계산
            String targetMonth = YearMonth.now().minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM"));
            log.info(">> 대상 계약월: {}", targetMonth);

            // 부산 16개 구/군 순회
            for (RegionCode region : RegionCode.values()) {

                // 아파트/빌라 매매/전월세 4개 API 순회
                for (DealApiType apiType : DealApiType.values()) {
                    try {
                        Properties params = new Properties();
                        params.put("dealYmd", targetMonth);
                        params.put("lawdCd", region.getLawdCd());
                        params.put("apiType", apiType.name());
                        params.put("runDate", runDate);

                        jobOperator.start("propertyApiJob", params);

                        Thread.sleep(1000);

                    } catch (Exception e) {
                        log.error("배치 실행 실패: 월={}, 지역={}, 타입={}", targetMonth, region.getName(), apiType.name(), e);
                    }
                }
            }
        }
        log.info("<<<< [월간 실거래가 3개월 치 배치 실행 완료]");
    }

}
