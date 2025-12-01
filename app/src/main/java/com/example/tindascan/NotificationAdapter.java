package com.example.tindascan;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private Context context;
    private List<AppNotification> notificationList;

    public NotificationAdapter(Context context, List<AppNotification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification notif = notificationList.get(position);

        holder.tvTitle.setText(notif.getTitle());
        holder.tvMessage.setText(notif.getMessage());
        holder.tvTime.setText(notif.getTimestamp());

        // Style based on Type
        if ("INVENTORY".equals(notif.getType())) {
            holder.iconBg.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Red bg
            holder.icon.setImageResource(R.drawable.ic_low_stock_graph);
            holder.icon.setColorFilter(Color.parseColor("#D32F2F"));
        } else if ("EXPIRY".equals(notif.getType())) {
            holder.iconBg.setCardBackgroundColor(Color.parseColor("#FFF3E0")); // Orange bg
            holder.icon.setImageResource(R.drawable.ic_history); // Clock icon
            holder.icon.setColorFilter(Color.parseColor("#EF6C00"));
        } else {
            holder.iconBg.setCardBackgroundColor(Color.parseColor("#E3F2FD")); // Blue bg
            holder.icon.setImageResource(R.drawable.ic_notification_bell);
            holder.icon.setColorFilter(Color.parseColor("#1976D2"));
        }

        // Unread styling
        if (!notif.isRead()) {
            holder.unreadDot.setVisibility(View.VISIBLE);
        } else {
            holder.unreadDot.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public void removeItem(int position) {
        notificationList.remove(position);
        notifyItemRemoved(position);
    }

    public AppNotification getItem(int position) {
        return notificationList.get(position);
    }

    public void updateList(List<AppNotification> newList) {
        this.notificationList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        ImageView icon;
        MaterialCardView iconBg;
        View unreadDot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_notif_title);
            tvMessage = itemView.findViewById(R.id.tv_notif_message);
            tvTime = itemView.findViewById(R.id.tv_notif_time);
            icon = itemView.findViewById(R.id.iv_notif_icon);
            iconBg = itemView.findViewById(R.id.card_notif_icon_bg);
            unreadDot = itemView.findViewById(R.id.view_unread_dot);
        }
    }
}