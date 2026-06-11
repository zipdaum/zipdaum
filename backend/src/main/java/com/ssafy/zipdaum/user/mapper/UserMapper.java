package com.ssafy.zipdaum.user.mapper;

import com.ssafy.zipdaum.user.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
  UserDto findByEmail(String email);
  UserDto findById(Long id);
  void insertUser(UserDto userDto);
}
