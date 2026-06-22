package com.ssafy.zipdaum.user.mapper;

import com.ssafy.zipdaum.user.dto.UserDto;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
  UserDto findByEmail(String email);
  UserDto findById(Long id);
  void insertUser(UserDto userDto);
  int updateNameById(@Param("id") Long id, @Param("name") String name);
  int softDeleteById(@Param("id") Long id, @Param("deletionScheduledAt") LocalDateTime deletionScheduledAt);
  int deleteScheduledUsers(@Param("now") LocalDateTime now);
}
