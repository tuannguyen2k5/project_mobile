package com.recipe.repository;

import com.recipe.entity.Recipe;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * =========================================================================================
 * [BACKEND] REPOSITORY TRUY VẤN DỮ LIỆU CÔNG THỨC MÓN ĂN (Recipe Data Access)
 * =========================================================================================
 * - Tương tác trực tiếp với bảng `recipes` trong MySQL bằng Spring Data JPA.
 * - Tự động sinh câu lệnh SQL tương ứng mà không cần viết SQL thủ công.
 * =========================================================================================
 */
@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    
    // Tìm kiếm món ăn theo từ khóa trên toàn bộ database (không phân biệt hoa thường)
    // Tương đương SQL: SELECT * FROM recipes WHERE LOWER(title) LIKE LOWER('%...%') ORDER BY ...
    List<Recipe> findByTitleContainingIgnoreCase(String title, Sort sort);

    // Lấy 10 món mặc định ban đầu của hệ thống (dành cho chế độ khách khi chưa đăng nhập)
    // Tương đương SQL: SELECT * FROM recipes WHERE id <= :maxId ORDER BY ...
    List<Recipe> findByIdLessThanEqual(Long maxId, Sort sort);

    // Tìm kiếm từ khóa chỉ trong phạm vi 10 món mặc định của khách
    // Tương đương SQL: SELECT * FROM recipes WHERE id <= :maxId AND LOWER(title) LIKE LOWER('%...%') ORDER BY ...
    List<Recipe> findByIdLessThanEqualAndTitleContainingIgnoreCase(Long maxId, String title, Sort sort);
}
