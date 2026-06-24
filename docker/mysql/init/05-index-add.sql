-- Property
ALTER TABLE property
    ADD INDEX idx_find_property (
	sgg_cd, umd_nm, jibun, property_type, name
);

ALTER TABLE property
    ADD FULLTEXT INDEX fx_property_search (name, umd_nm, jibun) WITH PARSER ngram;

ALTER TABLE recent_property
    ADD INDEX idx_user_updated (user_id, updated_at DESC, id DESC);


-- favorite_property
ALTER TABLE favorite_property
    ADD INDEX idx_favorite_property_user_created_id_property (
  user_id,
  created_at DESC,
  id DESC,
  property_id
);


-- favorite_region
ALTER TABLE favorite_region
    ADD INDEX idx_favorite_region_user_created (
  user_id, created_at DESC, id DESC
);


-- sale_deal
ALTER TABLE sale_deal
    ADD INDEX idx_sale_deal_property_deal_date_id (
  property_id,
  deal_date DESC,
  id DESC
);


-- rent_deal
ALTER TABLE rent_deal
    ADD INDEX idx_rent_deal_property_date_id (
  property_id, deal_date DESC, id DESC
);

ALTER TABLE rent_deal
    ADD INDEX idx_rent_deal_property_monthly_deal_date_id (
  property_id,
  monthly_rent,
  deal_date DESC,
  id DESC
);

ALTER TABLE rent_deal
    ADD INDEX idx_rent_deal_deal_date_property_monthly (
  deal_date,
  property_id,
  monthly_rent
);

