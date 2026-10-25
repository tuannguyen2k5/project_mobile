package vn.edu.usth.appfinal.network;

import android.content.Context;
import android.net.Uri;
import vn.edu.usth.appfinal.utils.SessionManager;

public class ApiEndpoints {
    // Default host for Android Emulator accessing host PC
    private static String host = "10.0.2.2:8080";

    public static void init(Context context) {
        if (context != null) {
            String savedHost = SessionManager.getInstance(context).getServerHost();
            if (savedHost != null && !savedHost.trim().isEmpty()) {
                host = savedHost.trim();
            }
        }
    }

    public static void setHost(String customHost) {
        if (customHost != null && !customHost.trim().isEmpty()) {
            host = customHost.trim();
        }
    }

    public static String getHost() {
        return host;
    }

    public static String getBaseUrl() {
        return "http://" + host + "/api";
    }

    // ==========================================
    // AUTHENTICATION (/api/auth)
    // ==========================================
    public static String getRegisterUrl() {
        return getBaseUrl() + "/auth/register";
    }

    public static String getLoginUrl() {
        return getBaseUrl() + "/auth/login";
    }

    // ==========================================
    // RECIPES (/api/recipes)
    // ==========================================
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

    public static String getRecipeDetailUrl(long id, Long userId) {
        String url = getBaseUrl() + "/recipes/" + id;
        if (userId != null && userId > 0) {
            url += "?userId=" + userId;
        }
        return url;
    }

    public static String getCreateRecipeUrl(Long userId) {
        String url = getBaseUrl() + "/recipes";
        if (userId != null && userId > 0) {
            url += "?userId=" + userId;
        }
        return url;
    }

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
    // FAVORITES (/api/favorites)
    // ==========================================
    public static String getFavoritesUrl(long userId) {
        return getBaseUrl() + "/favorites?userId=" + userId;
    }

    public static String getAddFavoriteUrl(long recipeId, long userId) {
        return getBaseUrl() + "/favorites/" + recipeId + "?userId=" + userId;
    }

    public static String getRemoveFavoriteUrl(long recipeId, long userId) {
        return getBaseUrl() + "/favorites/" + recipeId + "?userId=" + userId;
    }

    // ==========================================
    // USERS (/api/users)
    // ==========================================
    public static String getProfileUrl(long userId) {
        return getBaseUrl() + "/users/me?userId=" + userId;
    }

    public static String getUpdateProfileUrl(long userId) {
        return getBaseUrl() + "/users/me?userId=" + userId;
    }
}
