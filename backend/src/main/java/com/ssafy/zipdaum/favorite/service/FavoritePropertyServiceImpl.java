package com.ssafy.zipdaum.favorite.service;

import com.ssafy.zipdaum.favorite.dto.FavoritePropertyResponse;
import com.ssafy.zipdaum.favorite.mapper.FavoritePropertyMapper;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.RegionCode;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoritePropertyServiceImpl implements FavoritePropertyService {

  private final FavoritePropertyMapper favoritePropertyMapper;
  private final PropertyMapper propertyMapper;

  @Override
  @Transactional(readOnly = true)
  public List<FavoritePropertyResponse> findFavoriteProperties(Long userId) {
    List<FavoritePropertyResponse> favoriteProperties =
        favoritePropertyMapper.selectFavoriteProperties(userId, LocalDate.now().minusYears(1));

    favoriteProperties.forEach(property ->
        property.setRegionName(RegionCode.nameOf(property.getSggCd()))
    );

    return favoriteProperties;
  }

  @Override
  @Transactional
  public void saveFavoriteProperty(Long userId, Long propertyId) {
    if (!propertyMapper.existsPropertyById(propertyId)) {
      log.warn("존재하지 않는 주택 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    try {
      favoritePropertyMapper.insertFavoriteProperty(userId, propertyId);
    } catch (DuplicateKeyException e) {
      log.warn("이미 등록된 관심 주택 userId={}, propertyId={}", userId, propertyId);
      throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
    }

    log.info("관심 주택 등록 완료 userId={}, propertyId={}", userId, propertyId);
  }

  @Override
  @Transactional
  public void removeFavoriteProperty(Long userId, Long propertyId) {
    int deletedCount = favoritePropertyMapper.deleteFavoriteProperty(userId, propertyId);

    if (deletedCount == 0) {
      log.warn("관심 목록에 없는 주택 userId={}, propertyId={}", userId, propertyId);
      throw new BusinessException(ErrorCode.FAVORITE_NOT_FOUND);
    }

    log.info("관심 주택 해제 완료 userId={}, propertyId={}", userId, propertyId);
  }
}
