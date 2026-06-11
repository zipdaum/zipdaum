package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.property.dto.CoordinateDto;

public interface GeocodeService {

  CoordinateDto getCoordinate(String address);
}
