package vn.edu.usth.appfinal.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.DialogFragment;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import vn.edu.usth.appfinal.R;
import vn.edu.usth.appfinal.model.Recipe;
import vn.edu.usth.appfinal.network.ApiClient;
import vn.edu.usth.appfinal.network.ApiEndpoints;
import vn.edu.usth.appfinal.utils.SessionManager;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * =========================================================================================
 * [FRONTEND - UI] FORM ĐĂNG & CHỈNH SỬA CÔNG THỨC MÓN ĂN (AddRecipeFragment - DialogFragment)
 * =========================================================================================
 * - Nhiệm vụ:
 *   1. Chạy ở chế độ Fullscreen Dialog để người dùng nhập liệu thoải mái.
 *   2. Đảm nhận cả 2 chức năng:
 *      + THÊM MỚI (POST /api/recipes?userId={userId}): Khi bấm nút (+) ở trang chủ.
 *      + CHỈNH SỬA (PUT /api/recipes/{id}?userId={userId}): Khi bấm nút Sửa ở chi tiết món ăn của chính mình.
 *   3. Nhập động không giới hạn số lượng nguyên liệu (Tên, Định lượng, Đơn vị) và các bước nấu (Tự động đánh số Bước 1, Bước 2...).
 *   4. Thu thập toàn bộ dữ liệu, kiểm tra hợp lệ và đóng gói thành JSON gửi lên Spring Boot.
 * - Câu hỏi bảo vệ:
 *   + "Cách thêm/xóa nguyên liệu động trên giao diện làm thế nào?": Xem `addIngredientRow(...)` và `llIngredientsContainer.addView(row)`.
 *   + "Cách đánh số lại các bước khi xóa 1 bước ở giữa?": Xem hàm `renumberInstructionSteps()`.
 *   + "Làm sao phân biệt Thêm mới và Sửa?": Kiểm tra biến `recipeToEdit != null`. Nếu sửa thì method = PUT, thêm thì method = POST.
 * =========================================================================================
 */
public class AddRecipeFragment extends DialogFragment {

    public interface OnRecipeAddedListener {
        void onRecipeAdded(Recipe newRecipe);
    }

    // Các thành phần View giao diện
    private ImageView btnBack;
    private TextView tvHeaderTitle;
    private AppCompatButton btnQuickSave, btnSubmitRecipe;
    private EditText etTitle, etDescription, etImageUrl;
    private EditText etPrepTime, etCookTime, etServings;
    private EditText etCalories, etProtein, etCarbs, etFat;
    private AppCompatButton btnAddIngredient, btnAddInstruction;
    private LinearLayout llIngredientsContainer, llInstructionsContainer;
    private ProgressBar progressBarLoading;

    private SessionManager sessionManager;
    private OnRecipeAddedListener listener;
    private Recipe recipeToEdit;

    /**
     * Khởi tạo Fragment cho chức năng THÊM MỚI
     */
    public static AddRecipeFragment newInstance() {
        return new AddRecipeFragment();
    }

    /**
     * Khởi tạo Fragment cho chức năng CHỈNH SỬA (kèm dữ liệu món ăn cũ)
     */
    public static AddRecipeFragment newInstance(Recipe recipeToEdit) {
        AddRecipeFragment fragment = new AddRecipeFragment();
        Bundle args = new Bundle();
        args.putSerializable("recipe_to_edit", recipeToEdit);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnRecipeAddedListener(OnRecipeAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Thiết lập giao diện dạng Fullscreen không có thanh ActionBar mặc định
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);

        // Lấy thông tin món cần sửa nếu có truyền vào qua Arguments
        if (getArguments() != null && getArguments().containsKey("recipe_to_edit")) {
            recipeToEdit = (Recipe) getArguments().getSerializable("recipe_to_edit");
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_recipe, container, false);
        sessionManager = SessionManager.getInstance(requireContext());

        initViews(view);
        setupListeners();

        // Nếu là chế độ chỉnh sửa -> Đổ dữ liệu cũ vào các ô nhập
        if (recipeToEdit != null) {
            populateExistingRecipeData();
        } else {
            // Nếu là thêm mới -> Khởi tạo sẵn vài dòng mẫu để người dùng tiện nhập
            prepopulateDefaultRows();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Căn chỉnh kích thước cửa sổ chiếm trọn màn hình điện thoại
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

    /**
     * Ánh xạ các trường nhập liệu từ file giao diện fragment_add_recipe.xml
     */
    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle);
        btnQuickSave = view.findViewById(R.id.btnQuickSave);
        btnSubmitRecipe = view.findViewById(R.id.btnSubmitRecipe);

        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        etImageUrl = view.findViewById(R.id.etImageUrl);
        etPrepTime = view.findViewById(R.id.etPrepTime);
        etCookTime = view.findViewById(R.id.etCookTime);
        etServings = view.findViewById(R.id.etServings);

        etCalories = view.findViewById(R.id.etCalories);
        etProtein = view.findViewById(R.id.etProtein);
        etCarbs = view.findViewById(R.id.etCarbs);
        etFat = view.findViewById(R.id.etFat);

        btnAddIngredient = view.findViewById(R.id.btnAddIngredient);
        btnAddInstruction = view.findViewById(R.id.btnAddInstruction);
        llIngredientsContainer = view.findViewById(R.id.llIngredientsContainer);
        llInstructionsContainer = view.findViewById(R.id.llInstructionsContainer);
        progressBarLoading = view.findViewById(R.id.progressBarLoading);
    }

    /**
     * Đăng ký lắng nghe sự kiện click cho các nút bấm
     */
    private void setupListeners() {
        btnBack.setOnClickListener(v -> dismiss());
        btnQuickSave.setOnClickListener(v -> saveRecipe());
        btnSubmitRecipe.setOnClickListener(v -> saveRecipe());

        // Nút thêm dòng nguyên liệu mới
        btnAddIngredient.setOnClickListener(v -> addIngredientRow("", "", ""));
        // Nút thêm dòng bước nấu mới
        btnAddInstruction.setOnClickListener(v -> addInstructionRow(""));
    }

    /**
     * Khởi tạo sẵn 3 dòng nguyên liệu và 2 dòng bước nấu trống khi mở form thêm mới
     */
    private void prepopulateDefaultRows() {
        addIngredientRow("", "", "");
        addIngredientRow("", "", "");
        addIngredientRow("", "", "");

        addInstructionRow("");
        addInstructionRow("");
    }

    /**
     * [CHẾ ĐỘ CHỈNH SỬA]:
     * - Đổi tiêu đề Header thành "Chỉnh Sửa Món Ăn".
     * - Điền đầy đủ thông tin cũ của món ăn (Tên, mô tả, ảnh, thời gian, calo, đạm, carbs, chất béo).
     * - Tái hiện lại danh sách nguyên liệu và các bước hướng dẫn nấu cũ.
     */
    private void populateExistingRecipeData() {
        if (recipeToEdit == null) return;

        tvHeaderTitle.setText("Chỉnh Sửa Món Ăn");
        btnSubmitRecipe.setText("LƯU THAY ĐỔI CÔNG THỨC");
        btnQuickSave.setText("Lưu");

        etTitle.setText(recipeToEdit.getTitle() != null ? recipeToEdit.getTitle() : "");
        etDescription.setText(recipeToEdit.getDescription() != null && !recipeToEdit.getDescription().equalsIgnoreCase("null") ? recipeToEdit.getDescription() : "");
        etImageUrl.setText(recipeToEdit.getImageUrl() != null && !recipeToEdit.getImageUrl().equalsIgnoreCase("null") ? recipeToEdit.getImageUrl() : "");

        etPrepTime.setText(String.valueOf(recipeToEdit.getPrepTime()));
        etCookTime.setText(String.valueOf(recipeToEdit.getCookTime()));
        etServings.setText(String.valueOf(recipeToEdit.getServings() > 0 ? recipeToEdit.getServings() : 2));

        etCalories.setText(String.valueOf(recipeToEdit.getCalories()));
        etProtein.setText(String.valueOf(recipeToEdit.getProteinG()));
        etCarbs.setText(String.valueOf(recipeToEdit.getCarbsG()));
        etFat.setText(String.valueOf(recipeToEdit.getFatG()));

        // Điền danh sách nguyên liệu hiện có
        llIngredientsContainer.removeAllViews();
        if (recipeToEdit.getIngredients() != null && !recipeToEdit.getIngredients().isEmpty()) {
            for (var ing : recipeToEdit.getIngredients()) {
                if (ing != null) {
                    String qtyStr = "";
                    if (ing.getQuantity() != null && ing.getQuantity() > 0) {
                        qtyStr = (ing.getQuantity() == Math.floor(ing.getQuantity()))
                                ? String.valueOf((long) ing.getQuantity().doubleValue())
                                : String.valueOf(ing.getQuantity());
                    }
                    String unit = (ing.getUnit() != null && !ing.getUnit().equalsIgnoreCase("null")) ? ing.getUnit() : "";
                    addIngredientRow(ing.getName(), qtyStr, unit);
                }
            }
        } else {
            addIngredientRow("", "", "");
            addIngredientRow("", "", "");
        }

        // Điền danh sách các bước nấu hiện có
        llInstructionsContainer.removeAllViews();
        if (recipeToEdit.getInstructions() != null && !recipeToEdit.getInstructions().isEmpty()) {
            for (var inst : recipeToEdit.getInstructions()) {
                if (inst != null) {
                    String desc = (inst.getDescription() != null && !inst.getDescription().equalsIgnoreCase("null")) ? inst.getDescription() : "";
                    addInstructionRow(desc);
                }
            }
        } else {
            addInstructionRow("");
            addInstructionRow("");
        }
    }

    /**
     * [THÊM NGUYÊN LIỆU ĐỘNG]:
     * - Nạp layout item_add_ingredient_row.xml vào bên trong LinearLayout llIngredientsContainer.
     * - Bấm icon thùng rác (btnRemove) để xóa dòng đó khỏi màn hình.
     */
    private void addIngredientRow(String name, String quantity, String unit) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_add_ingredient_row, llIngredientsContainer, false);

        EditText etName = row.findViewById(R.id.etIngredientName);
        EditText etQty = row.findViewById(R.id.etIngredientQuantity);
        EditText etUnit = row.findViewById(R.id.etIngredientUnit);
        ImageView btnRemove = row.findViewById(R.id.btnRemoveIngredient);

        if (name != null) etName.setText(name);
        if (quantity != null) etQty.setText(quantity);
        if (unit != null) etUnit.setText(unit);

        btnRemove.setOnClickListener(v -> llIngredientsContainer.removeView(row));

        llIngredientsContainer.addView(row);
    }

    /**
     * [THÊM BƯỚC NẤU ĐỘNG]:
     * - Nạp layout item_add_instruction_row.xml vào bên trong llInstructionsContainer.
     * - Tự động cập nhật lại nhãn "Bước 1", "Bước 2", "Bước 3"... khi thêm hoặc xóa dòng.
     */
    private void addInstructionRow(String description) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_add_instruction_row, llInstructionsContainer, false);

        TextView tvStep = row.findViewById(R.id.tvStepNumber);
        EditText etDesc = row.findViewById(R.id.etInstructionDescription);
        ImageView btnRemove = row.findViewById(R.id.btnRemoveInstruction);

        if (description != null) etDesc.setText(description);

        btnRemove.setOnClickListener(v -> {
            llInstructionsContainer.removeView(row);
            renumberInstructionSteps(); // Đánh số lại các bước còn lại
        });

        llInstructionsContainer.addView(row);
        renumberInstructionSteps();
    }

    /**
     * Đánh số thứ tự lại các bước nấu liên tục từ Bước 1 đến Bước N
     */
    private void renumberInstructionSteps() {
        int count = llInstructionsContainer.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = llInstructionsContainer.getChildAt(i);
            TextView tvStep = child.findViewById(R.id.tvStepNumber);
            if (tvStep != null) {
                tvStep.setText("Bước " + (i + 1));
            }
        }
    }

    /**
     * [LƯU MÓN ĂN - THỰC THI GỬI DỮ LIỆU LÊN SERVER]:
     * 1. Kiểm tra session đăng nhập và validate bắt buộc tên món.
     * 2. Lấy các chỉ số thời gian nấu và dinh dưỡng (có fallback giá trị mặc định hợp lý).
     * 3. Duyệt container nguyên liệu để tạo JSONArray ingredients.
     * 4. Duyệt container bước nấu để tạo JSONArray instructions.
     * 5. Gói tất cả vào 1 đối tượng JSONObject tương thích với RecipeRequestDto phía backend.
     * 6. Nếu sửa: gửi PUT /api/recipes/{id}. Nếu tạo mới: gửi POST /api/recipes.
     */
    private void saveRecipe() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập tài khoản để thêm món ăn", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            etTitle.setError("Tên món ăn không được để trống");
            etTitle.requestFocus();
            Toast.makeText(requireContext(), "Vui lòng nhập tên món ăn", Toast.LENGTH_SHORT).show();
            return;
        }

        String desc = etDescription.getText().toString().trim();
        String imgUrl = etImageUrl.getText().toString().trim();

        // 1. Thời gian & Khẩu phần
        int prepTime = 15;
        try {
            String val = etPrepTime.getText().toString().trim();
            if (!val.isEmpty()) prepTime = Integer.parseInt(val);
        } catch (Exception ignored) {}

        int cookTime = 20;
        try {
            String val = etCookTime.getText().toString().trim();
            if (!val.isEmpty()) cookTime = Integer.parseInt(val);
        } catch (Exception ignored) {}

        int servings = 2;
        try {
            String val = etServings.getText().toString().trim();
            if (!val.isEmpty()) servings = Integer.parseInt(val);
        } catch (Exception ignored) {}

        // 2. Chỉ số dinh dưỡng (Macros)
        int calories = 350;
        try {
            String val = etCalories.getText().toString().trim();
            if (!val.isEmpty()) calories = Integer.parseInt(val);
        } catch (Exception ignored) {}

        double protein = 25.0;
        try {
            String val = etProtein.getText().toString().trim();
            if (!val.isEmpty()) protein = Double.parseDouble(val);
        } catch (Exception ignored) {}

        double carbs = 30.0;
        try {
            String val = etCarbs.getText().toString().trim();
            if (!val.isEmpty()) carbs = Double.parseDouble(val);
        } catch (Exception ignored) {}

        double fat = 10.0;
        try {
            String val = etFat.getText().toString().trim();
            if (!val.isEmpty()) fat = Double.parseDouble(val);
        } catch (Exception ignored) {}

        // 3. Đóng gói danh sách nguyên liệu (Ingredients)
        JSONArray ingredientsArray = new JSONArray();
        int ingChildCount = llIngredientsContainer.getChildCount();
        for (int i = 0; i < ingChildCount; i++) {
            View child = llIngredientsContainer.getChildAt(i);
            EditText etName = child.findViewById(R.id.etIngredientName);
            EditText etQty = child.findViewById(R.id.etIngredientQuantity);
            EditText etUnit = child.findViewById(R.id.etIngredientUnit);

            String ingName = etName.getText().toString().trim();
            if (!ingName.isEmpty()) {
                JSONObject ingObj = new JSONObject();
                try {
                    ingObj.put("name", ingName);

                    String qtyStr = etQty.getText().toString().trim();
                    if (!qtyStr.isEmpty()) {
                        try {
                            ingObj.put("quantity", Double.parseDouble(qtyStr));
                        } catch (Exception e) {
                            ingObj.put("quantity", JSONObject.NULL);
                        }
                    } else {
                        ingObj.put("quantity", JSONObject.NULL);
                    }

                    String unitStr = etUnit.getText().toString().trim();
                    if (!unitStr.isEmpty() && !unitStr.equalsIgnoreCase("null")) {
                        ingObj.put("unit", unitStr);
                    } else {
                        ingObj.put("unit", "");
                    }

                    ingredientsArray.put(ingObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // 4. Đóng gói các bước chế biến (Instructions)
        JSONArray instructionsArray = new JSONArray();
        int instChildCount = llInstructionsContainer.getChildCount();
        int stepNumber = 1;
        for (int i = 0; i < instChildCount; i++) {
            View child = llInstructionsContainer.getChildAt(i);
            EditText etDesc = child.findViewById(R.id.etInstructionDescription);
            String stepDesc = etDesc.getText().toString().trim();
            if (!stepDesc.isEmpty() && !stepDesc.equalsIgnoreCase("null")) {
                JSONObject instObj = new JSONObject();
                try {
                    instObj.put("stepNumber", stepNumber++);
                    instObj.put("description", stepDesc);
                    instructionsArray.put(instObj);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        // 5. Đóng gói toàn bộ vào JSON khớp 100% với RecipeRequestDto của Backend
        JSONObject req = new JSONObject();
        try {
            req.put("title", title);
            req.put("description", (desc.isEmpty() || desc.equalsIgnoreCase("null")) ? "" : desc);
            req.put("imageUrl", (imgUrl.isEmpty() || imgUrl.equalsIgnoreCase("null")) ? JSONObject.NULL : imgUrl);
            req.put("prepTime", prepTime);
            req.put("cookTime", cookTime);
            req.put("servings", servings);
            req.put("calories", calories);
            req.put("proteinG", protein);
            req.put("carbsG", carbs);
            req.put("fatG", fat);
            req.put("ingredients", ingredientsArray);
            req.put("instruction", instructionsArray);
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi khi xử lý dữ liệu món ăn", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bật trạng thái Loading để tránh người dùng nhấn lưu nhiều lần
        setLoading(true);

        long userId = sessionManager.getUserId();
        boolean isEditing = (recipeToEdit != null && recipeToEdit.getId() > 0);
        int method = isEditing ? Request.Method.PUT : Request.Method.POST;
        String requestUrl = isEditing
                ? ApiEndpoints.getUpdateRecipeUrl(recipeToEdit.getId(), userId)
                : ApiEndpoints.getCreateRecipeUrl(userId);
        String successMessage = isEditing ? "Cập nhật món ăn thành công!" : "Thêm món ăn thành công!";

        // Gửi Volley JsonObjectRequest lên Spring Boot
        JsonObjectRequest saveReq = new JsonObjectRequest(
                method,
                requestUrl,
                req,
                resp -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show();
                    // Bắn callback để màn hình cha (MainActivity hoặc RecipeDetailActivity) cập nhật lại UI
                    if (listener != null) {
                        Recipe savedRecipe = Recipe.fromJson(resp);
                        listener.onRecipeAdded(savedRecipe);
                    }
                    dismiss();
                },
                err -> {
                    setLoading(false);
                    String errMsg = isEditing ? "Không thể cập nhật món ăn" : "Không thể thêm món ăn";
                    if (err.networkResponse != null && err.networkResponse.data != null) {
                        try {
                            String errBody = new String(err.networkResponse.data, StandardCharsets.UTF_8);
                            JSONObject errJson = new JSONObject(errBody);
                            if (errJson.has("message")) {
                                errMsg = errJson.getString("message");
                            }
                        } catch (Exception ignored) {}
                    }
                    Toast.makeText(requireContext(), errMsg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-User-Id", String.valueOf(userId));
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };

        ApiClient.getQueue(requireContext()).add(saveReq);
    }

    /**
     * Bật/tắt vòng xoay Loading và khóa nút bấm khi đang chờ server phản hồi
     */
    private void setLoading(boolean loading) {
        if (progressBarLoading != null) {
            progressBarLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (btnSubmitRecipe != null) {
            btnSubmitRecipe.setEnabled(!loading);
        }
        if (btnQuickSave != null) {
            btnQuickSave.setEnabled(!loading);
        }
    }
}
