package com.recipe.service;

import com.recipe.dto.RecipeDetailDto;
import com.recipe.dto.RecipeRequestDto;
import com.recipe.dto.RecipeSummaryDto;
import com.recipe.entity.Ingredients;
import com.recipe.entity.Instruction;
import com.recipe.entity.Recipe;
import com.recipe.repository.RecipeRepository;
import com.recipe.repository.UserRepository;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * =========================================================================================
 * [BACKEND] SERVICE QUẢN LÝ CÔNG THỨC & DINH DƯỠNG (Recipe Business Logic)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Phân quyền hiển thị danh sách: Khách chỉ xem 10 món mặc định; User đăng nhập xem toàn bộ.
 *   2. Tìm kiếm món ăn theo từ khóa không phân biệt hoa thường.
 *   3. Lấy chi tiết món ăn (kèm nguyên liệu, các bước nấu, chỉ số dinh dưỡng).
 *   4. Thêm công thức món ăn mới (tự động liên kết với User ID đang đăng nhập).
 *   5. Chỉnh sửa công thức món ăn: Ràng buộc bảo vệ nghiêm ngặt (chỉ chủ sở hữu món mới được sửa,
 *      10 món gốc của hệ thống có id <= 10 bị khóa không cho sửa).
 * - Câu hỏi bảo vệ thường gặp:
 *   + "Tại sao khách chỉ thấy 10 món?": Xem phương thức getRecipes() - kiểm tra if (isGuest).
 *   + "Logic cascade nguyên liệu và các bước nấu lưu thế nào?": Xem createRecipe() / updateRecipe().
 *   + "Chặn người khác sửa món của mình ở đâu?": Xem updateRecipe() - kiểm tra recipe.getUser().getId().equals(currentUserId).
 * =========================================================================================
 */
@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    public RecipeService(RecipeRepository recipeRepository, UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    /**
     * LẤY DANH SÁCH MÓN ĂN (Hỗ trợ tìm kiếm & phân quyền khách/đăng nhập)
     * - Khách (userId == null hoặc <= 0): Chỉ lấy tối đa 10 món mặc định ban đầu trong CSDL (id <= 10).
     * - Người dùng đã đăng nhập (userId > 0): Lấy toàn bộ món ăn (cả của hệ thống và của tất cả người dùng khác).
     * - Kết quả được sắp xếp theo thứ tự ID tăng dần.
     */
    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> getRecipes(String search, Long userId) {
        Sort sort = Sort.by("id");

        List<Recipe> recipes;
        boolean isGuest = (userId == null || userId <= 0);

        if (isGuest) {
            // [LOGIC KHÁCH] Giới hạn truy cập chỉ trong 10 món mặc định ban đầu của database
            if (search == null || search.isBlank()) {
                // Không có từ khóa tìm kiếm: Lấy tất cả món có id <= 10
                recipes = recipeRepository.findByIdLessThanEqual(10L, sort);
            } else {
                // Có từ khóa: Tìm kiếm chỉ trong các món có id <= 10
                recipes = recipeRepository.findByIdLessThanEqualAndTitleContainingIgnoreCase(10L, search.trim(), sort);
            }
        } else {
            // [LOGIC ĐÃ ĐĂNG NHẬP] Xem được toàn bộ món ăn của toàn hệ thống
            if (search == null || search.isBlank()) {
                // Không tìm kiếm: Lấy tất cả các món trong bảng recipes
                recipes = recipeRepository.findAll(sort);
            } else {
                // Có tìm kiếm: Tìm theo tên món ăn (chứa chuỗi con, không phân biệt hoa thường)
                recipes = recipeRepository.findByTitleContainingIgnoreCase(search.trim(), sort);
            }
        }

        // Chuyển đổi danh sách Entity Recipe sang DTO tóm tắt (RecipeSummaryDto) để tối ưu dung lượng mạng
        return recipes.stream()
                .map(RecipeSummaryDto::from)
                .toList();
    }

    /**
     * Overload method lấy danh sách món ăn khi không truyền userId (mặc định coi như khách)
     */
    @Transactional(readOnly = true)
    public List<RecipeSummaryDto> getRecipes(String search) {
        return getRecipes(search, null);
    }

    /**
     * LẤY CHI TIẾT 1 MÓN ĂN THEO ID
     * - Tìm món theo khóa chính (id). Nếu không thấy trả về mã HTTP 404 NOT_FOUND.
     * - Map Entity sang RecipeDetailDto gồm: dinh dưỡng, người đăng, nguyên liệu, hướng dẫn.
     */
    @Transactional(readOnly = true)
    public RecipeDetailDto getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy công thức với id: " + id));

        return RecipeDetailDto.from(recipe);
    }

    /**
     * TẠO MÓN ĂN MỚI
     * - Validate dữ liệu đầu vào (tên món bắt buộc, không được để trống).
     * - Gán User sở hữu nếu có userId hợp lệ.
     * - Map các trường dinh dưỡng (calories, protein, carbs, fat, thời gian chuẩn bị, thời gian nấu).
     * - Map danh sách nguyên liệu (Ingredients) và các bước nấu (Instructions) vào Entity Recipe.
     * - Lưu xuống DB (Cascade sẽ tự động insert vào bảng ingredients và instructions).
     */
    @Transactional
    public RecipeDetailDto createRecipe(RecipeRequestDto request, Long userId) {
        // Kiểm tra tính hợp lệ của request
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu công thức không được để trống");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên món ăn là bắt buộc");
        }

        Recipe recipe = new Recipe();
        
        // Liên kết với tài khoản người tạo (nếu có userId hợp lệ)
        if (userId != null && userId > 0) {
            userRepository.findById(userId).ifPresent(recipe::setUser);
        }
        
        // Gán các thông tin cơ bản của món ăn
        recipe.setTitle(request.title().trim());
        recipe.setDescription(request.description());
        recipe.setImageUrl(request.imageUrl());
        recipe.setPrepTime(request.prepTime() != null ? request.prepTime() : 0);
        recipe.setCookTime(request.cookTime() != null ? request.cookTime() : 0);
        recipe.setServings(request.servings() != null ? request.servings() : 1);
        
        // Gán bảng thành phần dinh dưỡng (Nutrition Facts)
        recipe.setCalories(request.calories() != null ? request.calories() : 0);
        recipe.setProteinG(request.proteinG() != null ? request.proteinG() : BigDecimal.ZERO);
        recipe.setCarbsG(request.carbsG() != null ? request.carbsG() : BigDecimal.ZERO);
        recipe.setFatG(request.fatG() != null ? request.fatG() : BigDecimal.ZERO);

        // Duyệt danh sách nguyên liệu và thêm vào món ăn
        if (request.ingredients() != null) {
            for (var ingDto : request.ingredients()) {
                if (ingDto != null && ingDto.name() != null && !ingDto.name().isBlank()) {
                    BigDecimal qty = ingDto.quantity() != null ? BigDecimal.valueOf(ingDto.quantity()) : null;
                    Ingredients ing = new Ingredients(ingDto.name().trim(), qty, ingDto.unit());
                    recipe.addIngredient(ing);
                }
            }
        }

        // Duyệt danh sách các bước nấu và đánh số thứ tự (step_number)
        if (request.instruction() != null) {
            int autoStep = 1;
            for (var instDto : request.instruction()) {
                if (instDto != null && instDto.description() != null && !instDto.description().isBlank()) {
                    int stepNum = instDto.stepNumber() != null ? instDto.stepNumber() : autoStep;
                    Instruction inst = new Instruction(stepNum, instDto.description().trim());
                    recipe.addInstruction(inst);
                    autoStep = stepNum + 1;
                }
            }
        }

        // Lưu vào MySQL: Nhờ CascadeType.ALL, cả Recipe, Ingredients và Instructions đều được insert
        Recipe saved = recipeRepository.save(recipe);
        return RecipeDetailDto.from(saved);
    }

    @Transactional
    public RecipeDetailDto createRecipe(RecipeRequestDto request) {
        return createRecipe(request, null);
    }

    /**
     * CẬP NHẬT MÓN ĂN (Chỉnh sửa công thức)
     * - Bảo vệ quyền hạn:
     *   1. 10 món mặc định của hệ thống (id <= 10) bị cấm sửa (HTTP 403 FORBIDDEN).
     *   2. Chỉ tài khoản tạo ra món ăn đó (currentUserId == recipe.user.id) mới được phép sửa.
     * - Cập nhật toàn bộ thông tin cơ bản, bảng dinh dưỡng, làm mới danh sách nguyên liệu và hướng dẫn.
     */
    @Transactional
    public RecipeDetailDto updateRecipe(Long id, RecipeRequestDto request, Long currentUserId) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID món ăn không được để trống");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu cập nhật không được để trống");
        }
        if (request.title() != null && request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên món ăn không được để trống");
        }

        // Tìm món ăn theo id, nếu không thấy báo lỗi 404
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy món ăn với ID: " + id));

        // [KIỂM TRA 1]: Chặn chỉnh sửa 10 món mặc định ban đầu của hệ thống
        if (recipe.getUser() == null || (recipe.getId() != null && recipe.getId() <= 10)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Không thể chỉnh sửa các món ăn mặc định của hệ thống."
            );
        }

        // [KIỂM TRA 2]: Kiểm tra quyền sở hữu món ăn
        if (currentUserId == null || !recipe.getUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bạn chỉ có quyền chỉnh sửa món ăn do chính tài khoản của mình đăng lên."
            );
        }

        // Cập nhật các trường thông tin cơ bản
        if (request.title() != null) {
            recipe.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            recipe.setDescription(request.description());
        }
        if (request.imageUrl() != null) {
            recipe.setImageUrl(request.imageUrl());
        }
        if (request.prepTime() != null) {
            recipe.setPrepTime(request.prepTime());
        }
        if (request.cookTime() != null) {
            recipe.setCookTime(request.cookTime());
        }
        if (request.servings() != null) {
            recipe.setServings(request.servings());
        }
        
        // Cập nhật các chỉ số dinh dưỡng
        if (request.calories() != null) {
            recipe.setCalories(request.calories());
        }
        if (request.proteinG() != null) {
            recipe.setProteinG(request.proteinG());
        }
        if (request.carbsG() != null) {
            recipe.setCarbsG(request.carbsG());
        }
        if (request.fatG() != null) {
            recipe.setFatG(request.fatG());
        }

        // Làm mới danh sách nguyên liệu: Xóa các nguyên liệu cũ và nạp lại từ request
        if (request.ingredients() != null) {
            recipe.getIngredients().clear();
            for (var ingDto : request.ingredients()) {
                if (ingDto != null && ingDto.name() != null && !ingDto.name().isBlank()) {
                    BigDecimal qty = ingDto.quantity() != null ? BigDecimal.valueOf(ingDto.quantity()) : null;
                    Ingredients ing = new Ingredients(ingDto.name().trim(), qty, ingDto.unit());
                    recipe.addIngredient(ing);
                }
            }
        }

        // Làm mới các bước hướng dẫn: Xóa các bước cũ và nạp lại từ request
        if (request.instruction() != null) {
            recipe.getInstruction().clear();
            int autoStep = 1;
            for (var instDto : request.instruction()) {
                if (instDto != null && instDto.description() != null && !instDto.description().isBlank()) {
                    int stepNum = instDto.stepNumber() != null ? instDto.stepNumber() : autoStep;
                    Instruction inst = new Instruction(stepNum, instDto.description().trim());
                    recipe.addInstruction(inst);
                    autoStep = stepNum + 1;
                }
            }
        }

        // Lưu bản cập nhật vào MySQL
        Recipe updated = recipeRepository.save(recipe);
        return RecipeDetailDto.from(updated);
    }

    @Transactional
    public RecipeDetailDto updateRecipe(Long id, RecipeRequestDto request) {
        return updateRecipe(id, request, null);
    }
}
