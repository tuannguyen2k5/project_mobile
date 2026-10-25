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

/**
 * =========================================================================================
 * [FRONTEND - UI] MÀN HÌNH CHÍNH CỦA ỨNG DỤNG (MainActivity)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Thanh Header: Hiển thị lời chào "Xin chào, {Tên}!" kèm icon hồ sơ, hoặc nút "Đăng nhập" nếu là khách.
 *   2. Thanh tìm kiếm (Search bar): Lọc món ăn thời gian thực khi gõ phím hoặc nhấn nút Search trên bàn phím ảo.
 *   3. Điều hướng Tab: 2 Tab "Công thức" và "Yêu thích" trượt mượt mà bằng ViewPager2 + FragmentStateAdapter.
 *   4. Nút bấm nổi (FloatingActionButton): Mở hộp thoại thêm công thức món ăn mới (yêu cầu đăng nhập).
 *   5. Xem & Cập nhật hồ sơ cá nhân: Bấm vào tên/avatar để mở Dialog xem và sửa thông tin cá nhân.
 * - Khi thầy cô hỏi:
 *   + "Tìm kiếm tự động cập nhật ở đâu?": Tìm `etSearchRecipe.addTextChangedListener(...)`.
 *   + "Nút đổi Tab 'Công thức' và 'Yêu thích' ở đâu?": Xem `updateTabs(position)` và `viewPager.registerOnPageChangeCallback(...)`.
 *   + "Sửa thông tin tài khoản ở đâu?": Xem `showUserProfileDialog()` và `showEditProfileDialog()`.
 * =========================================================================================
 */
public class MainActivity extends AppCompatActivity {

    // Khai báo các thành phần giao diện (Views)
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

        // Khởi tạo quản lý phiên và địa chỉ IP kết nối server
        sessionManager = SessionManager.getInstance(this);
        ApiEndpoints.init(this);

        // Xử lý tràn viền (Edge-to-Edge) để tránh nội dung bị che bởi tai thỏ / thanh điều hướng hệ thống
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayoutMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Ánh xạ (Binding) các View từ file layout activity_main.xml
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

        // -------------------------------------------------------------
        // [CẤU HÌNH VIEWPAGER2 & 2 TAB]: Tab 0 (Công thức) - Tab 1 (Yêu thích)
        // -------------------------------------------------------------
        pagerAdapter = new HomePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Bắt sự kiện khi click vào chữ "Công thức" hoặc "Yêu thích" để chuyển trang
        tabRecipes.setOnClickListener(v -> viewPager.setCurrentItem(0));
        tabFavorites.setOnClickListener(v -> viewPager.setCurrentItem(1));

        // Lắng nghe sự kiện trượt trang (Swipe) của ViewPager2
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Đổi màu chữ và độ đậm của Tab được chọn
                updateTabs(position);
                // Nếu chuyển sang Tab Yêu thích (position = 1), tự động làm mới danh sách yêu thích
                if (position == 1 && pagerAdapter.getFavoritesFragment() != null) {
                    pagerAdapter.getFavoritesFragment().loadFavorites();
                }
            }
        });

        // -------------------------------------------------------------
        // [THANH TÌM KIẾM]: Tự động lọc kết quả khi gõ ký tự
        // -------------------------------------------------------------
        // 1. Nhấn nút Search trên bàn phím ảo
        etSearchRecipe.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchRecipe.getText().toString());
                return true;
            }
            return false;
        });

        // 2. Lắng nghe thay đổi văn bản thời gian thực (Real-time TextWatcher)
        etSearchRecipe.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Hiện nút xóa (X) nếu ô tìm kiếm có chữ, ẩn đi nếu rỗng
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                // Nếu đang ở tab Yêu thích mà người dùng gõ tìm kiếm, tự động nhảy về tab Công thức
                if (s.length() > 0 && viewPager.getCurrentItem() != 0) {
                    viewPager.setCurrentItem(0);
                }
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Nút (X) xóa nhanh toàn bộ chữ trong ô tìm kiếm
        btnClearSearch.setOnClickListener(v -> {
            etSearchRecipe.setText("");
            performSearch("");
        });

        // -------------------------------------------------------------
        // [NÚT ĐĂNG NHẬP / ĐĂNG XUẤT TRÊN HEADER]
        // -------------------------------------------------------------
        btnHeaderLogin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        btnHeaderLogout.setOnClickListener(v -> showLogoutDialog());

        // -------------------------------------------------------------
        // [NÚT NỔI (+) THÊM CÔNG THỨC MÓN ĂN MỚI]
        // -------------------------------------------------------------
        btnFabAddRecipe.setOnClickListener(v -> {
            if (!sessionManager.isLoggedIn()) {
                showLoginRequiredDialog("Thêm món ăn", "Bạn cần đăng nhập tài khoản để thêm món ăn mới vào hệ thống.");
            } else {
                openAddRecipeFragment();
            }
        });

        // Cập nhật trạng thái hiển thị của Header và Tab khi vừa khởi động
        updateHeaderStatus();
        updateTabs(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật lại giao diện Header khi quay lại từ LoginActivity hoặc RegisterActivity
        updateHeaderStatus();
    }

    /**
     * [CẬP NHẬT HEADER]:
     * - Nếu đã đăng nhập: Hiện "Xin chào, {Họ tên}!", hiện icon hồ sơ, hiện icon đăng xuất, ẩn nút "Đăng nhập".
     * - Nếu là khách: Ẩn lời chào, ẩn icon hồ sơ, ẩn nút đăng xuất, hiện nút "Đăng nhập".
     */
    private void updateHeaderStatus() {
        if (sessionManager.isLoggedIn()) {
            String fullName = sessionManager.getUserFullName();
            tvUserGreeting.setVisibility(View.VISIBLE);
            tvUserGreeting.setText("Xin chào, " + (fullName.isEmpty() ? "Thành viên" : fullName) + "!");
            if (ivProfileIcon != null) ivProfileIcon.setVisibility(View.VISIBLE);
            if (layoutUserGreeting != null) {
                layoutUserGreeting.setClickable(true);
                // Bấm vào lời chào hoặc avatar để xem chi tiết hồ sơ tài khoản
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

    /**
     * [HỘP THOẠI XEM THÔNG TIN CÁ NHÂN]:
     * - Gửi request GET /api/users/me?userId={id} lên server.
     * - Hiển thị Họ tên và Email hiện tại.
     * - Cho phép bấm "Cập nhật thông tin" để mở form sửa thông tin.
     */
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

    /**
     * [HỘP THOẠI CẬP NHẬT THÔNG TIN CÁ NHÂN]:
     * - Cho phép sửa Họ tên và Email.
     * - Gửi request PUT /api/users/me?userId={id}.
     * - Cập nhật lại SharedPreferences và làm mới Header nếu thành công.
     */
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

                    // Gửi request PUT cập nhật thông tin lên server
                    String updateUrl = ApiEndpoints.getUpdateProfileUrl(userId);
                    JsonObjectRequest updateReq = new JsonObjectRequest(
                            Request.Method.PUT,
                            updateUrl,
                            reqBody,
                            resp -> {
                                String updatedName = resp.optString("fullName", newName);
                                String updatedEmail = resp.optString("email", newEmail);
                                // Lưu thông tin mới vào session
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

    /**
     * Truyền từ khóa tìm kiếm sang RecipesFragment để lọc dữ liệu
     */
    private void performSearch(String query) {
        if (pagerAdapter.getRecipesFragment() != null) {
            pagerAdapter.getRecipesFragment().searchRecipes(query);
        }
    }

    /**
     * [ĐỔI GIAO DIỆN TAB]:
     * - Tab được chọn: Màu xanh đậm chủ đạo (0xFF285D38) và chữ IN ĐẬM (Typeface.BOLD).
     * - Tab không chọn: Màu xám (0xFF888888) và chữ THƯỜNG (Typeface.NORMAL).
     */
    private void updateTabs(int position) {
        tabRecipes.setTextColor(position == 0 ? 0xFF285D38 : 0xFF888888);
        tabFavorites.setTextColor(position == 1 ? 0xFF285D38 : 0xFF888888);

        tabRecipes.setTypeface(null, position == 0 ? Typeface.BOLD : Typeface.NORMAL);
        tabFavorites.setTypeface(null, position == 1 ? Typeface.BOLD : Typeface.NORMAL);
    }

    /**
     * Hiển thị thông báo khi khách nhấn vào tính năng yêu cầu tài khoản
     */
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

    /**
     * [HỘP THOẠI XÁC NHẬN ĐĂNG XUẤT]:
     * - Khi xác nhận: Gọi sessionManager.logout(), xóa thông tin phiên, làm mới danh sách và header.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    sessionManager.logout();
                    Toast.makeText(this, "Đã đăng xuất!", Toast.LENGTH_SHORT).show();
                    updateHeaderStatus();
                    // Làm mới lại tab công thức về chế độ khách (chỉ 10 món)
                    if (pagerAdapter.getRecipesFragment() != null) {
                        pagerAdapter.getRecipesFragment().searchRecipes("");
                    }
                    // Làm mới tab yêu thích (sẽ thông báo yêu cầu đăng nhập)
                    if (pagerAdapter.getFavoritesFragment() != null) {
                        pagerAdapter.getFavoritesFragment().loadFavorites();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    /**
     * Mở Dialog Fragment thêm món ăn mới
     */
    private void openAddRecipeFragment() {
        AddRecipeFragment addRecipeFragment = AddRecipeFragment.newInstance();
        addRecipeFragment.setOnRecipeAddedListener(newRecipe -> {
            // Sau khi thêm món thành công, làm mới lại danh sách món ăn trên trang chủ
            if (pagerAdapter.getRecipesFragment() != null) {
                pagerAdapter.getRecipesFragment().searchRecipes("");
            }
        });
        addRecipeFragment.show(getSupportFragmentManager(), "AddRecipeFragment");
    }
}
