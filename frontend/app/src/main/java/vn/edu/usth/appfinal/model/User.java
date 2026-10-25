package vn.edu.usth.appfinal.model;

import org.json.JSONObject;
import java.io.Serializable;

public class User implements Serializable {
    private long id;
    private String fullName;
    private String email;
    private String createdAt;

    public User() {}

    public User(long id, String fullName, String email) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
    }

    public static User fromJson(JSONObject json) {
        if (json == null) return null;
        User user = new User();
        user.setId(json.optLong("id", 0));
        user.setFullName(json.optString("fullName", ""));
        user.setEmail(json.optString("email", ""));
        user.setCreatedAt(json.optString("createdAt", ""));
        return user;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
