ALTER TABLE coupon_issues
    ADD CONSTRAINT uq_coupon_issues_coupon_user UNIQUE (coupon_id, user_id);