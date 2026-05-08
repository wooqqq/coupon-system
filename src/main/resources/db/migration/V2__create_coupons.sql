CREATE TABLE coupon (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    total_quantity INT          NOT NULL,
    issued_quantity INT         NOT NULL DEFAULT 0
);
