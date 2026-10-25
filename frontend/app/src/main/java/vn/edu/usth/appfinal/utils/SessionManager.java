package vn.edu.usth.appfinal.utils;

import android.content.Context;
import android.content.SharedPreferences;

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

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    public void saveUser(long id, String fullName, String email) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, id);
        editor.putString(KEY_USER_FULL_NAME, fullName);
        editor.putString(KEY_USER_EMAIL, email);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public long getUserId() {
        return pref.getLong(KEY_USER_ID, -1);
    }

    public String getUserFullName() {
        return pref.getString(KEY_USER_FULL_NAME, "");
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, "");
    }

    public String getServerHost() {
        return pref.getString(KEY_SERVER_HOST, DEFAULT_HOST);
    }

    public void setServerHost(String host) {
        if (host != null && !host.trim().isEmpty()) {
            editor.putString(KEY_SERVER_HOST, host.trim());
            editor.apply();
        }
    }

    public void logout() {
        String savedHost = getServerHost();
        editor.clear();
        editor.putString(KEY_SERVER_HOST, savedHost);
        editor.apply();
    }
}
