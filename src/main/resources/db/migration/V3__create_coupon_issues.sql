CREATE TABLE coupon_issues (
    id         BIGINT   AUTO_INCREMENT PRIMARY KEY,
    coupon_id  BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    reg_dt     DATETIME NOT NULL,
    CONSTRAINT fk_coupon_issue_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id),
    CONSTRAINT fk_coupon_issue_user   FOREIGN KEY (user_id)   REFERENCES users (id)
);
