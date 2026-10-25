package vn.edu.usth.appfinal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import vn.edu.usth.appfinal.R;
import vn.edu.usth.appfinal.model.Ingredient;

import java.util.List;

/**
 * =========================================================================================
 * [FRONTEND - ADAPTER] ADAPTER HIỂN THỊ DANH SÁCH NGUYÊN LIỆU (IngredientAdapter)
 * =========================================================================================
 * - Dùng trong `RecipeDetailActivity` để hiển thị từng dòng nguyên liệu (`item_ingredient.xml`).
 * - Hiển thị:
 *   + Tên nguyên liệu (tvIngredientName, vd: "Thịt bò thăn").
 *   + Định lượng & đơn vị (tvIngredientQuantity, vd: "300g" hoặc "2 thìa cà phê").
 * =========================================================================================
 */
public class IngredientAdapter extends RecyclerView.Adapter<IngredientAdapter.ViewHolder> {

    private final List<Ingredient> ingredients;

    public IngredientAdapter(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ingredient, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Gắn tên nguyên liệu và định lượng vào từng dòng item_ingredient
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ingredient item = ingredients.get(position);
        holder.tvIngredientName.setText(item.getName());
        
        // Định lượng nguyên liệu (số lượng + đơn vị)
        String formattedQty = item.getFormattedQuantity();
        if (formattedQty == null || formattedQty.trim().isEmpty() || formattedQty.trim().equalsIgnoreCase("null")) {
            holder.tvIngredientQuantity.setText("");
            holder.tvIngredientQuantity.setVisibility(View.GONE);
        } else {
            holder.tvIngredientQuantity.setVisibility(View.VISIBLE);
            holder.tvIngredientQuantity.setText(formattedQty.trim());
        }
    }

    @Override
    public int getItemCount() {
        return ingredients != null ? ingredients.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIngredientName;
        TextView tvIngredientQuantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIngredientName = itemView.findViewById(R.id.tvIngredientName);
            tvIngredientQuantity = itemView.findViewById(R.id.tvIngredientQuantity);
        }
    }
}
