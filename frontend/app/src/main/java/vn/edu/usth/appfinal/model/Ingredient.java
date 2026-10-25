package vn.edu.usth.appfinal.model;

import org.json.JSONException;
import org.json.JSONObject;
import java.io.Serializable;

public class Ingredient implements Serializable {
    private String name;
    private Double quantity;
    private String unit;

    public Ingredient() {}

    public Ingredient(String name, Double quantity, String unit) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }

    public static Ingredient fromJson(JSONObject json) {
        if (json == null) return null;
        Ingredient item = new Ingredient();

        String name = json.isNull("name") ? "" : json.optString("name", "");
        if ("null".equalsIgnoreCase(name)) name = "";
        item.setName(name);

        if (json.has("quantity") && !json.isNull("quantity")) {
            item.setQuantity(json.optDouble("quantity", 0.0));
        } else {
            item.setQuantity(null);
        }

        String unit = json.isNull("unit") ? "" : json.optString("unit", "");
        if ("null".equalsIgnoreCase(unit)) unit = "";
        item.setUnit(unit.trim());

        return item;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", name != null ? name : "");
            if (quantity != null) {
                json.put("quantity", quantity);
            } else {
                json.put("quantity", JSONObject.NULL);
            }
            json.put("unit", (unit != null && !unit.equalsIgnoreCase("null")) ? unit.trim() : "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public String getFormattedQuantity() {
        String cleanUnit = (unit != null && !unit.trim().isEmpty() && !unit.trim().equalsIgnoreCase("null"))
                ? unit.trim()
                : "";
        if (quantity == null || quantity <= 0) {
            return cleanUnit;
        }
        String qtyStr = (quantity == Math.floor(quantity))
                ? String.format("%d", (long) quantity.doubleValue())
                : String.format("%.1f", quantity);
        if (!cleanUnit.isEmpty()) {
            return qtyStr + " " + cleanUnit;
        }
        return qtyStr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
