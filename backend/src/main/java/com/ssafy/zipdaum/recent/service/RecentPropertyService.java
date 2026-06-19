package com.ssafy.zipdaum.recent.service;

import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
import java.util.List;

public interface RecentPropertyService {

  List<RecentPropertyResponse> findRecentProperties(Long userId);

  void recordRecentProperty(Long userId, Long propertyId);
}
