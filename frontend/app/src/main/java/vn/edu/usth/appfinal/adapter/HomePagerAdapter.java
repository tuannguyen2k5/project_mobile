package vn.edu.usth.appfinal.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import vn.edu.usth.appfinal.ui.FavoritesFragment;
import vn.edu.usth.appfinal.ui.RecipesFragment;

public class HomePagerAdapter extends FragmentStateAdapter {

    private RecipesFragment recipesFragment;
    private FavoritesFragment favoritesFragment;

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            recipesFragment = new RecipesFragment();
            return recipesFragment;
        } else {
            favoritesFragment = new FavoritesFragment();
            return favoritesFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public RecipesFragment getRecipesFragment() {
        return recipesFragment;
    }

    public FavoritesFragment getFavoritesFragment() {
        return favoritesFragment;
    }
}
