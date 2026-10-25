package vn.edu.usth.appfinal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;
import vn.edu.usth.appfinal.R;
import vn.edu.usth.appfinal.network.ApiClient;
import vn.edu.usth.appfinal.network.ApiEndpoints;
import vn.edu.usth.appfinal.utils.SessionManager;

import java.nio.charset.StandardCharsets;

/**
 * =========================================================================================
 * [FRONTEND - UI] MÀN HÌNH ĐĂNG KÝ TÀI KHOẢN (RegisterActivity)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Kiểm tra tính hợp lệ dữ liệu đăng ký:
 *      + Họ và tên không rỗng.
 *      + Email đúng định dạng regex (Patterns.EMAIL_ADDRESS).
 *      + Mật khẩu tối thiểu 6 ký tự.
 *      + Mật khẩu xác nhận phải trùng khớp.
 *   2. Gửi request POST /api/auth/register lên Spring Boot.
 *   3. Tự động đăng nhập (Auto-login) ngay sau khi đăng ký thành công và chuyển về MainActivity.
 * - Câu hỏi bảo vệ:
 *   + "Kiểm tra định dạng email ở đâu?": `Patterns.EMAIL_ADDRESS.matcher(email).matches()`.
 *   + "Nếu email đã được đăng ký thì app hiển thị gì?": Backend trả về 400 Bad Request kèm message,
 *     khối error callback bóc tách chuỗi JSON hiển thị lên Toast.
 * =========================================================================================
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private AppCompatButton btnRegister;
    private ProgressBar progressBar;
    private TextView tvGoToLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sessionManager = SessionManager.getInstance(this);
        ApiEndpoints.init(this);

        // Ánh xạ View
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Bắt sự kiện bấm nút Đăng ký
        btnRegister.setOnClickListener(v -> attemptRegister());

        // Quay lại màn hình Đăng nhập
        tvGoToLogin.setOnClickListener(v -> finish());
    }

    /**
     * [VALIDATE & GỬI YÊU CẦU ĐĂNG KÝ]:
     */
    private void attemptRegister() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // 1. Kiểm tra họ và tên
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Họ và tên là bắt buộc");
            etFullName.requestFocus();
            return;
        }

        // 2. Kiểm tra email
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email là bắt buộc");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không đúng định dạng");
            etEmail.requestFocus();
            return;
        }

        // 3. Kiểm tra độ dài mật khẩu
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        // 4. Kiểm tra xác nhận mật khẩu
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        setLoading(true);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("fullName", fullName);
            requestBody.put("email", email);
            requestBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = ApiEndpoints.getRegisterUrl();

        // Gửi POST /api/auth/register
        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    setLoading(false);
                    long userId = response.optLong("id", 0);
                    String userFullName = response.optString("fullName", fullName);
                    String userEmail = response.optString("email", email);

                    // [TỰ ĐỘNG ĐĂNG NHẬP] Lưu phiên người dùng ngay sau khi tạo tài khoản
                    sessionManager.saveUser(userId, userFullName, userEmail);

                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công! Chào mừng bạn!", Toast.LENGTH_SHORT).show();

                    // Chuyển sang MainActivity và dọn dẹp các Activity cũ trong Stack
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                },
                error -> {
                    setLoading(false);
                    String errorMsg;
                    // Bóc tách phản hồi báo lỗi từ Backend (như trùng email)
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg = "Đăng ký thất bại.";
                        try {
                            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            JSONObject obj = new JSONObject(responseBody);
                            if (obj.has("error")) {
                                errorMsg = obj.getString("error");
                            }
                        } catch (Exception ignored) {}
                    } else {
                        errorMsg = "Không thể kết nối đến máy chủ Backend (" + ApiEndpoints.getBaseUrl() + "). Vui lòng đảm bảo Spring Boot đang chạy!";
                    }
                    Toast.makeText(RegisterActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
        );

        ApiClient.getQueue(this).add(request);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
    }
}
