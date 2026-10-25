package vn.edu.usth.appfinal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import vn.edu.usth.appfinal.R;

/**
 * =========================================================================================
 * [FRONTEND - UI] MÀN HÌNH CHỜ KHỞI ĐỘNG (SplashActivity - App Launcher)
 * =========================================================================================
 * - Là Activity đầu tiên được Android gọi khi khởi động ứng dụng (khai báo trong AndroidManifest.xml).
 * - Hiển thị logo ứng dụng trong 2 giây (SPLASH_DELAY_MS = 2000ms), sau đó tự động điều hướng sang MainActivity.
 * - Sử dụng Handler kết hợp Looper.getMainLooper() để lên lịch chuyển Activity trên Main Thread (UI Thread).
 * =========================================================================================
 */
public class SplashActivity extends AppCompatActivity {

    // Thời gian hiển thị màn hình chờ (2000 mili giây = 2 giây)
    private static final int SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Xử lý đệm viền tránh tai thỏ
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootSplash), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Hẹn giờ 2 giây sau sẽ chuyển sang MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Đóng hẳn SplashActivity để người dùng bấm nút Back không bị quay lại màn hình này
        }, SPLASH_DELAY_MS);
    }
}
