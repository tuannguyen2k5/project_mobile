package com.recipe.service;

import com.recipe.dto.FavoriteRecipeDto;
import com.recipe.entity.Favorite;
import com.recipe.entity.Recipe;
import com.recipe.entity.User;
import com.recipe.repository.FavoriteRepository;
import com.recipe.repository.RecipeRepository;
import com.recipe.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * =========================================================================================
 * [BACKEND] SERVICE QUẢN LÝ MÓN ĂN YÊU THÍCH (Favorite Business Logic)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Đảm bảo đúng chuẩn Kiến trúc 3 lớp (Controller -> Service -> Repository).
 *   2. Controller chỉ nhận/trả HTTP request; toàn bộ logic kiểm tra User/Recipe và tương tác CSDL
 *      được đóng gói tại Service này.
 *   3. Lấy danh sách món ăn yêu thích của từng tài khoản người dùng.
 *   4. Thêm món ăn vào yêu thích (kiểm tra tồn tại, chống trùng lặp dữ liệu).
 *   5. Xóa món ăn khỏi danh sách yêu thích khi người dùng bỏ thả tim.
 * - Quản lý giao dịch:
 *   + @Transactional(readOnly = true) cho thao tác đọc giúp tối ưu bộ nhớ Hibernate cache.
 *   + @Transactional cho thao tác thêm/xóa đảm bảo tính toàn vẹn (ACID), tự động rollback nếu lỗi.
 * =========================================================================================
 */
@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;

    public FavoriteService(FavoriteRepository favoriteRepository,
                           UserRepository userRepository,
                           RecipeRepository recipeRepository) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.recipeRepository = recipeRepository;
    }

    /**
     * LẤY DANH SÁCH MÓN ĂN YÊU THÍCH CỦA NGƯỜI DÙNG
     * @param userId ID người dùng
     * @return Danh sách DTO món ăn đã được yêu thích
     */
    @Transactional(readOnly = true)
    public List<FavoriteRecipeDto> getUserFavorites(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID không hợp lệ");
        }

        // Truy vấn danh sách Favorite từ database (kèm Recipe qua JOIN FETCH)
        List<Favorite> favorites = favoriteRepository.findByUserId(userId);

        // Chuyển đổi sang danh sách DTO để trả về cho Client
        return favorites.stream()
                .map(fav -> FavoriteRecipeDto.from(fav.getRecipe()))
                .toList();
    }

    /**
     * THÊM MÓN ĂN VÀO DANH SÁCH YÊU THÍCH
     * @param recipeId ID món ăn
     * @param userId ID người dùng
     * @return true nếu thêm mới thành công, false nếu món ăn đã có trong danh sách từ trước
     */
    @Transactional
    public boolean addFavorite(Long recipeId, Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID không hợp lệ");
        }
        if (recipeId == null || recipeId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe ID không hợp lệ");
        }

        // Kiểm tra User có tồn tại trong CSDL không
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng với ID: " + userId));

        // Kiểm tra Recipe có tồn tại trong CSDL không
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy món ăn với ID: " + recipeId));

        // Kiểm tra nếu đã yêu thích rồi thì không thêm trùng
        if (favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId)) {
            return false; // Đã có sẵn trong danh sách
        }

        // Lưu bản ghi liên kết mới vào bảng favorites
        favoriteRepository.save(new Favorite(user, recipe));
        return true; // Thêm mới thành công
    }

    /**
     * XÓA MÓN ĂN KHỎI DANH SÁCH YÊU THÍCH
     * @param recipeId ID món ăn
     * @param userId ID người dùng
     */
    @Transactional
    public void removeFavorite(Long recipeId, Long userId) {
        if (userId == null || userId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID không hợp lệ");
        }
        if (recipeId == null || recipeId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipe ID không hợp lệ");
        }

        // Tìm bản ghi yêu thích trong MySQL
        Favorite favorite = favoriteRepository.findByUserIdAndRecipeId(userId, recipeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Món ăn này chưa có trong danh sách yêu thích"));

        // Xóa khỏi bảng favorites
        favoriteRepository.delete(favorite);
    }

    /**
     * Kiểm tra xem một món ăn có đang được người dùng yêu thích hay không
     */
    @Transactional(readOnly = true)
    public boolean isFavorite(Long recipeId, Long userId) {
        if (userId == null || userId <= 0 || recipeId == null || recipeId <= 0) {
            return false;
        }
        return favoriteRepository.existsByUserIdAndRecipeId(userId, recipeId);
    }
}
