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

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(Recipe recipe);
    }

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

    public void updateData(List<Recipe> newRecipes) {
        this.recipes = newRecipes != null ? new ArrayList<>(newRecipes) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recipe_item, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);

        holder.tvRecipeName.setText(recipe.getTitle());

        int totalTime = recipe.getTotalTime();
        if (totalTime == 0) totalTime = recipe.getCookingTime();
        String timeStr = totalTime > 0 ? (totalTime + " min") : "Nấu nhanh";
        String calStr = recipe.getCalories() > 0 ? ("  •  " + recipe.getCalories() + " kcal") : "";
        holder.tvRecipeInfo.setText(timeStr + calStr);

        String nutritionText = "";
        if (recipe.getProteinG() > 0 || recipe.getCarbsG() > 0 || recipe.getFatG() > 0) {
            nutritionText = String.format("P: %.0fg  •  C: %.0fg  •  F: %.0fg",
                    recipe.getProteinG(), recipe.getCarbsG(), recipe.getFatG());
        } else if (recipe.getPrepTime() > 0 || recipe.getCookTime() > 0) {
            nutritionText = "Chuẩn bị: " + recipe.getPrepTime() + "m  •  Nấu: " + recipe.getCookTime() + "m";
        }

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

        // Favorite icon state
        if (recipe.isFavorite()) {
            holder.ivFavorite.setImageResource(R.drawable.ic_heart_filled);
        } else {
            holder.ivFavorite.setImageResource(R.drawable.ic_heart_outline);
        }

        // Image loading via Volley ImageLoader - Luôn giữ khung ảnh bên trái, không cắt bỏ
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

        if (holder.layoutRecipeText != null && holder.layoutRecipeText.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) holder.layoutRecipeText.getLayoutParams();
            int marginPx = (int) (14 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            lp.setMarginStart(marginPx);
            holder.layoutRecipeText.setLayoutParams(lp);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe);
            }
        });

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
