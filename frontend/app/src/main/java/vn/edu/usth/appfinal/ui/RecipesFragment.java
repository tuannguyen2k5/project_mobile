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

/**
 * =========================================================================================
 * [FRONTEND - UI] FRAGMENT HIỂN THỊ DANH SÁCH CÔNG THỨC (Tab "Công thức")
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Tải danh sách món ăn từ Backend qua API GET /api/recipes (hỗ trợ tìm kiếm theo từ khóa).
 *   2. Phân quyền hiển thị:
 *      + Khách (chưa đăng nhập): Chỉ hiển thị 10 món mặc định của CSDL (id <= 10).
 *      + Đã đăng nhập: Xem toàn bộ các món của hệ thống và người dùng khác.
 *   3. Đồng bộ trạng thái yêu thích (trái tim đỏ/trắng) dựa trên danh sách món đã lưu của user.
 *   4. Bấm vào món: Khách bị chặn bằng Dialog yêu cầu đăng nhập; User đã đăng nhập được mở RecipeDetailActivity.
 *   5. Bấm vào icon trái tim trên thẻ: Cập nhật giao diện lập tức (Optimistic UI) và gửi API thêm/xóa yêu thích.
 * - Câu hỏi bảo vệ:
 *   + "Tại sao khách chỉ thấy 10 món?": Xem dòng kiểm tra `if (isGuest && r.getId() > 10) continue;`.
 *   + "Optimistic UI là gì?": Cập nhật icon trái tim ngay trước khi API phản hồi để tạo cảm giác mượt mà,
 *     nếu API trả về lỗi thì mới hoàn tác lại (rollback).
 * =========================================================================================
 */
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

        // Ánh xạ các thành phần giao diện
        rvRecipes = view.findViewById(R.id.rvRecipes);
        progressBar = view.findViewById(R.id.progressBar);
        layoutError = view.findViewById(R.id.layoutError);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);

        // Cài đặt RecyclerView và Adapter
        rvRecipes.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecipeAdapter(new ArrayList<>());
        adapter.setOnRecipeClickListener(this);     // Bắt sự kiện click vào thẻ món ăn
        adapter.setOnFavoriteClickListener(this);   // Bắt sự kiện click vào icon trái tim
        rvRecipes.setAdapter(adapter);

        // Nút thử lại khi kết nối mạng lỗi
        btnRetry.setOnClickListener(v -> loadRecipes(currentSearchQuery));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tự động tải lại danh sách món ăn mỗi khi quay lại màn hình
        loadRecipes(currentSearchQuery);
    }

    /**
     * Hàm nhận từ khóa tìm kiếm từ thanh Search của MainActivity
     */
    public void searchRecipes(String query) {
        this.currentSearchQuery = query != null ? query : "";
        loadRecipes(this.currentSearchQuery);
    }

    /**
     * [LUỒNG TẢI DỮ LIỆU TỔNG THỂ]:
     * - Nếu đã đăng nhập: Tải danh sách yêu thích trước để biết món nào đã thả tim, sau đó tải danh sách món.
     * - Nếu là khách: Bỏ qua bước kiểm tra yêu thích, tải thẳng danh sách món mặc định.
     */
    private void loadRecipes(String search) {
        if (!isAdded()) return;

        progressBar.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);

        if (sessionManager.isLoggedIn()) {
            long userId = sessionManager.getUserId();
            String favUrl = ApiEndpoints.getFavoritesUrl(userId);

            // Nạp danh sách ID các món đã yêu thích
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

    /**
     * [GỌI API LẤY DANH SÁCH MÓN ĂN]:
     * - Endpoint: GET /api/recipes?search={query}&userId={userId}
     */
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
                            // [CHẶN KHÁCH]: Khách chỉ được xem 10 món mặc định của CSDL (id <= 10)
                            if (isGuest && r.getId() > 10) {
                                continue;
                            }
                            // Đánh dấu true nếu món này nằm trong danh sách đã yêu thích
                            if (favoriteRecipeIds.contains(r.getId())) {
                                r.setFavorite(true);
                            }
                            currentRecipes.add(r);
                        }
                    }

                    // Nếu danh sách rỗng -> Hiện thông báo giao diện trống
                    if (currentRecipes.isEmpty()) {
                        layoutError.setVisibility(View.VISIBLE);
                        tvErrorMessage.setText(search.isEmpty()
                                ? "Chưa có công thức món ăn nào trên máy chủ."
                                : "Không tìm thấy món ăn nào khớp với: \"" + search + "\"");
                        btnRetry.setVisibility(View.GONE);
                    } else {
                        layoutError.setVisibility(View.GONE);
                    }

                    // Cập nhật dữ liệu mới vào Adapter để hiển thị lên màn hình
                    adapter.updateData(currentRecipes);
                },
                error -> {
                    // Xử lý khi mất mạng hoặc Backend chưa bật
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

    /**
     * [SỰ KIỆN CLICK VÀO THẺ MÓN ĂN]:
     * - Khách: Bị chặn và hiện Dialog yêu cầu đăng nhập.
     * - Đã đăng nhập: Chuyển sang RecipeDetailActivity kèm recipe_id.
     */
    @Override
    public void onRecipeClick(Recipe recipe) {
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

    /**
     * [SỰ KIỆN CLICK VÀO ICON TRÁI TIM TRÊN THẺ MÓN ĂN]:
     * - Sử dụng Optimistic UI update: Đổi màu icon lập tức trên item của RecyclerView.
     * - Gọi API ngầm: Thêm hoặc xóa món yêu thích. Nếu lỗi mạng thì hoàn tác lại màu icon cũ.
     */
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

        // 1. [OPTIMISTIC UI UPDATE] Cập nhật ngay trên giao diện trước khi API trả về
        recipe.setFavorite(willBeFavorite);
        if (willBeFavorite) {
            favoriteRecipeIds.add(recipe.getId());
        } else {
            favoriteRecipeIds.remove(recipe.getId());
        }
        // Làm mới đúng vị trí của item vừa click mà không load lại toàn bộ danh sách
        adapter.notifyItemChanged(position);

        // 2. Gửi request mạng tương ứng
        if (willBeFavorite) {
            // Thêm vào yêu thích
            String addUrl = ApiEndpoints.getAddFavoriteUrl(recipe.getId(), userId);
            JsonObjectRequest addReq = new JsonObjectRequest(
                    Request.Method.POST,
                    addUrl,
                    null,
                    resp -> Toast.makeText(requireContext(), "Đã thêm vào yêu thích ♥", Toast.LENGTH_SHORT).show(),
                    err -> {
                        // Rollback nếu thất bại
                        recipe.setFavorite(false);
                        favoriteRecipeIds.remove(recipe.getId());
                        adapter.notifyItemChanged(position);
                        Toast.makeText(requireContext(), "Không thể thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    }
            );
            ApiClient.getQueue(requireContext()).add(addReq);
        } else {
            // Xóa khỏi yêu thích
            String delUrl = ApiEndpoints.getRemoveFavoriteUrl(recipe.getId(), userId);
            JsonObjectRequest delReq = new JsonObjectRequest(
                    Request.Method.DELETE,
                    delUrl,
                    null,
                    resp -> Toast.makeText(requireContext(), "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show(),
                    err -> {
                        // Rollback nếu thất bại
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
