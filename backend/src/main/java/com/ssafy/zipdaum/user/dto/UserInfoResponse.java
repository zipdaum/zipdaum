package com.ssafy.zipdaum.user.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserInfoResponse {
  private final Long id;
  private final String email;
  private final String name;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;
}
