package vn.edu.usth.appfinal.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * =========================================================================================
 * [FRONTEND - UTILS] QUẢN LÝ PHIÊN ĐĂNG NHẬP (SessionManager - SharedPreferences)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Lưu trữ trạng thái đăng nhập của người dùng dưới dạng Key-Value trong bộ nhớ Android (SharedPreferences).
 *   2. Lưu các thông tin thiết yếu: userId, fullName, email và địa chỉ serverHost.
 *   3. Hỗ trợ kiểm tra nhanh `isLoggedIn()` để điều hướng UI (khách vs đã đăng nhập).
 *   4. Xử lý đăng xuất: Xóa sạch thông tin tài khoản nhưng giữ lại cấu hình serverHost.
 * - Câu hỏi bảo vệ:
 *   + "Lưu thông tin đăng nhập của người dùng ở đâu?": Dùng SharedPreferences (MODE_PRIVATE).
 *   + "Tại sao không xóa sạch cả serverHost khi logout?": Để lần sau mở app không phải nhập lại IP server.
 * =========================================================================================
 */
public class SessionManager {
    private static final String PREF_NAME = "RecipeNutritionPref";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_FULL_NAME = "userFullName";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_SERVER_HOST = "serverHost";
    private static final String DEFAULT_HOST = "10.0.2.2:8080";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private static SessionManager instance;

    public SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Singleton đảm bảo chỉ có 1 đối tượng quản lý Session trong toàn ứng dụng
     */
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    /**
     * Lưu thông tin tài khoản sau khi đăng nhập/đăng ký thành công từ API
     */
    public void saveUser(long id, String fullName, String email) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, id);
        editor.putString(KEY_USER_FULL_NAME, fullName);
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply(); // Ghi bất đồng bộ xuống file XML nội bộ
    }

    /**
     * Kiểm tra người dùng đã đăng nhập chưa
     * @return true nếu đã đăng nhập, false nếu đang ở chế độ khách (Guest)
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Lấy ID của người dùng hiện tại (trả về -1 nếu là khách)
     */
    public long getUserId() {
        return pref.getLong(KEY_USER_ID, -1);
    }

    /**
     * Lấy họ tên người dùng để hiển thị trên lời chào Header
     */
    public String getUserFullName() {
        return pref.getString(KEY_USER_FULL_NAME, "");
    }

    /**
     * Lấy email tài khoản
     */
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    /**
     * Lấy địa chỉ host máy chủ đang kết nối (mặc định 10.0.2.2:8080)
     */
    public String getServerHost() {
        return pref.getString(KEY_SERVER_HOST, DEFAULT_HOST);
    }

    /**
     * Lưu địa chỉ IP máy chủ mới
     */
    public void setServerHost(String host) {
        if (host != null && !host.trim().isEmpty()) {
            editor.putString(KEY_SERVER_HOST, host.trim());
            editor.apply();
        }
    }

    /**
     * Đăng xuất tài khoản:
     * - Giữ lại địa chỉ IP server đã cấu hình.
     * - Xóa toàn bộ id, tên, email và cờ isLoggedIn.
     */
    public void logout() {
        String savedHost = getServerHost();
        editor.clear();
        editor.putString(KEY_SERVER_HOST, savedHost);
        editor.apply();
    }
}
