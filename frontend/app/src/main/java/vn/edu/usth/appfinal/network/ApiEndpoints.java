package vn.edu.usth.appfinal.network;

import android.content.Context;
import android.net.Uri;
import vn.edu.usth.appfinal.utils.SessionManager;

/**
 * =========================================================================================
 * [FRONTEND - NETWORK] QUẢN LÝ TẬP TRUNG TẤT CẢ ENDPOINT REST API (ApiEndpoints)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Chứa địa chỉ IP máy chủ backend:
 *      + Trên máy ảo Android Emulator: mặc định là "10.0.2.2:8080" (alias trỏ về localhost máy tính).
 *      + Trên điện thoại thật: IP mạng LAN máy tính (ví dụ "192.168.1.5:8080").
 *   2. Cung cấp các hàm tĩnh sinh URL cho từng chức năng: Đăng ký, đăng nhập, lấy danh sách món,
 *      xem chi tiết, thêm/sửa món, danh sách yêu thích, thông tin cá nhân.
 * - Khi thầy cô hỏi:
 *   + "App kết nối vào server ở địa chỉ nào? Muốn đổi IP server thì sửa ở đâu?":
 *     -> Trả lời: Sửa biến `host` trong ApiEndpoints.java hoặc người dùng có thể đổi động qua hộp thoại
 *        cấu hình máy chủ được lưu trong SharedPreferences (SessionManager).
 * =========================================================================================
 */
public class ApiEndpoints {
    // Địa chỉ IP mặc định cho Android Emulator kết nối vào Spring Boot trên máy host (localhost:8080)
    private static String host = "10.0.2.2:8080";

    /**
     * Khởi tạo địa chỉ host từ cấu hình đã lưu trong SessionManager (SharedPreferences).
     * Gọi tại onCreate của SplashActivity và MainActivity.
     */
    public static void init(Context context) {
        if (context != null) {
            String savedHost = SessionManager.getInstance(context).getServerHost();
            if (savedHost != null && !savedHost.trim().isEmpty()) {
                host = savedHost.trim();
            }
        }
    }

    /**
     * Cập nhật địa chỉ host tùy chỉnh (dùng khi kiểm thử trên điện thoại thật kết nối qua wifi).
     */
    public static void setHost(String customHost) {
        if (customHost != null && !customHost.trim().isEmpty()) {
            host = customHost.trim();
        }
    }

    public static String getHost() {
        return host;
    }

    /**
     * URL cơ sở của toàn bộ REST API: http://{host}/api
     */
    public static String getBaseUrl() {
        return "http://" + host + "/api";
    }

    // ==========================================
    // 1. NHÓM XÁC THỰC TÀI KHOẢN (/api/auth)
    // ==========================================
    
    // URL đăng ký: POST http://{host}/api/auth/register
    public static String getRegisterUrl() {
        return getBaseUrl() + "/auth/register";
    }

    // URL đăng nhập: POST http://{host}/api/auth/login
    public static String getLoginUrl() {
        return getBaseUrl() + "/auth/login";
    }

    // ==========================================
    // 2. NHÓM CÔNG THỨC MÓN ĂN (/api/recipes)
    // ==========================================

    /**
     * Sinh URL lấy danh sách món ăn:
     * - Hỗ trợ query ?search={từ khóa}
     * - Hỗ trợ query ?userId={id} để backend phân quyền (khách chỉ xem 10 món, đăng nhập xem toàn bộ).
     */
    public static String getRecipesUrl(String search, Long userId) {
        StringBuilder url = new StringBuilder(getBaseUrl() + "/recipes");
        boolean hasParam = false;
        if (search != null && !search.trim().isEmpty()) {
            url.append("?search=").append(Uri.encode(search.trim()));
            hasParam = true;
        }
        if (userId != null && userId > 0) {
            url.append(hasParam ? "&" : "?").append("userId=").append(userId);
        }
        return url.toString();
    }

    public static String getRecipesUrl(String search) {
        return getRecipesUrl(search, null);
    }

    /**
     * URL lấy chi tiết món ăn theo ID: GET http://{host}/api/recipes/{id}?userId={userId}
     */
    public static String getRecipeDetailUrl(long id, Long userId) {
        String url = getBaseUrl() + "/recipes/" + id;
        if (userId != null && userId > 0) {
            url += "?userId=" + userId;
        }
        return url;
    }

    /**
     * URL tạo món ăn mới: POST http://{host}/api/recipes?userId={userId}
     */
    public static String getCreateRecipeUrl(Long userId) {
        String url = getBaseUrl() + "/recipes";
        if (userId != null && userId > 0) {
            url += "?userId=" + userId;
        }
        return url;
    }

    /**
     * URL cập nhật món ăn: PUT http://{host}/api/recipes/{id}?userId={userId}
     */
    public static String getUpdateRecipeUrl(long id, Long userId) {
        String url = getBaseUrl() + "/recipes/" + id;
        if (userId != null && userId > 0) {
            url += "?userId=" + userId;
        }
        return url;
    }

    public static String getUpdateRecipeUrl(long id) {
        return getUpdateRecipeUrl(id, null);
    }

    // ==========================================
    // 3. NHÓM MÓN ĂN YÊU THÍCH (/api/favorites)
    // ==========================================

    // Lấy danh sách món yêu thích: GET http://{host}/api/favorites?userId={userId}
    public static String getFavoritesUrl(long userId) {
        return getBaseUrl() + "/favorites?userId=" + userId;
    }

    // Thêm món vào yêu thích: POST http://{host}/api/favorites/{recipeId}?userId={userId}
    public static String getAddFavoriteUrl(long recipeId, long userId) {
        return getBaseUrl() + "/favorites/" + recipeId + "?userId=" + userId;
    }

    // Xóa món khỏi yêu thích: DELETE http://{host}/api/favorites/{recipeId}?userId={userId}
    public static String getRemoveFavoriteUrl(long recipeId, long userId) {
        return getBaseUrl() + "/favorites/" + recipeId + "?userId=" + userId;
    }

    // ==========================================
    // 4. NHÓM THÔNG TIN CÁ NHÂN (/api/users)
    // ==========================================

    // Xem thông tin cá nhân: GET http://{host}/api/users/me?userId={userId}
    public static String getProfileUrl(long userId) {
        return getBaseUrl() + "/users/me?userId=" + userId;
    }

    // Cập nhật thông tin cá nhân: PUT http://{host}/api/users/me?userId={userId}
    public static String getUpdateProfileUrl(long userId) {
        return getBaseUrl() + "/users/me?userId=" + userId;
    }
}
