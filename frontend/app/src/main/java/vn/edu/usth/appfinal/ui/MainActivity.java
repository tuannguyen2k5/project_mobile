package vn.edu.usth.appfinal.ui;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import vn.edu.usth.appfinal.R;
import vn.edu.usth.appfinal.adapter.HomePagerAdapter;
import vn.edu.usth.appfinal.network.ApiClient;
import vn.edu.usth.appfinal.network.ApiEndpoints;
import vn.edu.usth.appfinal.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tabRecipes;
    private TextView tabFavorites;
    private View layoutUserGreeting;
    private TextView tvUserGreeting;
    private ImageView ivProfileIcon;
    private AppCompatButton btnHeaderLogin;
    private ImageView btnHeaderLogout;
    private EditText etSearchRecipe;
    private ImageView btnClearSearch;
    private FloatingActionButton btnFabAddRecipe;
    private HomePagerAdapter pagerAdapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = SessionManager.getInstance(this);
        ApiEndpoints.init(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayoutMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        viewPager = findViewById(R.id.viewPager);
        tabRecipes = findViewById(R.id.tabRecipes);
        tabFavorites = findViewById(R.id.tabFavorites);
        layoutUserGreeting = findViewById(R.id.layoutUserGreeting);
        tvUserGreeting = findViewById(R.id.tvUserGreeting);
        ivProfileIcon = findViewById(R.id.ivProfileIcon);
        btnHeaderLogin = findViewById(R.id.btnHeaderLogin);
        btnHeaderLogout = findViewById(R.id.btnHeaderLogout);
        etSearchRecipe = findViewById(R.id.etSearchRecipe);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnFabAddRecipe = findViewById(R.id.btnFabAddRecipe);

        // ViewPager setup
        pagerAdapter = new HomePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        tabRecipes.setOnClickListener(v -> viewPager.setCurrentItem(0));
        tabFavorites.setOnClickListener(v -> viewPager.setCurrentItem(1));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabs(position);
                if (position == 1 && pagerAdapter.getFavoritesFragment() != null) {
                    pagerAdapter.getFavoritesFragment().loadFavorites();
                }
            }
        });

        // Search bar listeners
        etSearchRecipe.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchRecipe.getText().toString());
                return true;
            }
            return false;
        });

        etSearchRecipe.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                if (s.length() > 0 && viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(0);
                }
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> {
            etSearchRecipe.setText("");
            performSearch("");
        });

        // Login / Logout buttons
        btnHeaderLogin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        btnHeaderLogout.setOnClickListener(v -> showLogoutDialog());

        // Floating button to Add Recipe (POST /api/recipes)
        btnFabAddRecipe.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                showLoginRequiredDialog("Thêm món ăn", "Bạn cần đăng nhập tài khoản để thêm món ăn mới vào hệ thống.");
            } else {
                openAddRecipeFragment();
            }
        });

        updateHeaderStatus();
        updateTabs(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateHeaderStatus();
    }

    private void updateHeaderStatus() {
        if (sessionManager.isLoggedIn()) {
            String fullName = sessionManager.getUserFullName();
            tvUserGreeting.setVisibility(View.VISIBLE);
            tvUserGreeting.setText("Xin chào, " + (fullName.isEmpty() ? "Thành viên" : fullName) + "!");
            if (ivProfileIcon != null) ivProfileIcon.setVisibility(View.VISIBLE);
            if (layoutUserGreeting != null) {
                layoutUserGreeting.setClickable(true);
                layoutUserGreeting.setOnClickListener(v -> showUserProfileDialog());
            }
            btnHeaderLogin.setVisibility(View.GONE);
            btnHeaderLogout.setVisibility(View.VISIBLE);
        } else {
            tvUserGreeting.setVisibility(View.GONE);
            tvUserGreeting.setText("");
            if (ivProfileIcon != null) ivProfileIcon.setVisibility(View.GONE);
            if (layoutUserGreeting != null) {
                layoutUserGreeting.setOnClickListener(null);
                layoutUserGreeting.setClickable(false);
            }
            btnHeaderLogin.setVisibility(View.VISIBLE);
            btnHeaderLogout.setVisibility(View.GONE);
        }
    }

    private void showUserProfileDialog() {
        if (!sessionManager.isLoggedIn()) {
            return;
        }
        long userId = sessionManager.getUserId();
        String url = ApiEndpoints.getProfileUrl(userId);

        JsonObjectRequest profileReq = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    String name = response.optString("fullName", sessionManager.getUserFullName());
                    String email = response.optString("email", sessionManager.getUserEmail());
                    String createdAt = response.optString("createdAt", "");

                    String info = "👤 Họ và tên: " + name + "\n\n"
                                + "📧 Email: " + email;

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Thông tin tài khoản")
                            .setMessage(info)
                            .setPositiveButton("Đóng", null)
                            .setNeutralButton("Cập nhật thông tin", (dialog, which) -> showEditProfileDialog(name, email))
                            .show();
                },
                error -> Toast.makeText(MainActivity.this, "Không thể tải thông tin từ máy chủ", Toast.LENGTH_SHORT).show()
        );

        ApiClient.getQueue(this).add(profileReq);
    }

    private void showEditProfileDialog(String currentName, String currentEmail) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 16);

        TextView tvNameLabel = new TextView(this);
        tvNameLabel.setText("Họ và tên:");
        tvNameLabel.setTextSize(13);
        layout.addView(tvNameLabel);

        EditText etName = new EditText(this);
        etName.setText(currentName);
        etName.setHint("Nhập họ và tên");
        layout.addView(etName);

        TextView tvEmailLabel = new TextView(this);
        tvEmailLabel.setText("Địa chỉ Email:");
        tvEmailLabel.setTextSize(13);
        tvEmailLabel.setPadding(0, 16, 0, 0);
        layout.addView(tvEmailLabel);

        EditText etEmail = new EditText(this);
        etEmail.setText(currentEmail);
        etEmail.setHint("Nhập email");
        layout.addView(etEmail);

        new AlertDialog.Builder(this)
                .setTitle("Cập nhật thông tin")
                .setView(layout)
                .setPositiveButton("Lưu thay đổi", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    String newEmail = etEmail.getText().toString().trim();

                    if (newName.isEmpty()) {
                        Toast.makeText(this, "Họ và tên không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newEmail.isEmpty()) {
                        Toast.makeText(this, "Email không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    long userId = sessionManager.getUserId();
                    JSONObject reqBody = new JSONObject();
                    try {
                        reqBody.put("fullName", newName);
                        reqBody.put("email", newEmail);
                    } catch (JSONException ignored) {}

                    // PUT /api/users/me?userId={userId}
                    String updateUrl = ApiEndpoints.getUpdateProfileUrl(userId);
                    JsonObjectRequest updateReq = new JsonObjectRequest(
                            Request.Method.PUT,
                            updateUrl,
                            reqBody,
                            resp -> {
                                String updatedName = resp.optString("fullName", newName);
                                String updatedEmail = resp.optString("email", newEmail);
                                sessionManager.saveUser(userId, updatedName, updatedEmail);
                                updateHeaderStatus();
                                Toast.makeText(this, "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                            },
                            err -> {
                                String errMsg = "Không thể cập nhật thông tin";
                                if (err.networkResponse != null && err.networkResponse.data != null) {
                                    try {
                                        String body = new String(err.networkResponse.data, java.nio.charset.StandardCharsets.UTF_8);
                                        JSONObject errObj = new JSONObject(body);
                                        if (errObj.has("error")) {
                                            errMsg = errObj.getString("error");
                                        }
                                    } catch (Exception ignored) {}
                                }
                                Toast.makeText(this, errMsg, Toast.LENGTH_LONG).show();
                            }
                    );

                    ApiClient.getQueue(this).add(updateReq);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performSearch(String query) {
        if (pagerAdapter.getRecipesFragment() != null) {
            pagerAdapter.getRecipesFragment().searchRecipes(query);
        }
    }

    private void updateTabs(int position) {
        tabRecipes.setTextColor(position == 0 ? 0xFF285D38 : 0xFF888888);
        tabFavorites.setTextColor(position == 1 ? 0xFF285D38 : 0xFF888888);

        tabRecipes.setTypeface(null, position == 0 ? Typeface.BOLD : Typeface.NORMAL);
        tabFavorites.setTypeface(null, position == 1 ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void showLoginRequiredDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đăng nhập ngay", (dialog, which) -> {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                })
                .setNegativeButton("Để sau", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    sessionManager.logout();
                    Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
                    updateHeaderStatus();
                    if (pagerAdapter.getRecipesFragment() != null) {
                        pagerAdapter.getRecipesFragment().searchRecipes("");
                    }
                    if (pagerAdapter.getFavoritesFragment() != null) {
                        pagerAdapter.getFavoritesFragment().loadFavorites();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openAddRecipeFragment() {
        AddRecipeFragment addRecipeFragment = AddRecipeFragment.newInstance();
        addRecipeFragment.setOnRecipeAddedListener(newRecipe -> {
            if (pagerAdapter.getRecipesFragment() != null) {
                pagerAdapter.getRecipesFragment().searchRecipes("");
            }
        });
        addRecipeFragment.show(getSupportFragmentManager(), "AddRecipeFragment");
    }
}
