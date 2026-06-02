CREATE TABLE product_category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE product (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    count_in_stock INT CHECK ( count_in_stock >= 0 ),
    price DOUBLE PRECISION CHECK ( price > 0 ),
    product_category_id INT REFERENCES product_category(id)
)