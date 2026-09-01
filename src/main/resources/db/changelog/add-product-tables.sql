-- Create product table
CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    description VARCHAR(255) NOT NULL
);


CREATE TABLE order_product (
    order_id BIGINT NOT NULL REFERENCES "order" (id),
    product_id BIGINT NOT NULL REFERENCES product (id),
    PRIMARY KEY (order_id, product_id)
);


CREATE INDEX idx_order_product_product_id ON order_product (product_id);
