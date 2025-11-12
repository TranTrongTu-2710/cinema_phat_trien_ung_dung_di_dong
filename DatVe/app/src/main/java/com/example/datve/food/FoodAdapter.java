package com.example.datve.food;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.datve.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FoodAdapter extends RecyclerView.Adapter<FoodAdapter.FoodViewHolder> {

    private final List<Food> foods;
    // Sửa lại listener để đơn giản và hiệu quả hơn
    private final OnQuantityChangedListener listener;

    // Interface chỉ cần báo hiệu rằng số lượng đã thay đổi
    public interface OnQuantityChangedListener {
        void onQuantityChanged();
    }

    public FoodAdapter(List<Food> foods, OnQuantityChangedListener listener) {
        this.foods = foods;
        this.listener = listener;
    }

    @NonNull
    @Override
    public FoodViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.food_item, parent, false);
        return new FoodViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FoodViewHolder holder, int position) {
        Food food = foods.get(position);
        holder.bind(food, listener);
    }

    @Override
    public int getItemCount() {
        return foods.size();
    }

    static class FoodViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivFood;
        private final TextView tvFoodName;
        private final TextView tvFoodDescription;
        private final TextView tvFoodPrice;
        private final TextView tvQuantity;
        private final TextView btnDecrease;
        private final TextView btnIncrease;

        public FoodViewHolder(@NonNull View itemView) {
            super(itemView);
            // Giả sử ID của ImageView là 'iv_food' theo file của bạn
            ivFood = itemView.findViewById(R.id.iv_food);
            tvFoodName = itemView.findViewById(R.id.tv_food_name);
            tvFoodDescription = itemView.findViewById(R.id.tv_food_description);
            tvFoodPrice = itemView.findViewById(R.id.tv_food_price);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
        }

        public void bind(Food food, OnQuantityChangedListener listener) {
            // Load image
            if (food.getImage() != null && !food.getImage().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(food.getImage())
                        .placeholder(R.drawable.ic_food_placeholder) // Ảnh chờ
                        .error(R.drawable.ic_food_placeholder) // Ảnh lỗi
                        .into(ivFood);
            }

            tvFoodName.setText(food.getName());
            tvFoodDescription.setText(food.getDescription());

            NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
            tvFoodPrice.setText(format.format(food.getPrice()) + "đ");

            // Hiển thị số lượng ban đầu và trạng thái của nút
            updateQuantityView(food);

            btnDecrease.setOnClickListener(v -> {
                if (food.getQuantity() > 0) {
                    food.setQuantity(food.getQuantity() - 1);
                    updateQuantityView(food);
                    if (listener != null) {
                        listener.onQuantityChanged();
                    }
                }
            });

            btnIncrease.setOnClickListener(v -> {
                food.setQuantity(food.getQuantity() + 1);
                updateQuantityView(food);
                if (listener != null) {
                    listener.onQuantityChanged();
                }
            });
        }

        /**
         * Hàm helper để cập nhật giao diện của số lượng và các nút liên quan.
         * @param food Món ăn cần cập nhật.
         */
        private void updateQuantityView(Food food) {
            tvQuantity.setText(String.valueOf(food.getQuantity()));
            // Hiển thị hoặc ẩn nút giảm và TextView số lượng dựa trên giá trị
            boolean isVisible = food.getQuantity() > 0;
            tvQuantity.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
            btnDecrease.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
