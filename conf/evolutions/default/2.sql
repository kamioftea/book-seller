# --- !Ups

CREATE TABLE location (
  id BIGINT(20) UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(255)
);

INSERT INTO location (name) VALUES
  ('Box 1'),
  ('Box 2'),
  ('Box 3'),
  ('Box 4'),
  ('Box 5'),
  ('Box 6'),
  ('Green Crate 1'),
  ('Green Crate 2'),
  ('Black Crate 1');

CREATE TABLE stock (
  id BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  book_id BIGINT(20) UNSIGNED NOT NULL ,
  location_id BIGINT(20) UNSIGNED NOT NULL ,
  condition_description TEXT NULL,
  FOREIGN KEY fk_stock_book (book_id) REFERENCES book(id),
  FOREIGN KEY fk_stock_location (location_id) REFERENCES location(id)
);

# --- !Downs

DROP TABLE stock;

DROP TABLE location;
