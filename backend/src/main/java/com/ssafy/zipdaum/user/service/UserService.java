package com.ssafy.zipdaum.user.service;

import com.ssafy.zipdaum.user.dto.UserDeleteRequest;
import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.dto.UserSignUpRequest;

public interface UserService {
  void signUp(UserSignUpRequest request);
  UserDto findById(Long id);
  void updateName(Long id, String name);
  void requestDeletion(Long id, UserDeleteRequest request);
}
