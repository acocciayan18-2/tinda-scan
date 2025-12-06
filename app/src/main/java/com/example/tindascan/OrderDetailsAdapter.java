package com.example.tindascan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class OrderDetailsAdapter extends RecyclerView.Adapter<OrderDetailsAdapter.DetailViewHolder> {

    private List<TransactionDetail> detailList;

    public OrderDetailsAdapter(List<TransactionDetail> detailList) {
        this.detailList = detailList;
    }

    @NonNull
    @Override
    public DetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_details, parent, false);
        return new DetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailViewHolder holder, int position) {
        TransactionDetail detail = detailList.get(position);
        holder.bind(detail);
    }

    @Override
    public int getItemCount() {
        return detailList.size();
    }

    static class DetailViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvQty, tvPrice;

        public DetailViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvQty = itemView.findViewById(R.id.tv_item_quantity);
            tvPrice = itemView.findViewById(R.id.tv_item_price);
        }

        public void bind(TransactionDetail detail) {
            tvName.setText(detail.getProductName());
            tvQty.setText(String.format(Locale.US, "%dx", detail.getQuantity()));

            double itemTotal = detail.getPriceAtSale() * detail.getQuantity();
            tvPrice.setText(String.format(Locale.US, "₱%.2f", itemTotal));
        }
    }
}