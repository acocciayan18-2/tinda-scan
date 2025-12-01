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

public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ViewHolder> {

    private Context context;
    private List<ActivityLog> logList;

    public RecentActivityAdapter(Context context, List<ActivityLog> logList) {
        this.context = context;
        this.logList = logList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityLog log = logList.get(position);

        holder.tvDetails.setText(log.getDetails());
        holder.tvTime.setText(log.getTimestamp());

        // Dynamic Styling based on Type
        switch (log.getType()) {
            case "SALE":
                holder.tvTitle.setText("New Sale");
                holder.ivIcon.setImageResource(R.drawable.ic_peso_sign);
                holder.cardIconBg.setCardBackgroundColor(Color.parseColor("#E8F5E9")); // Light Green
                holder.ivIcon.setColorFilter(Color.parseColor("#4CAF50"));
                break;
            case "ADD":
                holder.tvTitle.setText("Product Added");
                holder.ivIcon.setImageResource(R.drawable.ic_add);
                holder.cardIconBg.setCardBackgroundColor(Color.parseColor("#E3F2FD")); // Light Blue
                holder.ivIcon.setColorFilter(Color.parseColor("#2196F3"));
                break;
            case "EDIT":
                holder.tvTitle.setText("Product Updated");
                holder.ivIcon.setImageResource(R.drawable.ic_edit);
                holder.cardIconBg.setCardBackgroundColor(Color.parseColor("#FFF3E0")); // Light Orange
                holder.ivIcon.setColorFilter(Color.parseColor("#FF9800"));
                break;
            case "DELETE":
                holder.tvTitle.setText("Product Deleted");
                holder.ivIcon.setImageResource(R.drawable.ic_trash_stroke);
                holder.cardIconBg.setCardBackgroundColor(Color.parseColor("#FFEBEE")); // Light Red
                holder.ivIcon.setColorFilter(Color.parseColor("#F44336"));
                break;
            case "BACKUP":
                holder.tvTitle.setText("Data Backup");
                holder.ivIcon.setImageResource(R.drawable.ic_download); // or cloud upload
                holder.cardIconBg.setCardBackgroundColor(Color.parseColor("#F3E5F5")); // Light Purple
                holder.ivIcon.setColorFilter(Color.parseColor("#9C27B0"));
                break;
            case "RESTORE":
                holder.tvTitle.setText("Data Restored");
                holder.ivIcon.setImageResource(R.drawable.ic_upload); // or cloud download
                holder.cardIconBg.setCardBackgroundColor(Color.parseColor("#F3E5F5")); // Light Purple
                holder.ivIcon.setColorFilter(Color.parseColor("#9C27B0"));
                break;
            default:
                holder.tvTitle.setText("Activity");
                holder.ivIcon.setImageResource(R.drawable.ic_history);
                holder.cardIconBg.setCardBackgroundColor(Color.parseColor("#EEEEEE"));
                holder.ivIcon.setColorFilter(Color.parseColor("#757575"));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public void updateList(List<ActivityLog> newList) {
        this.logList = newList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails, tvTime;
        ImageView ivIcon;
        MaterialCardView cardIconBg;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_activity_title);
            tvDetails = itemView.findViewById(R.id.tv_activity_details);
            tvTime = itemView.findViewById(R.id.tv_activity_time);
            ivIcon = itemView.findViewById(R.id.iv_activity_icon);
            cardIconBg = itemView.findViewById(R.id.card_icon_bg);
        }
    }
}