-- Table: author
CREATE TABLE author (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        name VARCHAR(255),
                        surname VARCHAR(255),
                        bio TEXT
);

-- Table: genre
CREATE TABLE genre (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(255)
);

-- Table: manga
CREATE TABLE manga (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       title VARCHAR(255),
                       description TEXT,
                       year INT NOT NULL,
                       chapters INT NOT NULL,
                       volumes INT NOT NULL,
                       author_id BIGINT,
                       CONSTRAINT fk_manga_author FOREIGN KEY (author_id) REFERENCES author(id)
);

-- JoinTable: manga_genre
CREATE TABLE manga_genre (
                             manga_id BIGINT NOT NULL,
                             genre_id BIGINT NOT NULL,
                             PRIMARY KEY (manga_id, genre_id),
                             CONSTRAINT fk_mg_manga FOREIGN KEY (manga_id) REFERENCES manga(id) ON DELETE CASCADE,
                             CONSTRAINT fk_mg_genre FOREIGN KEY (genre_id) REFERENCES genre(id) ON DELETE CASCADE
);

-- Table: users
CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
    );

-- Table: user_profiles
CREATE TABLE IF NOT EXISTS user_profiles (
                                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                             bio TEXT,
                                             avatar_url VARCHAR(255),
    user_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_up_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
    );

-- Table: user_manga_progress (User Library)
CREATE TABLE IF NOT EXISTS user_manga_progress (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   user_id BIGINT NOT NULL,
                                                   manga_id BIGINT NOT NULL,
                                                   status VARCHAR(50) NOT NULL, -- Enum: PLAN_TO_READ, READING, COMPLETED, ON_HOLD, DROPPED
    current_chapter INT DEFAULT 0,
    current_volume INT DEFAULT 0,
    CONSTRAINT fk_ump_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ump_manga FOREIGN KEY (manga_id) REFERENCES manga(id) ON DELETE CASCADE
    );

--- Initialize Seed Data ---
-- Default Admin profile (Replace placeholder with your hashed password)
INSERT INTO users (username, email, password, role)
VALUES ('admin', 'admin@example.com', '$2a$10$YOUR_ADMIN_BCRYPT_HASH_HERE', 'ROLE_ADMIN')
    ON DUPLICATE KEY UPDATE username=username;

-- Default User profile (Replace placeholder with your hashed password)
INSERT INTO users (username, email, password, role)
VALUES ('user123', 'user@example.com', '$2a$10$YOUR_USER_BCRYPT_HASH_HERE', 'ROLE_USER')
    ON DUPLICATE KEY UPDATE username=username;