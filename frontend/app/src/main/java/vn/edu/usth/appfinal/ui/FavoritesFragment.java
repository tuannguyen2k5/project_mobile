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
import java.util.List;

/**
 * =========================================================================================
 * [FRONTEND - UI] FRAGMENT MÓN ĂN YÊU THÍCH (Tab "Yêu thích")
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Kiểm tra trạng thái đăng nhập:
 *      + Nếu là khách: Hiển thị giao diện nhắc nhở đăng nhập kèm nút "Đăng nhập ngay".
 *      + Nếu đã đăng nhập: Gọi API GET /api/favorites?userId={userId} để lấy danh sách món đã lưu.
 *   2. Hiển thị danh sách món đã thả tim: Tái sử dụng RecipeAdapter.
 *   3. Xử lý bỏ yêu thích: Khi bấm vào icon trái tim trên tab này, gửi API DELETE để xóa khỏi danh sách,
 *      đồng thời loại bỏ item ra khỏi RecyclerView ngay lập tức.
 * - Câu hỏi bảo vệ:
 *   + "Giao diện khi chưa đăng nhập hoặc khi chưa có món yêu thích được cấu hình ở đâu?":
 *     Xem layoutEmpty, tvEmptyTitle, tvEmptySub, btnEmptyLogin trong hàm loadFavorites().
 * =========================================================================================
 */
public class FavoritesFragment extends Fragment implements RecipeAdapter.OnRecipeClickListener, RecipeAdapter.OnFavoriteClickListener {

    private RecyclerView rvFavorites;
    private RecipeAdapter adapter;
    private ProgressBar progressBar;
    private View layoutEmpty;
    private TextView tvEmptyTitle, tvEmptySub;
    private AppCompatButton btnEmptyLogin;
    private View layoutError;
    private TextView tvErrorMessage;
    private AppCompatButton btnRetry;

    private SessionManager sessionManager;
    private final List<Recipe> favoriteList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        sessionManager = SessionManager.getInstance(requireContext());

        // Ánh xạ các view
        rvFavorites = view.findViewById(R.id.rvFavorites);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySub = view.findViewById(R.id.tvEmptySub);
        btnEmptyLogin = view.findViewById(R.id.btnEmptyLogin);
        layoutError = view.findViewById(R.id.layoutError);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);

        // Khởi tạo danh sách RecyclerView
        rvFavorites.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new RecipeAdapter(new ArrayList<>());
        adapter.setOnRecipeClickListener(this);
        adapter.setOnFavoriteClickListener(this);
        rvFavorites.setAdapter(adapter);

        btnRetry.setOnClickListener(v -> loadFavorites());
        btnEmptyLogin.setOnClickListener(v -> startActivity(new Intent(requireContext(), LoginActivity.class)));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại danh sách yêu thích khi người dùng chuyển lại tab này
        loadFavorites();
    }

    /**
     * [TẢI DANH SÁCH MÓN YÊU THÍCH TỪ SERVER]:
     * - Nếu chưa đăng nhập: Hiện khung rỗng với tiêu đề "Yêu cầu đăng nhập" và nút Login.
     * - Nếu đã đăng nhập: Gọi GET /api/favorites?userId={id}.
     */
    public void loadFavorites() {
        if (!isAdded()) return;

        // [KIỂM TRA ĐĂNG NHẬP]
        if (!sessionManager.isLoggedIn()) {
            progressBar.setVisibility(View.GONE);
            layoutError.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptyTitle.setText("Yêu cầu đăng nhập");
            tvEmptySub.setText("Vui lòng đăng nhập tài khoản để xem và quản lý danh sách món ăn yêu thích của bạn.");
            btnEmptyLogin.setVisibility(View.VISIBLE);
            adapter.updateData(new ArrayList<>());
            return;
        }

        // Đã đăng nhập -> Chuẩn bị tải dữ liệu
        btnEmptyLogin.setVisibility(View.GONE);
        tvEmptyTitle.setText(R.string.no_favorites_yet);
        tvEmptySub.setText(R.string.favorites_sub);

        long userId = sessionManager.getUserId();

        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        String url = ApiEndpoints.getFavoritesUrl(userId);

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);

                    favoriteList.clear();
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        if (obj != null) {
                            Recipe r = Recipe.fromJson(obj);
                            r.setFavorite(true); // Toàn bộ món trong tab này đều đã được thả tim
                            favoriteList.add(r);
                        }
                    }

                    // Nếu người dùng chưa lưu món nào -> Hiện layout báo trống
                    if (favoriteList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                        layoutError.setVisibility(View.GONE);
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                    }

                    adapter.updateData(favoriteList);
                },
                error -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    layoutError.setVisibility(View.VISIBLE);
                    tvErrorMessage.setText(getString(R.string.error_network));
                }
        );

        ApiClient.getQueue(requireContext()).add(request);
    }

    /**
     * Bấm vào món yêu thích -> Mở RecipeDetailActivity để xem chi tiết
     */
    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        startActivity(intent);
    }

    /**
     * [BỎ YÊU THÍCH TRỰC TIẾP TRÊN TAB YÊU THÍCH]:
     * - Gửi DELETE /api/favorites/{recipeId}?userId={userId}
     * - Xóa ngay món đó khỏi danh sách favoriteList và cập nhật RecyclerView
     */
    @Override
    public void onFavoriteClick(Recipe recipe, int position) {
        long userId = sessionManager.getUserId();
        if (userId <= 0) return;

        String delUrl = ApiEndpoints.getRemoveFavoriteUrl(recipe.getId(), userId);
        JsonObjectRequest delReq = new JsonObjectRequest(
                Request.Method.DELETE,
                delUrl,
                null,
                resp -> {
                    Toast.makeText(requireContext(), "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    favoriteList.remove(position);
                    adapter.updateData(favoriteList);
                    if (favoriteList.isEmpty()) {
                        layoutEmpty.setVisibility(View.VISIBLE);
                    }
                },
                err -> Toast.makeText(requireContext(), "Không thể xóa khỏi yêu thích", Toast.LENGTH_SHORT).show()
        );
        ApiClient.getQueue(requireContext()).add(delReq);
    }
}
