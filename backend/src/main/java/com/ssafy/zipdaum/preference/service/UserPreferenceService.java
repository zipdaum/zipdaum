package com.ssafy.zipdaum.preference.service;

import com.ssafy.zipdaum.preference.dto.UserPreferenceRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceRegionCandidateResponse;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import java.util.List;

public interface UserPreferenceService {

  List<UserPreferenceResponse> findPreferences(Long userId);

  List<UserPreferenceRegionCandidateResponse> findRegionCandidates(String keyword);

  void savePreferences(Long userId, UserPreferenceRequest request);

  void removePreferences(Long userId);
}
