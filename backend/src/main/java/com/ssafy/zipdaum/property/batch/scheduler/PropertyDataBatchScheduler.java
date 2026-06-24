package com.ssafy.zipdaum.property.batch.scheduler;

import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.domain.RegionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Async;
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
        log.info(">>>> [월간 실거래가 배치 시작] 최근 3개월 데이터 갱신");
        for (int i = 1; i <= 3; i++) {
            String targetMonth = YearMonth.now().minusMonths(i).format(DateTimeFormatter.ofPattern("yyyyMM"));
            executeBatchForMonth(targetMonth); // 💡 분리한 메서드 호출
        }
        log.info("<<<< [월간 실거래가 3개월 치 배치 실행 완료]");
    }

    private void executeBatchForMonth(String targetMonth) {

        String runDate = String.valueOf(System.currentTimeMillis());

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

    @Async
    public void runManualPropertyDataJobByRange(String startMonthStr, String endMonthStr) {
        log.info(">>>> [관리자 수동 배치 시작] 수집 기간: {} ~ {}", startMonthStr, endMonthStr);
        long startTime = System.currentTimeMillis();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");
        YearMonth startMonth = YearMonth.parse(startMonthStr, formatter);
        YearMonth endMonth = YearMonth.parse(endMonthStr, formatter);

        if (startMonth.isAfter(endMonth)) {
            log.error("시작 연월이 종료 연월보다 클 수 없습니다.");
            return;
        }

        YearMonth currentMonth = startMonth;
        while (!currentMonth.isAfter(endMonth)) {
            String targetMonth = currentMonth.format(formatter);
            log.info(">> 대상 계약월: {} 실행 중...", targetMonth);

            executeBatchForMonth(targetMonth);

            currentMonth = currentMonth.plusMonths(1);
        }

        log.info("<<<< [관리자 수동 배치 완료] 수집 기간: {} ~ {}", startMonthStr, endMonthStr);
        long endTime = System.currentTimeMillis();
        log.info("스케줄러 비동기 호출 소요 시간: {} ms", (endTime - startTime));
    }

}
