package vn.edu.usth.appfinal.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import vn.edu.usth.appfinal.ui.FavoritesFragment;
import vn.edu.usth.appfinal.ui.RecipesFragment;

/**
 * =========================================================================================
 * [FRONTEND - ADAPTER] ADAPTER QUẢN LÝ 2 TAB CỦA VIEWPAGER2 (HomePagerAdapter)
 * =========================================================================================
 * - Sử dụng FragmentStateAdapter để quản lý vòng đời của 2 Fragment chính trong ViewPager2:
 *   + Tab 0 (position = 0): RecipesFragment (Hiển thị danh sách tất cả món ăn).
 *   + Tab 1 (position = 1): FavoritesFragment (Hiển thị danh sách món đã lưu yêu thích).
 * - Cung cấp hàm getter `getRecipesFragment()` và `getFavoritesFragment()` để MainActivity
 *   có thể gọi trực tiếp các phương thức reload/tìm kiếm của Fragment con.
 * =========================================================================================
 */
public class HomePagerAdapter extends FragmentStateAdapter {

    private RecipesFragment recipesFragment;
    private FavoritesFragment favoritesFragment;

    public HomePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    /**
     * Khởi tạo Fragment tương ứng theo vị trí tab (position):
     * - position 0 -> RecipesFragment
     * - position 1 -> FavoritesFragment
     */
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

    /**
     * Tổng số tab hiển thị trên màn hình chính: 2 tab
     */
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
