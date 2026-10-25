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

        rvFavorites = view.findViewById(R.id.rvFavorites);
        progressBar = view.findViewById(R.id.progressBar);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySub = view.findViewById(R.id.tvEmptySub);
        btnEmptyLogin = view.findViewById(R.id.btnEmptyLogin);
        layoutError = view.findViewById(R.id.layoutError);
        tvErrorMessage = view.findViewById(R.id.tvErrorMessage);
        btnRetry = view.findViewById(R.id.btnRetry);

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
        loadFavorites();
    }

    public void loadFavorites() {
        if (!isAdded()) return;

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
                            r.setFavorite(true);
                            favoriteList.add(r);
                        }
                    }

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

    @Override
    public void onRecipeClick(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra("recipe_id", recipe.getId());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Recipe recipe, int position) {
        long userId = sessionManager.getUserId();
        if (userId <= 0) return;

        // DELETE /api/favorites/{recipeId}?userId={userId}
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
