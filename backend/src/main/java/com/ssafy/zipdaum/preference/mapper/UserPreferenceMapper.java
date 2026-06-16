package com.ssafy.zipdaum.preference.mapper;

import com.ssafy.zipdaum.preference.dto.UserPreferenceSaveCommand;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.dto.PreferenceTypeDto;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserPreferenceMapper {

  List<UserPreferenceResponse> selectUserPreferencesByUserId(@Param("userId") Long userId);

  List<PreferenceTypeDto> selectPreferenceTypesByCodes(@Param("codes") List<String> codes);

  void insertUserPreferences(
      @Param("userId") Long userId,
      @Param("items") List<UserPreferenceSaveCommand> items
  );

  int deleteUserPreferencesByUserId(@Param("userId") Long userId);
}
