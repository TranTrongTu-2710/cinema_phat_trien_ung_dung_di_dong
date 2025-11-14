package com.example.datve.notification;

import static android.text.TextUtils.split;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.datve.R;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<Notification> notificationList;

    public NotificationAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvContent.setText(notification.getContent());
        holder.tvTimestamp.setText(notification.getTimestamp());

        // Xử lý giao diện cho thông báo đã đọc và chưa đọc
        if (notification.isRead()) {
            holder.ivUnreadIndicator.setVisibility(View.GONE);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.rootLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        } else {
            holder.ivUnreadIndicator.setVisibility(View.VISIBLE);
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            // Sử dụng ContextCompat để lấy màu an toàn hơn
            holder.rootLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.unread_notification_bg));
        }

        // Xử lý sự kiện click vào một thông báo
        holder.itemView.setOnClickListener(v -> {
//            if (!notification.isRead()) {
//                // Chỉ xử lý khi thông báo chưa đọc
//                Toast.makeText(context, "Đã đọc: " + notification.getTitle(), Toast.LENGTH_SHORT).show();
//                notification.setRead(true);
//                notifyItemChanged(position); // Cập nhật lại item này
//            }
            String msg = notification.getContent().replace("|", "\n");
            msg = notification.getContent().replaceAll("\\s*\\|\\s*", "\n");
            new AlertDialog.Builder(context)
                    .setTitle(notification.getTitle()) // Đặt tiêu đề của dialog
                    .setMessage(msg) // Đặt nội dung chi tiết của dialog
                    .setPositiveButton("Đóng", (dialog, which) -> {
                        // Khi nhấn nút "Đóng", chỉ cần tắt dialog đi
                        dialog.dismiss();
                    })
                    .setCancelable(true) // Cho phép người dùng tắt dialog bằng cách bấm ra ngoài
                    .show(); // Hiển thị dialog
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTimestamp;
        ImageView ivUnreadIndicator;
        LinearLayout rootLayout;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvContent = itemView.findViewById(R.id.tv_notification_content);
            tvTimestamp = itemView.findViewById(R.id.tv_notification_timestamp);
            ivUnreadIndicator = itemView.findViewById(R.id.iv_unread_indicator);
            rootLayout = itemView.findViewById(R.id.notification_item_root);
        }
    }
}
