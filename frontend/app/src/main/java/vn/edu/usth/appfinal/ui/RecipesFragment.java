package vn.edu.usth.appfinal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;
import vn.edu.usth.appfinal.R;
import vn.edu.usth.appfinal.adapter.RecipeAdapter;
import vn.edu.usth.appfinal.model.Recipe;
import vn.edu.usth.appfinal.network.ApiClient;
import vn.edu.usth.appfinal.network.ApiEndpoints;
import vn.edu.usth.appfinal.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecipesFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener, RecipeAdapter.OnFavoriteClickListener {

    private RecyclerView rvRecipes;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private View layoutError;
    private TextView tvErrorMessage;
    private AppCompatButton btnRetry;

    private String currentSearchQuery = "";
    private SessionManager sessionManager;
    private final List<Recipe> currentRecipes = new ArrayList<>();
    private final Set<Long> favoriteRecipeIds = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_recipes, container, false);

        sessionManager = SessionManager.getInstance(requireContext());

        rvRecipes = view.findViewById(R.id.rvRecipes);
        progressBar = view.findViewById(R.id.progressBar);
        layoutError = view.findViewById(R.id.layoutError);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);

        rvRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecipeAdapter(new ArrayList<>());
        adapter.setOnRecipeClickListener(this);
        adapter.setOnFavoriteClickListener(this);
        rvRecipes.setAdapter(adapter);

        btnRetry.setOnClickListener(v -> loadRecipes(currentSearchQuery));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecipes(currentSearchQuery);
    }

    public void searchRecipes(String query) {
        this.currentSearchQuery = query != null ? query : "";
        loadRecipes(this.currentSearchQuery);
    }

    private void loadRecipes(String search) {
        if (!isAdded()) return;

        progressBar.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);

        // If user is logged in, sync favorite list first or in parallel
        if (sessionManager.isLoggedIn()) {
            long userId = sessionManager.getUserId();
            String favUrl = ApiEndpoints.getFavoritesUrl(userId);

            JsonArrayRequest favReq = new JsonArrayRequest(
                    Request.Method.GET,
                    favUrl,
                    null,
                    response -> {
                        favoriteRecipeIds.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.optJSONObject(i);
                            if (obj != null) {
                                long id = obj.optLong("id", 0);
                                if (id > 0) favoriteRecipeIds.add(id);
                            }
                        }
                        fetchRecipesFromServer(search);
                    },
                    error -> {
                        favoriteRecipeIds.clear();
                        fetchRecipesFromServer(search);
                    }
            );
            ApiClient.getQueue(requireContext()).add(favReq);
        } else {
            favoriteRecipeIds.clear();
            fetchRecipesFromServer(search);
        }
    }

    private void fetchRecipesFromServer(String search) {
        if (!isAdded()) return;

        Long userId = sessionManager.isLoggedIn() ? sessionManager.getUserId() : null;
        String url = ApiEndpoints.getRecipesUrl(search, userId);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);

                    currentRecipes.clear();
                    boolean isGuest = !sessionManager.isLoggedIn();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        if (obj != null) {
                            Recipe r = Recipe.fromJson(obj);
                            // Ở chế độ khách: CHỈ hiển thị danh sách 10 món fix cứng trong database (id <= 10)
                            if (isGuest && r.getId() > 10) {
                                continue;
                            }
                            if (favoriteRecipeIds.contains(r.getId())) {
                                r.setFavorite(true);
                            }
                            currentRecipes.add(r);
                        }
                    }

                    if (currentRecipes.isEmpty()) {
                        layoutError.setVisibility(View.VISIBLE);
                        tvErrorMessage.setText(search.isEmpty()
                                ? "Chưa có công thức món ăn nào trên máy chủ."
                                : "Không tìm thấy món ăn nào khớp với: \"" + search + "\"");
                        btnRetry.setVisibility(View.GONE);
                    } else {
                        layoutError.setVisibility(View.GONE);
                    }

                    adapter.updateData(currentRecipes);
                },
                error -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    layoutError.setVisibility(View.VISIBLE);
                    btnRetry.setVisibility(View.VISIBLE);
                    tvErrorMessage.setText("Không thể kết nối đến Backend: " + ApiEndpoints.getBaseUrl() + "\nVui lòng đảm bảo Spring Boot đang chạy.");
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

        ApiClient.getQueue(requireContext()).add(request);
    }

    @Override
    public void onRecipeClick(Recipe recipe) {
        // GUEST CHECK: Guest chỉ được xem danh sách, không thể bấm vào xem chi tiết nếu chưa đăng ký / đăng nhập
        if (!sessionManager.isLoggedIn()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Yêu cầu đăng nhập")
                    .setMessage("Bạn cần đăng ký hoặc đăng nhập tài khoản để xem chi tiết nguyên liệu, các bước nấu và bảng dinh dưỡng của món ăn này.")
                    .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
            return;
        }

        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Recipe recipe, int position) {
        if (!sessionManager.isLoggedIn()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Yêu cầu đăng nhập")
                    .setMessage("Bạn cần đăng nhập tài khoản để lưu món ăn này vào danh sách yêu thích.")
                    .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                        startActivity(new Intent(requireContext(), LoginActivity.class));
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
            return;
        }

        long userId = sessionManager.getUserId();
        boolean willBeFavorite = !recipe.isFavorite();

        // Optimistic UI update
        recipe.setFavorite(willBeFavorite);
        if (willBeFavorite) {
            favoriteRecipeIds.add(recipe.getId());
        } else {
            favoriteRecipeIds.remove(recipe.getId());
        }
        adapter.notifyItemChanged(position);

        if (willBeFavorite) {
            // POST /api/favorites/{recipeId}?userId={userId}
            String addUrl = ApiEndpoints.getAddFavoriteUrl(recipe.getId(), userId);
            JsonObjectRequest addReq = new JsonObjectRequest(
                    Request.Method.POST,
                    addUrl,
                    null,
                    resp -> Toast.makeText(requireContext(), "Đã thêm vào yêu thích ♥", Toast.LENGTH_SHORT).show(),
                    err -> {
                        recipe.setFavorite(false);
                        favoriteRecipeIds.remove(recipe.getId());
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Không thể thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    }
            );
            ApiClient.getQueue(requireContext()).add(addReq);
        } else {
            // DELETE /api/favorites/{recipeId}?userId={userId}
            String delUrl = ApiEndpoints.getRemoveFavoriteUrl(recipe.getId(), userId);
            JsonObjectRequest delReq = new JsonObjectRequest(
                    Request.Method.DELETE,
                    delUrl,
                    null,
                    resp -> Toast.makeText(requireContext(), "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show(),
                    err -> {
                        recipe.setFavorite(true);
                        favoriteRecipeIds.add(recipe.getId());
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Không thể xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    }
            );
            ApiClient.getQueue(requireContext()).add(delReq);
        }
    }
}
