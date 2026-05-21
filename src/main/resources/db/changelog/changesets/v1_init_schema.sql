--liquibase formatted sql
--changeset chaika:1

--users
CREATE TABLE users (
id BIGSERIAL PRIMARY KEY,
name VARCHAR(255) NOT NULL,
surname VARCHAR(255) NOT NULL,
birth_date DATE NOT NULL,
email VARCHAR(255) NOT NULL UNIQUE,
active BOOLEAN DEFAULT TRUE NOT NULL,
created_at TIMESTAMP WITH TIME ZONE NOT NULL,
updated_at TIMESTAMP WITH TIME ZONE
);

--payment_cards
CREATE TABLE payment_cards (
id BIGSERIAL PRIMARY KEY,
user_id BIGINT NOT NULL,
number VARCHAR(16) NOT NULL UNIQUE,
holder VARCHAR(255) NOT NULL,
expiration_date DATE NOT NULL,
active BOOLEAN DEFAULT TRUE NOT NULL,
created_at TIMESTAMP WITH TIME ZONE NOT NULL,
updated_at TIMESTAMP WITH TIME ZONE
);

--Indexes
CREATE INDEX idx_payment_cards_user_id ON payment_cards (user_id);
CREATE INDEX idx_users_lower_name ON users (lower(name));
CREATE INDEX idx_users_lower_surname ON users (lower(surname));

--FK
ALTER TABLE payment_cards ADD CONSTRAINT fk_payment_cards_user
FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;