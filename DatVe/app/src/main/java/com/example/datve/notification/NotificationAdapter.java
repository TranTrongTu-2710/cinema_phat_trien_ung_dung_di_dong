package com.example.datve.notification;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.datve.R;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<Notification> notificationList;

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

        // Gọi hàm getTimestamp() đã được sửa lỗi triệt để ở model Notification
        holder.tvTimestamp.setText(notification.getTimestamp());

        // Xử lý giao diện cho thông báo đã đọc và chưa đọc
        if (notification.isRead()) {
            holder.ivUnreadIndicator.setVisibility(View.GONE);
            holder.tvTitle.setTypeface(null, Typeface.NORMAL);
            holder.rootLayout.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
        } else {
            holder.ivUnreadIndicator.setVisibility(View.VISIBLE);
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
            holder.rootLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.unread_notification_bg));
        }

        // Xử lý sự kiện click
        holder.itemView.setOnClickListener(v -> {
            String formattedMessage = notification.getContent().replaceAll("\\s*\\|\\s*", "\n");
            new AlertDialog.Builder(context)
                    .setTitle(notification.getTitle())
                    .setMessage(formattedMessage)
                    .setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss())
                    .setCancelable(true)
                    .show();

            // Đánh dấu là đã đọc sau khi bấm vào
            if (!notification.isRead()) {
                notification.setRead(true);
                notifyItemChanged(position); // Cập nhật lại giao diện của item này
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTimestamp;
        ImageView ivUnreadIndicator, ivNotificationIcon;
        LinearLayout rootLayout;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notification_title);
            tvContent = itemView.findViewById(R.id.tv_notification_content);
            tvTimestamp = itemView.findViewById(R.id.tv_notification_timestamp);
            ivUnreadIndicator = itemView.findViewById(R.id.iv_unread_indicator);
            rootLayout = itemView.findViewById(R.id.notification_item_root);
            ivNotificationIcon = itemView.findViewById(R.id.iv_notification_icon);
        }
    }
}
