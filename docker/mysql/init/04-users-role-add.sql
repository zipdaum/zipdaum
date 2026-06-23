SET NAMES utf8mb4;

INSERT INTO users (
  email,
  password,
  name,
  is_deleted,
  created_at,
  updated_at,
  role
) VALUES (
  'admin@zipdaum.com',
  '$2a$10$N3SFCleoHkQg98sgAQCXw.EMRI9I91nGL6RgBnpryL3YM26dtgqxC',
  '최성보',
  FALSE,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP,
  'ROLE_ADMIN'
) ON DUPLICATE KEY UPDATE
  password = VALUES(password),
  name = VALUES(name),
  role = VALUES(role),
  updated_at = CURRENT_TIMESTAMP;
