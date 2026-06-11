USE zipdaum;

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(100) NOT NULL,
  password VARCHAR(255) NOT NULL,
  name VARCHAR(30) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE property (
  id BIGINT NOT NULL AUTO_INCREMENT,
  property_type VARCHAR(20) NOT NULL,
  name VARCHAR(100) NOT NULL,
  sgg_cd VARCHAR(10) NOT NULL,
  umd_nm VARCHAR(50) NOT NULL,
  jibun VARCHAR(50) NOT NULL,
  build_year SMALLINT NOT NULL,
  latitude DECIMAL(10, 7) NOT NULL,
  longitude DECIMAL(10, 7) NOT NULL,
  latest_sale_price BIGINT NOT NULL,
  latest_deposit BIGINT NOT NULL,
  latest_monthly_rent BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_property_region (sgg_cd, umd_nm),
  KEY idx_property_name (name),
  UNIQUE KEY uk_property_public_source (property_type, sgg_cd, umd_nm, jibun, name, build_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE sale_deal (
  id BIGINT NOT NULL AUTO_INCREMENT,
  property_id BIGINT NOT NULL,
  exclusive_area DECIMAL(8, 2) NOT NULL,
  land_area DECIMAL(8, 2) NULL,
  deal_amount BIGINT NOT NULL,
  floor SMALLINT NOT NULL,
  deal_date DATE NOT NULL,
  buyer_gbn VARCHAR(20) NOT NULL,
  seller_gbn VARCHAR(20) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_sale_deal_property_id (property_id),
  KEY idx_sale_deal_date (deal_date),
  UNIQUE KEY uk_sale_deal_public_source (property_id, exclusive_area, deal_amount, floor, deal_date),
  CONSTRAINT fk_sale_deal_property
    FOREIGN KEY (property_id) REFERENCES property (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rent_deal (
  id BIGINT NOT NULL AUTO_INCREMENT,
  property_id BIGINT NOT NULL,
  exclusive_area DECIMAL(8, 2) NOT NULL,
  land_area DECIMAL(8, 2) NULL,
  deposit BIGINT NOT NULL,
  monthly_rent BIGINT NOT NULL,
  floor SMALLINT NOT NULL,
  contract_term VARCHAR(30) NOT NULL,
  contract_type VARCHAR(20) NOT NULL,
  use_rr_right BOOLEAN NOT NULL,
  pre_deposit BIGINT NULL,
  pre_monthly_rent BIGINT NULL,
  deal_date DATE NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_rent_deal_property_id (property_id),
  KEY idx_rent_deal_date (deal_date),
  UNIQUE KEY uk_rent_deal_public_source (property_id, exclusive_area, deposit, monthly_rent, floor, deal_date),
  CONSTRAINT fk_rent_deal_property
    FOREIGN KEY (property_id) REFERENCES property (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE favorite_property (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  property_id BIGINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_favorite_property_user_property (user_id, property_id),
  KEY idx_favorite_property_property_id (property_id),
  CONSTRAINT fk_favorite_property_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_favorite_property_property
    FOREIGN KEY (property_id) REFERENCES property (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE favorite_region (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sgg_cd VARCHAR(10) NOT NULL,
  umd_nm VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_favorite_region_user_region (user_id, sgg_cd, umd_nm),
  CONSTRAINT fk_favorite_region_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE condition_type (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(50) NOT NULL,
  name VARCHAR(50) NOT NULL,
  description VARCHAR(255) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_condition_type_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE condition_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  condition_type_id BIGINT NOT NULL,
  condition_value VARCHAR(100) NOT NULL,
  priority TINYINT NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_condition_item_value (condition_value),
  KEY idx_condition_item_user_id (user_id),
  KEY idx_condition_item_type_id (condition_type_id),
  CONSTRAINT fk_condition_item_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_condition_item_condition_type
    FOREIGN KEY (condition_type_id) REFERENCES condition_type (id)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  content VARCHAR(255) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notification_user_id (user_id),
  KEY idx_notification_is_read (is_read),
  CONSTRAINT fk_notification_user
    FOREIGN KEY (user_id) REFERENCES users (id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
