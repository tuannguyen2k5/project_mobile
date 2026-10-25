package vn.edu.usth.appfinal.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import vn.edu.usth.appfinal.R;
import vn.edu.usth.appfinal.model.Recipe;
import vn.edu.usth.appfinal.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================================
 * [FRONTEND - ADAPTER] ADAPTER HIỂN THỊ DANH SÁCH THẺ MÓN ĂN (RecipeAdapter)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Kết nối dữ liệu từ danh sách `List<Recipe>` với các thành phần giao diện trong `recipe_item.xml`.
 *   2. Tái sử dụng (Recycle) các ViewHolder giúp cuộn danh sách hàng trăm món ăn mà không bị giật lag hay tràn RAM.
 *   3. Hiển thị: Ảnh đại diện món ăn (NetworkImageView), tên món, thời gian nấu, lượng calories,
 *      thông tin dinh dưỡng tóm tắt (Protein, Carbs, Fat), người đăng, và icon trái tim yêu thích.
 *   4. Bắn sự kiện (Callback Interface):
 *      + `OnRecipeClickListener`: Khi click vào cả thẻ món ăn.
 *      + `OnFavoriteClickListener`: Khi click riêng vào icon trái tim góc phải.
 * - Câu hỏi bảo vệ:
 *   + "Muốn sửa thông tin hiển thị trên từng thẻ món ăn thì sửa ở đâu?": Phương thức `onBindViewHolder()`.
 *   + "Cách tải ảnh và gán ảnh mặc định khi ảnh lỗi hoặc URL rỗng?": Xem dòng `holder.ivRecipeThumb.setDefaultImageResId(...)`
 *     và `holder.ivRecipeThumb.setImageUrl(...)`.
 * =========================================================================================
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    // Interface callback khi bấm vào thẻ món ăn
    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

    // Interface callback khi bấm vào icon trái tim trên thẻ
    public interface OnFavoriteClickListener {
        void onFavoriteClick(Recipe recipe, int position);
    }

    private List<Recipe> recipes = new ArrayList<>();
    private OnRecipeClickListener listener;
    private OnFavoriteClickListener favoriteClickListener;

    public RecipeAdapter(List<Recipe> recipes) {
        if (recipes != null) {
            this.recipes = new ArrayList<>(recipes);
        }
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener favoriteClickListener) {
        this.favoriteClickListener = favoriteClickListener;
    }

    /**
     * Cập nhật toàn bộ danh sách món ăn mới (khi tìm kiếm hoặc reload)
     */
    public void updateData(List<Recipe> newRecipes) {
        this.recipes = newRecipes != null ? new ArrayList<>(newRecipes) : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Khởi tạo ViewHolder bằng cách nạp layout XML `recipe_item.xml`
     */
    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    /**
     * [GẮN DỮ LIỆU VÀO TỪNG THẺ MÓN ĂN (BIND DATA TO VIEW)]:
     * - Đây là nơi các thầy cô thường yêu cầu thay đổi nhất (thêm text, đổi icon, đổi màu, format dữ liệu).
     */
    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        // 1. Tên món ăn
        holder.tvRecipeName.setText(recipe.getTitle());

        // 2. Thời gian nấu & Calories
        int totalTime = recipe.getTotalTime();
        if (totalTime == 0) totalTime = recipe.getCookingTime();
        String timeStr = totalTime > 0 ? (totalTime + " min") : "Nấu nhanh";
        String calStr = recipe.getCalories() > 0 ? ("  •  " + recipe.getCalories() + " kcal") : "";
        holder.tvRecipeInfo.setText(timeStr + calStr);

        // 3. Tóm tắt chỉ số dinh dưỡng (Macros: Protein, Carbs, Fat) và Người đăng
        String nutritionText = "";
        if (recipe.getProteinG() > 0 || recipe.getCarbsG() > 0 || recipe.getFatG() > 0) {
            nutritionText = String.format("P: %.0fg  •  C: %.0fg  •  F: %.0fg",
                    recipe.getProteinG(), recipe.getCarbsG(), recipe.getFatG());
        } else if (recipe.getPrepTime() > 0 || recipe.getCookTime() > 0) {
            nutritionText = "Chuẩn bị: " + recipe.getPrepTime() + "m  •  Nấu: " + recipe.getCookTime() + "m";
        }

        // Tên người đăng món ăn
        String author = recipe.getAuthorName();
        if (author != null && !author.trim().isEmpty() && !author.equalsIgnoreCase("null") && !author.equalsIgnoreCase("Hệ thống")) {
            if (!nutritionText.isEmpty()) {
                nutritionText += "  •  👤 " + author.trim();
            } else {
                nutritionText = "👤 " + author.trim();
            }
        }

        if (!nutritionText.isEmpty()) {
            holder.tvNutritionSummary.setVisibility(View.VISIBLE);
            holder.tvNutritionSummary.setText(nutritionText);
        } else {
            holder.tvNutritionSummary.setVisibility(View.GONE);
        }

        // 4. Trạng thái icon Trái tim Yêu thích (Đã thả tim vs Chưa thả tim)
        if (recipe.isFavorite()) {
            holder.ivFavorite.setImageResource(R.drawable.ic_heart_filled);
        } else {
            holder.ivFavorite.setImageResource(R.drawable.ic_heart_outline);
        }

        // 5. Nạp ảnh món ăn bằng Volley ImageLoader (kèm ảnh placeholder khi đang tải hoặc lỗi)
        ImageLoader imageLoader = ApiClient.getInstance(holder.itemView.getContext()).getImageLoader();
        holder.ivRecipeThumb.setVisibility(View.VISIBLE);
        holder.ivRecipeThumb.setDefaultImageResId(R.drawable.ic_food_placeholder);
        holder.ivRecipeThumb.setErrorImageResId(R.drawable.ic_food_placeholder);

        boolean hasImage = recipe.getImageUrl() != null 
                && !recipe.getImageUrl().trim().isEmpty() 
                && !recipe.getImageUrl().trim().equalsIgnoreCase("null");

        if (hasImage) {
            holder.ivRecipeThumb.setImageUrl(recipe.getImageUrl().trim(), imageLoader);
        } else {
            holder.ivRecipeThumb.setImageUrl(null, imageLoader);
            holder.ivRecipeThumb.setImageResource(R.drawable.ic_food_placeholder);
        }

        // Căn lề lề trái cho cụm nội dung text
        if (holder.layoutRecipeText != null && holder.layoutRecipeText.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) holder.layoutRecipeText.getLayoutParams();
            int marginPx = (int) (14 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            lp.setMarginStart(marginPx);
            holder.layoutRecipeText.setLayoutParams(lp);
        }

        // 6. Bắt sự kiện click vào cả thẻ -> Mở chi tiết
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe);
            }
        });

        // 7. Bắt sự kiện click riêng vào icon trái tim -> Thả tim / Bỏ tim
        holder.ivFavorite.setOnClickListener(v -> {
            if (favoriteClickListener != null) {
                favoriteClickListener.onFavoriteClick(recipe, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    /**
     * ViewHolder lưu trữ ánh xạ các view con trong recipe_item.xml để tránh findViewById nhiều lần
     */
    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        NetworkImageView ivRecipeThumb;
        View layoutRecipeText;
        TextView tvRecipeName;
        TextView tvRecipeInfo;
        TextView tvNutritionSummary;
        ImageView ivFavorite;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivRecipeThumb = itemView.findViewById(R.id.ivRecipeThumb);
            layoutRecipeText = itemView.findViewById(R.id.layoutRecipeText);
            tvRecipeName = itemView.findViewById(R.id.tvRecipeName);
            tvRecipeInfo = itemView.findViewById(R.id.tvRecipeInfo);
            tvNutritionSummary = itemView.findViewById(R.id.tvNutritionSummary);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
        }
    }
}
