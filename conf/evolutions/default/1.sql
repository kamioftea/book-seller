# Users schema

# --- !Ups

CREATE TABLE author (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE book (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  title varchar(255) NULL,
  description text NULL,
  isbn VARCHAR(13) NOT NULL,
  thumbnail_url VARCHAR(255),
  published VARCHAR(10),
  PRIMARY KEY (id),
  UNIQUE KEY (isbn)
);

CREATE TABLE book_author (
  book_id BIGINT(20),
  author_id BIGINT(20),
  PRIMARY KEY (book_id, author_id),
  FOREIGN KEY fk_book_author_book_id (book_id) REFERENCES book (id) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY fk_book_author_author_id (author_id) REFERENCES author (id) ON UPDATE CASCADE ON DELETE RESTRICT
);

# --- !Downs
DROP TABLE book_author;

DROP TABLE book;

DROP TABLE author;
