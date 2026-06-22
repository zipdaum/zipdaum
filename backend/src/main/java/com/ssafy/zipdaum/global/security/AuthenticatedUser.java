package com.ssafy.zipdaum.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthenticatedUser {
  private final Long id;
  private final String email;
}
