package com.ssafy.zipdaum.auth.service;

import com.ssafy.zipdaum.auth.dto.AuthRequest;
import com.ssafy.zipdaum.auth.dto.AuthResponse;

public interface AuthService {
  AuthResponse login(AuthRequest authRequest);
}
