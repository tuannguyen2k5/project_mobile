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

public class AddRecipeFragment extends DialogFragment {

    public interface OnRecipeAddedListener {
        void onRecipeAdded(Recipe newRecipe);
    }

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

    public static AddRecipeFragment newInstance() {
        return new AddRecipeFragment();
    }

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
        // Chế độ toàn màn hình để người dùng nhập đầy đủ nguyên liệu và công thức một cách thoải mái
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);

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

        if (recipeToEdit != null) {
            populateExistingRecipeData();
        } else {
            prepopulateDefaultRows();
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }

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

    private void setupListeners() {
        btnBack.setOnClickListener(v -> dismiss());
        btnQuickSave.setOnClickListener(v -> saveRecipe());
        btnSubmitRecipe.setOnClickListener(v -> saveRecipe());

        btnAddIngredient.setOnClickListener(v -> addIngredientRow("", "", ""));
        btnAddInstruction.setOnClickListener(v -> addInstructionRow(""));
    }

    private void prepopulateDefaultRows() {
        // Khởi tạo sẵn 3 dòng nguyên liệu mẫu để người dùng bắt đầu nhập ngay
        addIngredientRow("", "", "");
        addIngredientRow("", "", "");
        addIngredientRow("", "", "");

        // Khởi tạo sẵn 2 bước nấu mẫu
        addInstructionRow("");
        addInstructionRow("");
    }

    /**
     * Điền toàn bộ thông tin món ăn, dinh dưỡng, nguyên liệu và các bước khi ở chế độ chỉnh sửa
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
     * Thêm một dòng nguyên liệu mới vào danh sách
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
     * Thêm một bước nấu mới vào danh sách công thức chế biến
     */
    private void addInstructionRow(String description) {
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_add_instruction_row, llInstructionsContainer, false);

        TextView tvStep = row.findViewById(R.id.tvStepNumber);
        EditText etDesc = row.findViewById(R.id.etInstructionDescription);
        ImageView btnRemove = row.findViewById(R.id.btnRemoveInstruction);

        if (description != null) etDesc.setText(description);

        btnRemove.setOnClickListener(v -> {
            llInstructionsContainer.removeView(row);
            renumberInstructionSteps();
        });

        llInstructionsContainer.addView(row);
        renumberInstructionSteps();
    }

    /**
     * Đánh số thứ tự lại các bước nấu (Bước 1, Bước 2, ...) sau khi thêm hoặc xóa
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
     * Thu thập toàn bộ dữ liệu (thông tin chung, dinh dưỡng, nguyên liệu, công thức) và gửi lên Backend
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

        // 1. Thời gian & khẩu phần
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

        // 2. Dinh dưỡng
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

        // 3. Nguyên liệu (Ingredients)
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

        // 4. Công thức chế biến & Các bước nấu (Instructions)
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

        // Đóng gói JSON tương thích 100% với RecipeRequestDto
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

        setLoading(true);

        long userId = sessionManager.getUserId();
        boolean isEditing = (recipeToEdit != null && recipeToEdit.getId() > 0);
        int method = isEditing ? Request.Method.PUT : Request.Method.POST;
        String requestUrl = isEditing
                ? ApiEndpoints.getUpdateRecipeUrl(recipeToEdit.getId(), userId)
                : ApiEndpoints.getCreateRecipeUrl(userId);
        String successMessage = isEditing ? "Cập nhật món ăn thành công!" : "Thêm món ăn thành công!";

        JsonObjectRequest saveReq = new JsonObjectRequest(
                method,
                requestUrl,
                req,
                resp -> {
                    setLoading(false);
                    Toast.makeText(requireContext(), successMessage, Toast.LENGTH_LONG).show();
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
