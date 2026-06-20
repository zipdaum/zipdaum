package com.ssafy.zipdaum.interaction.mapper;

import com.ssafy.zipdaum.interaction.dto.UserPropertyInteractionSaveCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPropertyInteractionMapper {

  void saveUserPropertyInteraction(UserPropertyInteractionSaveCommand command);
}
