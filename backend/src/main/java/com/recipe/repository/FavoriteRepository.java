package com.recipe.repository;

import com.recipe.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * =========================================================================================
 * [BACKEND] REPOSITORY QUẢN LÝ MÓN ĂN YÊU THÍCH (Favorite Data Access)
 * =========================================================================================
 * - Tương tác với bảng trung gian `favorites` (liên kết nhiều - nhiều giữa users và recipes).
 * =========================================================================================
 */
@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // Lấy danh sách món yêu thích của 1 user theo ID, dùng JOIN FETCH để nạp luôn thông tin Recipe trong 1 query (tránh lỗi N+1)
    // Sắp xếp mới nhất lên đầu (ORDER BY f.id DESC)
    @Query("SELECT f FROM Favorite f JOIN FETCH f.recipe WHERE f.user.id = :userId ORDER BY f.id DESC")
    List<Favorite> findByUserId(@Param("userId") Long userId);

    // Tìm bản ghi yêu thích theo cặp userId và recipeId
    Optional<Favorite> findByUserIdAndRecipeId(Long userId, Long recipeId);

    // Kiểm tra nhanh xem user đã yêu thích món ăn này chưa (trả về true/false)
    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

    // Xóa món ăn khỏi danh sách yêu thích
    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);
}
