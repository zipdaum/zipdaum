package com.ssafy.zipdaum.user.service;

import com.ssafy.zipdaum.user.mapper.UserMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletionScheduler {

  private final UserMapper userMapper;

  @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
  @Transactional
  public void deleteScheduledUsers() {
    int deletedRows = userMapper.deleteScheduledUsers(LocalDateTime.now());
    if (deletedRows > 0) {
      log.info("회원 물리 삭제 완료 count={}", deletedRows);
    }
  }
}
