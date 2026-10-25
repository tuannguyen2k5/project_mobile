package com.recipe.repository;

import com.recipe.entity.Recipe;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    // Tìm theo tên trên toàn bộ database (chứa từ khóa, không phân biệt hoa thường)
    List<Recipe> findByTitleContainingIgnoreCase(String title, Sort sort);

    // Lấy 10 món fix cứng mặc định trong database (id <= 10)
    List<Recipe> findByIdLessThanEqual(Long maxId, Sort sort);

    // Tìm kiếm trong 10 món fix cứng mặc định (id <= 10)
    List<Recipe> findByIdLessThanEqualAndTitleContainingIgnoreCase(Long maxId, String title, Sort sort);
}
