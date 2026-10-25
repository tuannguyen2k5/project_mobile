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

public class RecipeDetailActivity extends AppCompatActivity {

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

        // Chặn trực tiếp nếu Guest cố tình mở Activity này
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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayoutDetail), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        recipeId = getIntent().getLongExtra("recipe_id", -1);
        if (recipeId == -1) {
            Toast.makeText(this, "Không tìm thấy thông tin món ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadRecipeDetail();
    }

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

        rvIngredients.setLayoutManager(new LinearLayoutManager(this));
        rvIngredients.setNestedScrollingEnabled(false);
        rvInstructions.setLayoutManager(new LinearLayoutManager(this));
        rvInstructions.setNestedScrollingEnabled(false);

        btnBack.setOnClickListener(v -> finish());
        btnDetailFavorite.setOnClickListener(v -> toggleFavorite());

        btnDetailEdit = findViewById(R.id.btnDetailEdit);
        btnDetailEdit.setVisibility(View.GONE); // Mặc định ẩn, chỉ hiển thị nếu là chính chủ khi tải chi tiết món
        btnDetailEdit.setOnClickListener(v -> showEditRecipeDialog());
    }

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

                    currentRecipe = Recipe.fromJson(response);
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

    private void displayRecipe(Recipe recipe) {
        if (recipe == null) return;

        tvHeaderTitle.setText(recipe.getTitle());
        tvRecipeTitle.setText(recipe.getTitle());
        String rawDesc = recipe.getDescription();
        String desc = (rawDesc != null && !rawDesc.trim().isEmpty() && !rawDesc.trim().equalsIgnoreCase("null"))
                ? rawDesc.trim()
                : "Món ăn ngon và giàu dưỡng chất phù hợp cho cả gia đình.";
        String author = recipe.getAuthorName();
        if (author != null && !author.trim().isEmpty() && !author.trim().equalsIgnoreCase("null") && !author.trim().equalsIgnoreCase("Hệ thống")) {
            desc += "\n\n👤 Người chia sẻ: " + author.trim();
        }
        tvRecipeDescription.setText(desc);

        tvPrepTime.setText(recipe.getPrepTime() + " min");
        tvCookTime.setText(recipe.getCookTime() + " min");
        tvServings.setText(String.valueOf(recipe.getServings() > 0 ? recipe.getServings() : 1));

        // Nutrition Facts (per serving)
        tvCalories.setText(String.valueOf(recipe.getCalories()));
        tvProtein.setText(String.format("%.1fg", recipe.getProteinG()));
        tvCarbs.setText(String.format("%.1fg", recipe.getCarbsG()));
        tvFat.setText(String.format("%.1fg", recipe.getFatG()));

        // Image - Luôn hiển thị khung ảnh, nếu không có URL thì hiển thị placeholder
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

        // Favorite Button Icon
        updateFavoriteIcon(recipe.isFavorite());

        // Ingredients
        int ingCount = recipe.getIngredients() != null ? recipe.getIngredients().size() : 0;
        tvIngredientsHeader.setText(getString(R.string.ingredients) + " (" + ingCount + ")");
        rvIngredients.setAdapter(new IngredientAdapter(recipe.getIngredients()));

        // Instructions
        rvInstructions.setAdapter(new InstructionAdapter(recipe.getInstructions()));

        // Quyền chỉnh sửa:
        // - Chỉ tài khoản đã đăng nhập mới có thể có quyền
        // - Món ăn phải là do chính tài khoản đó đăng (recipe.userId == currentUserId)
        // - Không cho phép chỉnh sửa 10 món mặc định của hệ thống (recipe.id <= 10 hoặc userId == null)
        long currentUserId = sessionManager.getUserId();
        boolean isOwner = sessionManager.isLoggedIn()
                && recipe.getId() > 10
                && recipe.getUserId() != null
                && recipe.getUserId() == currentUserId;

        btnDetailEdit.setVisibility(isOwner ? View.VISIBLE : View.GONE);
    }

    private void updateFavoriteIcon(boolean isFav) {
        btnDetailFavorite.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
    }

    private void toggleFavorite() {
        if (currentRecipe == null) return;

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
            // POST /api/favorites/{recipeId}?userId={userId}
            String addUrl = ApiEndpoints.getAddFavoriteUrl(currentRecipe.getId(), userId);
            JsonObjectRequest addReq = new JsonObjectRequest(
                    Request.Method.POST,
                    addUrl,
                    null,
                    resp -> Toast.makeText(this, "Đã thêm vào yêu thích ♥", Toast.LENGTH_SHORT).show(),
                    err -> {
                        currentRecipe.setFavorite(false);
                        updateFavoriteIcon(false);
                        Toast.makeText(this, "Không thể thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    }
            );
            ApiClient.getQueue(this).add(addReq);
        } else {
            // DELETE /api/favorites/{recipeId}?userId={userId}
            String delUrl = ApiEndpoints.getRemoveFavoriteUrl(currentRecipe.getId(), userId);
            JsonObjectRequest delReq = new JsonObjectRequest(
                    Request.Method.DELETE,
                    delUrl,
                    null,
                    resp -> Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show(),
                    err -> {
                        currentRecipe.setFavorite(true);
                        updateFavoriteIcon(true);
                        Toast.makeText(this, "Không thể xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    }
            );
            ApiClient.getQueue(this).add(delReq);
        }
    }

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

        // Mở full form chỉnh sửa (AddRecipeFragment) với đầy đủ thông tin:
        // Thông tin cơ bản, Dinh dưỡng, Nguyên liệu động, và Công thức chế biến từng bước
        AddRecipeFragment editFragment = AddRecipeFragment.newInstance(currentRecipe);
        editFragment.setOnRecipeAddedListener(updatedRecipe -> {
            if (updatedRecipe != null) {
                boolean wasFav = currentRecipe.isFavorite();
                currentRecipe = updatedRecipe;
                currentRecipe.setFavorite(wasFav);
                displayRecipe(currentRecipe);
            }
        });
        editFragment.show(getSupportFragmentManager(), "EditRecipeFragment");
    }
}
