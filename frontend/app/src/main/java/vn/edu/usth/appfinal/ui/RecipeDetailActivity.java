package vn.edu.usth.appfinal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import org.json.JSONObject;
import vn.edu.usth.appfinal.R;
import vn.edu.usth.appfinal.adapter.IngredientAdapter;
import vn.edu.usth.appfinal.adapter.InstructionAdapter;
import vn.edu.usth.appfinal.model.Recipe;
import vn.edu.usth.appfinal.network.ApiClient;
import vn.edu.usth.appfinal.network.ApiEndpoints;
import vn.edu.usth.appfinal.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================================================
 * [FRONTEND - UI] MÀN HÌNH CHI TIẾT CÔNG THỨC & DINH DƯỠNG (RecipeDetailActivity)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Chặn trực tiếp nếu khách (chưa đăng nhập) cố tình mở màn hình này.
 *   2. Tải toàn bộ chi tiết món ăn từ API (GET /api/recipes/{id}?userId={userId}).
 *   3. Hiển thị:
 *      + Ảnh món ăn (NetworkImageView + Volley ImageLoader).
 *      + Tiêu đề, mô tả, tên người chia sẻ (author).
 *      + Thời gian chuẩn bị, thời gian nấu, khẩu phần (servings).
 *      + Bảng phân tích dinh dưỡng chi tiết (Calories, Protein, Carbs, Fat).
 *      + Danh sách nguyên liệu (RecyclerView + IngredientAdapter).
 *      + Các bước hướng dẫn nấu (RecyclerView + InstructionAdapter).
 *   4. Thao tác yêu thích (Thả tim ♥): Gọi API thêm/xóa yêu thích và cập nhật icon tức thì.
 *   5. Quyền chỉnh sửa món ăn: Chỉ hiển thị nút "Sửa" nếu món ăn do chính tài khoản hiện tại tạo ra (không cho sửa 10 món mặc định của hệ thống).
 * - Câu hỏi bảo vệ:
 *   + "Thay đổi màu icon trái tim hoặc icon nút sửa ở đâu?": Xem updateFavoriteIcon() và initViews().
 *   + "Bảng dinh dưỡng hiển thị ở đâu?": Xem displayRecipe() - các TextView tvCalories, tvProtein, tvCarbs, tvFat.
 *   + "Nguyên liệu và các bước nấu hiển thị thế nào?": Xem rvIngredients và rvInstructions dùng RecyclerView lồng nhau tắt NestedScrolling.
 * =========================================================================================
 */
public class RecipeDetailActivity extends AppCompatActivity {

    // Khai báo các view giao diện
    private ImageView btnBack, btnDetailFavorite, btnDetailEdit;
    private TextView tvHeaderTitle, tvRecipeTitle, tvRecipeDescription;
    private TextView tvPrepTime, tvCookTime, tvServings;
    private TextView tvCalories, tvProtein, tvCarbs, tvFat;
    private TextView tvIngredientsHeader;
    private NetworkImageView ivRecipeDetailImage;
    private RecyclerView rvIngredients, rvInstructions;
    private ProgressBar progressBar;
    private View scrollViewContent;

    private long recipeId;
    private Recipe currentRecipe;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);

        // [BẢO MẬT & PHÂN QUYỀN]: Chặn trực tiếp nếu chưa đăng nhập mà cố tình vào Activity này
        if (!sessionManager.isLoggedIn()) {
            new AlertDialog.Builder(this)
                    .setTitle("Yêu cầu đăng nhập")
                    .setMessage("Bạn cần đăng nhập hoặc tạo tài khoản để xem chi tiết nguyên liệu, hướng dẫn nấu và bảng dinh dưỡng.")
                    .setCancelable(false)
                    .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                        startActivity(new Intent(RecipeDetailActivity.this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Đóng", (dialog, which) -> finish())
                    .show();
            return;
        }

        setContentView(R.layout.activity_recipe_detail);

        // Xử lý Edge-to-Edge căn chỉnh khoảng đệm hệ thống (tai thỏ / camera nốt ruồi)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayoutDetail), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Nhận ID món ăn được truyền từ Intent (khi bấm vào thẻ món ở trang chủ hoặc yêu thích)
        recipeId = getIntent().getLongExtra("recipe_id", -1);
        if (recipeId == -1) {
            Toast.makeText(this, "Không tìm thấy thông tin món ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadRecipeDetail();
    }

    /**
     * [ÁNH XẠ VIEW & BẮT SỰ KIỆN CLICK]:
     */
    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnDetailFavorite = findViewById(R.id.btnDetailFavorite);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvRecipeTitle = findViewById(R.id.tvRecipeTitle);
        tvRecipeDescription = findViewById(R.id.tvRecipeDescription);
        tvPrepTime = findViewById(R.id.tvPrepTime);
        tvCookTime = findViewById(R.id.tvCookTime);
        tvServings = findViewById(R.id.tvServings);
        tvCalories = findViewById(R.id.tvCalories);
        tvProtein = findViewById(R.id.tvProtein);
        tvCarbs = findViewById(R.id.tvCarbs);
        tvFat = findViewById(R.id.tvFat);
        tvIngredientsHeader = findViewById(R.id.tvIngredientsHeader);
        ivRecipeDetailImage = findViewById(R.id.ivRecipeDetailImage);
        rvIngredients = findViewById(R.id.rvIngredients);
        rvInstructions = findViewById(R.id.rvInstructions);
        progressBar = findViewById(R.id.progressBar);
        scrollViewContent = findViewById(R.id.scrollViewContent);

        // Cấu hình LayoutManager cho danh sách nguyên liệu và các bước nấu
        // Tắt NestedScrolling để cuộn mượt mà bên trong ScrollView cha
        rvIngredients.setLayoutManager(new LinearLayoutManager(this));
        rvIngredients.setNestedScrollingEnabled(false);
        rvInstructions.setLayoutManager(new LinearLayoutManager(this));
        rvInstructions.setNestedScrollingEnabled(false);

        // Nút quay lại (Back)
        btnBack.setOnClickListener(v -> finish());
        
        // Nút thả tim yêu thích
        btnDetailFavorite.setOnClickListener(v -> toggleFavorite());

        // Nút chỉnh sửa món ăn: Mặc định ẩn, chỉ hiển thị nếu là chính chủ khi tải chi tiết món
        btnDetailEdit = findViewById(R.id.btnDetailEdit);
        btnDetailEdit.setVisibility(View.GONE);
        btnDetailEdit.setOnClickListener(v -> showEditRecipeDialog());
    }

    /**
     * [TẢI DỮ LIỆU CHI TIẾT TỪ API]:
     * - Endpoint: GET /api/recipes/{id}?userId={userId}
     * - Header: X-User-Id
     */
    private void loadRecipeDetail() {
        progressBar.setVisibility(View.VISIBLE);
        scrollViewContent.setVisibility(View.GONE);

        long userId = sessionManager.getUserId();
        String url = ApiEndpoints.getRecipeDetailUrl(recipeId, userId);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    scrollViewContent.setVisibility(View.VISIBLE);

                    // Parse JSON response sang Model Recipe
                    currentRecipe = Recipe.fromJson(response);
                    // Đồng bộ kiểm tra trạng thái yêu thích và vẽ lên UI
                    checkFavoriteStatusAndDisplay();
                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                        new AlertDialog.Builder(RecipeDetailActivity.this)
                                .setTitle("Yêu cầu đăng nhập")
                                .setMessage("Vui lòng đăng nhập hoặc đăng ký tài khoản để xem chi tiết công thức và bảng dinh dưỡng.")
                                .setCancelable(false)
                                .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                                    startActivity(new Intent(RecipeDetailActivity.this, LoginActivity.class));
                                    finish();
                                })
                                .setNegativeButton("Đóng", (dialog, which) -> finish())
                                .show();
                    } else {
                        Toast.makeText(RecipeDetailActivity.this, "Không thể tải chi tiết món ăn", Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                if (sessionManager.isLoggedIn()) {
                    headers.put("X-User-Id", String.valueOf(sessionManager.getUserId()));
                }
                return headers;
            }
        };

        ApiClient.getQueue(this).add(request);
    }

    /**
     * Kiểm tra xem món ăn này có nằm trong danh sách yêu thích của người dùng không
     */
    private void checkFavoriteStatusAndDisplay() {
        if (currentRecipe == null) return;

        if (sessionManager.isLoggedIn()) {
            long userId = sessionManager.getUserId();
            String favUrl = ApiEndpoints.getFavoritesUrl(userId);

            JsonArrayRequest favReq = new JsonArrayRequest(
                    Request.Method.GET,
                    favUrl,
                    null,
                    response -> {
                        boolean isFav = false;
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.optJSONObject(i);
                            if (obj != null && obj.optLong("id") == recipeId) {
                                isFav = true;
                                break;
                            }
                        }
                        currentRecipe.setFavorite(isFav);
                        displayRecipe(currentRecipe);
                    },
                    error -> displayRecipe(currentRecipe)
            );
            ApiClient.getQueue(this).add(favReq);
        } else {
            displayRecipe(currentRecipe);
        }
    }

    /**
     * [HIỂN THỊ DỮ LIỆU LÊN GIAO DIỆN (BINDING DATA TO UI)]:
     * - Đây là nơi quyết định toàn bộ nội dung hiển thị của màn hình chi tiết.
     * - Thầy cô hỏi: "Chỗ nào gán tên món, calo, đạm, béo, thời gian nấu?": Chỉ vào hàm này!
     */
    private void displayRecipe(Recipe recipe) {
        if (recipe == null) return;

        // 1. Tiêu đề và Mô tả món ăn
        tvHeaderTitle.setText(recipe.getTitle());
        tvRecipeTitle.setText(recipe.getTitle());
        
        String rawDesc = recipe.getDescription();
        String desc = (rawDesc != null && !rawDesc.trim().isEmpty() && !rawDesc.trim().equalsIgnoreCase("null"))
                ? rawDesc.trim()
                : "Món ăn ngon và giàu dưỡng chất phù hợp cho cả gia đình.";
        
        // Hiển thị tên người chia sẻ công thức (nếu có)
        String author = recipe.getAuthorName();
        if (author != null && !author.trim().isEmpty() && !author.trim().equalsIgnoreCase("null") && !author.trim().equalsIgnoreCase("Hệ thống")) {
            desc += "\n\n👤 Người chia sẻ: " + author.trim();
        }
        tvRecipeDescription.setText(desc);

        // 2. Thời gian và khẩu phần
        tvPrepTime.setText(recipe.getPrepTime() + " min");
        tvCookTime.setText(recipe.getCookTime() + " min");
        tvServings.setText(String.valueOf(recipe.getServings() > 0 ? recipe.getServings() : 1));

        // 3. Bảng phân tích dinh dưỡng (Nutrition Facts - tính trên mỗi khẩu phần)
        tvCalories.setText(String.valueOf(recipe.getCalories()));
        tvProtein.setText(String.format("%.1fg", recipe.getProteinG()));
        tvCarbs.setText(String.format("%.1fg", recipe.getCarbsG()));
        tvFat.setText(String.format("%.1fg", recipe.getFatG()));

        // 4. Hình ảnh món ăn: Nạp bằng Volley ImageLoader, nếu không có URL thì hiển thị ảnh mặc định
        ImageLoader imageLoader = ApiClient.getInstance(this).getImageLoader();
        ivRecipeDetailImage.setVisibility(View.VISIBLE);
        ivRecipeDetailImage.setDefaultImageResId(R.drawable.ic_food_placeholder);
        ivRecipeDetailImage.setErrorImageResId(R.drawable.ic_food_placeholder);

        boolean hasImage = recipe.getImageUrl() != null 
                && !recipe.getImageUrl().trim().isEmpty() 
                && !recipe.getImageUrl().trim().equalsIgnoreCase("null");

        if (hasImage) {
            ivRecipeDetailImage.setImageUrl(recipe.getImageUrl().trim(), imageLoader);
        } else {
            ivRecipeDetailImage.setImageUrl(null, imageLoader);
            ivRecipeDetailImage.setImageResource(R.drawable.ic_food_placeholder);
        }

        // 5. Cập nhật icon Trái tim Yêu thích
        updateFavoriteIcon(recipe.isFavorite());

        // 6. Danh sách nguyên liệu (Ingredients)
        int ingCount = recipe.getIngredients() != null ? recipe.getIngredients().size() : 0;
        tvIngredientsHeader.setText(getString(R.string.ingredients) + " (" + ingCount + ")");
        rvIngredients.setAdapter(new IngredientAdapter(recipe.getIngredients()));

        // 7. Các bước hướng dẫn chế biến (Instructions)
        rvInstructions.setAdapter(new InstructionAdapter(recipe.getInstructions()));

        // 8. [QUYỀN CHỈNH SỬA]:
        // - Tài khoản phải đã đăng nhập
        // - Món ăn phải do chính tài khoản đó đăng (recipe.userId == currentUserId)
        // - Không cho phép chỉnh sửa 10 món mặc định của hệ thống (recipe.id <= 10)
        long currentUserId = sessionManager.getUserId();
        boolean isOwner = sessionManager.isLoggedIn()
                && recipe.getId() > 10
                && recipe.getUserId() != null
                && recipe.getUserId() == currentUserId;

        // Ẩn/Hiện nút Sửa trên giao diện
        btnDetailEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
    }

    /**
     * [ĐỔI ICON TRÁI TIM]:
     * - isFav = true  -> Trái tim đỏ tô đặc (ic_heart_filled)
     * - isFav = false -> Trái tim rỗng viền (ic_heart_outline)
     */
    private void updateFavoriteIcon(boolean isFav) {
        btnDetailFavorite.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    /**
     * [XỬ LÝ THẢ TIM YÊU THÍCH]:
     * - Bật/tắt trạng thái yêu thích:
     *   + Nếu chưa yêu thích -> Gửi POST /api/favorites/{recipeId}?userId={userId}
     *   + Nếu đã yêu thích -> Gửi DELETE /api/favorites/{recipeId}?userId={userId}
     * - Tự động rollback icon nếu request mạng thất bại.
     */
    private void toggleFavorite() {
        if (currentRecipe == null) return;

        // Nếu chưa đăng nhập thì nhắc nhở người dùng
        if (!sessionManager.isLoggedIn()) {
            new AlertDialog.Builder(this)
                    .setTitle("Lưu món yêu thích")
                    .setMessage("Bạn cần đăng nhập tài khoản để lưu món ăn này vào danh sách yêu thích.")
                    .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                        startActivity(new Intent(RecipeDetailActivity.this, LoginActivity.class));
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
            return;
        }

        long userId = sessionManager.getUserId();
        boolean willBeFav = !currentRecipe.isFavorite();
        currentRecipe.setFavorite(willBeFav);
        updateFavoriteIcon(willBeFav);

        if (willBeFav) {
            // Thêm vào danh sách yêu thích
            String addUrl = ApiEndpoints.getAddFavoriteUrl(currentRecipe.getId(), userId);
            JsonObjectRequest addReq = new JsonObjectRequest(
                    Request.Method.POST,
                    addUrl,
                    null,
                    resp -> Toast.makeText(this, "Đã thêm vào yêu thích ♥", Toast.LENGTH_SHORT).show(),
                    err -> {
                        // Rollback nếu lỗi mạng
                        currentRecipe.setFavorite(false);
                        updateFavoriteIcon(false);
                        Toast.makeText(this, "Không thể thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    }
            );
            ApiClient.getQueue(this).add(addReq);
        } else {
            // Xóa khỏi danh sách yêu thích
            String delUrl = ApiEndpoints.getRemoveFavoriteUrl(currentRecipe.getId(), userId);
            JsonObjectRequest delReq = new JsonObjectRequest(
                    Request.Method.DELETE,
                    delUrl,
                    null,
                    resp -> Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show(),
                    err -> {
                        // Rollback nếu lỗi mạng
                        currentRecipe.setFavorite(true);
                        updateFavoriteIcon(true);
                        Toast.makeText(this, "Không thể xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    }
            );
            ApiClient.getQueue(this).add(delReq);
        }
    }

    /**
     * [MỞ FORM CHỈNH SỬA MÓN ĂN]:
     * - Chỉ chính chủ mới mở được form này.
     * - Mở AddRecipeFragment với dữ liệu cũ của món ăn để chỉnh sửa.
     */
    private void showEditRecipeDialog() {
        if (currentRecipe == null) return;

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Vui lòng đăng nhập để chỉnh sửa món ăn", Toast.LENGTH_SHORT).show();
            return;
        }

        long currentUserId = sessionManager.getUserId();
        if (currentRecipe.getId() <= 10 || currentRecipe.getUserId() == null || currentRecipe.getUserId() != currentUserId) {
            Toast.makeText(this, "Bạn chỉ có thể chỉnh sửa món ăn do chính tài khoản của bạn đăng lên!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Mở full form chỉnh sửa (AddRecipeFragment) với dữ liệu đã điền sẵn
        AddRecipeFragment editFragment = AddRecipeFragment.newInstance(currentRecipe);
        editFragment.setOnRecipeAddedListener(updatedRecipe -> {
            if (updatedRecipe != null) {
                boolean wasFav = currentRecipe.isFavorite();
                currentRecipe = updatedRecipe;
                currentRecipe.setFavorite(wasFav);
                // Vẽ lại dữ liệu mới lên màn hình sau khi cập nhật thành công
                displayRecipe(currentRecipe);
            }
        });
        editFragment.show(getSupportFragmentManager(), "EditRecipeFragment");
    }
}
