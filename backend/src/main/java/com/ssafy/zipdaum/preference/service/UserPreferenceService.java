package com.ssafy.zipdaum.preference.service;

import com.ssafy.zipdaum.preference.dto.UserPreferenceRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import java.util.List;

public interface UserPreferenceService {

  List<UserPreferenceResponse> findPreferences(Long userId);

  void savePreferences(Long userId, UserPreferenceRequest request);

  void removePreferences(Long userId);
}
