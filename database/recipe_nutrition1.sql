-- =====================================================
-- Recipe Nutrition App - MySQL 8.x
-- Chạy file này trong MySQL Workbench hoặc: mysql -u root -p < recipe_nutrition.sql
-- =====================================================

DROP DATABASE IF EXISTS recipe_nutrition;
CREATE DATABASE recipe_nutrition
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE recipe_nutrition;

-- ---------------------- USERS ----------------------
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,          -- BCrypt (~60 ký tự)
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB;

-- ---------------------- RECIPES --------------------
CREATE TABLE recipes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(150) NOT NULL,
    description TEXT,
    image_url   VARCHAR(500),
    prep_time   INT NOT NULL DEFAULT 0,           -- phút
    cook_time   INT NOT NULL DEFAULT 0,           -- phút
    servings    INT NOT NULL DEFAULT 1,
    -- Dinh dưỡng tính trên MỖI KHẨU PHẦN (per serving)
    calories    INT NOT NULL DEFAULT 0,           -- kcal
    protein_g   DECIMAL(6,1) NOT NULL DEFAULT 0,
    carbs_g     DECIMAL(6,1) NOT NULL DEFAULT 0,
    fat_g       DECIMAL(6,1) NOT NULL DEFAULT 0,
    user_id     BIGINT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recipes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_recipes_title (title)
) ENGINE=InnoDB;

-- ---------------------- INGREDIENTS ----------------
CREATE TABLE ingredients (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id BIGINT NOT NULL,
    name      VARCHAR(150) NOT NULL,
    quantity  DECIMAL(8,2),                       -- cho phép NULL (vd: "chút muối")
    unit      VARCHAR(30),
    CONSTRAINT fk_ingredients_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
    INDEX idx_ingredients_recipe (recipe_id)
) ENGINE=InnoDB;

-- ---------------------- INSTRUCTIONS ---------------
CREATE TABLE instructions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipe_id   BIGINT NOT NULL,
    step_number INT NOT NULL,
    description TEXT NOT NULL,
    CONSTRAINT fk_instructions_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
    CONSTRAINT uq_instructions_step UNIQUE (recipe_id, step_number)
) ENGINE=InnoDB;

-- ---------------------- FAVORITES ------------------
CREATE TABLE favorites (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    recipe_id  BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_favorites_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_recipe
        FOREIGN KEY (recipe_id) REFERENCES recipes(id) ON DELETE CASCADE,
    CONSTRAINT uq_favorites_user_recipe UNIQUE (user_id, recipe_id)
) ENGINE=InnoDB;