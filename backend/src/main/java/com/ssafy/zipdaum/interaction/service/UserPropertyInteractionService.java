package com.ssafy.zipdaum.interaction.service;

import com.ssafy.zipdaum.interaction.dto.UserPropertyInteractionRequest;

public interface UserPropertyInteractionService {

  void saveInteraction(Long userId, Long propertyId, UserPropertyInteractionRequest request);
}
