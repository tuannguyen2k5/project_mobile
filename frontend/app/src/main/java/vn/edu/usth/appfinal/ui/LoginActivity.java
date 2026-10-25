package vn.edu.usth.appfinal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
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

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private AppCompatButton btnLogin;
    private ProgressBar progressBar;
    private TextView tvGoToRegister, tvServerConfig;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = SessionManager.getInstance(this);
        ApiEndpoints.init(this);

        // If already logged in, navigate straight to MainActivity
        if (sessionManager.isLoggedIn()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvServerConfig = findViewById(R.id.tvServerConfig);

        updateServerConfigText();

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvServerConfig.setOnClickListener(v -> showServerConfigDialog());
    }

    private void updateServerConfigText() {
        if (tvServerConfig != null) {
            tvServerConfig.setText("⚙ Địa chỉ Máy chủ: " + ApiEndpoints.getHost());
        }
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("email", email);
            requestBody.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String url = ApiEndpoints.getLoginUrl();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                response -> {
                    setLoading(false);
                    long userId = response.optLong("id", 0);
                    String fullName = response.optString("fullName", "User");
                    String userEmail = response.optString("email", email);

                    sessionManager.saveUser(userId, fullName, userEmail);

                    Toast.makeText(LoginActivity.this, "Chào mừng " + fullName + "!", Toast.LENGTH_SHORT).show();

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                },
                error -> {
                    setLoading(false);
                    String errorMsg;
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        errorMsg = "Sai email hoặc mật khẩu.";
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
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
        );

        ApiClient.getQueue(this).add(request);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }

    private void showServerConfigDialog() {
        EditText input = new EditText(this);
        input.setHint("e.g. 10.0.2.2:8080 or 192.168.1.x:8080");
        input.setText(sessionManager.getServerHost());
        new AlertDialog.Builder(this)
                .setTitle("Cấu hình IP Backend")
                .setMessage("Nhập host và port của backend Spring Boot:")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String customHost = input.getText().toString().trim();
                    if (!customHost.isEmpty()) {
                        sessionManager.setServerHost(customHost);
                        ApiEndpoints.setHost(customHost);
                        updateServerConfigText();
                        Toast.makeText(this, "Địa chỉ kết nối: " + ApiEndpoints.getBaseUrl(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
