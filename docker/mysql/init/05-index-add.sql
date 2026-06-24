ALTER TABLE property
    ADD INDEX idx_find_property (
	sgg_cd, umd_nm, jibun, property_type, name
);

ALTER TABLE property
    ADD FULLTEXT INDEX fx_property_search (name, umd_nm, jibun);

ALTER TABLE recent_property
    ADD INDEX idx_user_updated (user_id, updated_at DESC, id DESC);