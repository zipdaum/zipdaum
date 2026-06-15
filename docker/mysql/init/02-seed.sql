USE zipdaum;

SET NAMES utf8mb4;

INSERT INTO preference_type (code, name, description)
VALUES
  ('BUDGET', '예산', '사용자가 원하는 예산'),
  ('AREA', '면적', '사용자가 원하는 면적'),
  ('BUILD_YEAR', '건축연도', '사용자가 원하는 건축연도'),
  ('REGION', '지역', '사용자가 선호하는 지역'),
  ('BUS', '버스', '버스 접근성 선호 여부'),
  ('SUBWAY', '지하철역', '지하철역 접근성 선호 여부'),
  ('HOSPITAL', '병원', '병원 접근성 선호 여부'),
  ('CCTV', '방범용 CCTV', '방범용 CCTV 접근성 선호 여부'),
  ('PARK', '공원', '공원 접근성 선호 여부');
