CREATE TABLE t_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(255)
);

CREATE TABLE t_order_line_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_code VARCHAR(255),
    price DECIMAL(19, 2),
    quantity INT
);

CREATE TABLE t_orders_order_line_items_list (
    order_id BIGINT NOT NULL,
    order_line_items_list_id BIGINT NOT NULL,
    PRIMARY KEY (order_id, order_line_items_list_id),
    FOREIGN KEY (order_id) REFERENCES t_orders(id),
    FOREIGN KEY (order_line_items_list_id) REFERENCES t_order_line_items(id)
);