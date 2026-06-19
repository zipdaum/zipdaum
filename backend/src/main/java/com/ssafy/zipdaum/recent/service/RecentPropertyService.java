package com.ssafy.zipdaum.recent.service;

import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
import com.ssafy.zipdaum.recent.dto.RecentPropertySaveRequest;
import java.util.List;

public interface RecentPropertyService {

  List<RecentPropertyResponse> findRecentProperties(Long userId);

  void saveRecentProperty(Long userId, RecentPropertySaveRequest request);
}
