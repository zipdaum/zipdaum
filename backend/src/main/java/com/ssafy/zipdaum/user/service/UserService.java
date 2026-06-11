package com.ssafy.zipdaum.user.service;

import com.ssafy.zipdaum.user.dto.UserDto;

public interface UserService {
  void signUp(UserDto userDto);
  UserDto findById(Long id);
  void updateName(Long id, String name);
  void deleteById(Long id);
}
