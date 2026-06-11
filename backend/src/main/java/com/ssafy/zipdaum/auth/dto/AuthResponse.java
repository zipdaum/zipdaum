package com.ssafy.zipdaum.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthResponse {
  private final String accessToken;
  private final Long userId;
  private final String email;
  private final String name;
}
