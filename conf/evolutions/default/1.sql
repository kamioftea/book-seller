# book & author

# --- !Ups

CREATE TABLE author (
  id BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY ,
  name VARCHAR(255) NOT NULL
);

CREATE TABLE book (
  id bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY ,
  title varchar(255) NULL,
  description text NULL,
  isbn VARCHAR(13) NOT NULL,
  thumbnail_url VARCHAR(255),
  published VARCHAR(10),
  type ENUM ('novel', 'short_stories', 'non_fiction', 'graphic_novel') NULL,
  format ENUM ('paperback', 'hardback') NULL,
  oversized TINYINT(1) UNSIGNED NOT NULL DEFAULT 0,
  notes TEXT NULL,
  series VARCHAR(255) NULL,
  series_position INT(10) UNSIGNED,
  UNIQUE KEY (isbn)
);

CREATE TABLE book_author (
  book_id BIGINT(20) UNSIGNED NOT NULL,
  author_id BIGINT(20) UNSIGNED NOT NULL,
  PRIMARY KEY (book_id, author_id),
  FOREIGN KEY fk_book_author_book_id (book_id) REFERENCES book (id) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY fk_book_author_author_id (author_id) REFERENCES author (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

# --- !Downs
DROP TABLE book_author;

DROP TABLE book;

DROP TABLE author;
