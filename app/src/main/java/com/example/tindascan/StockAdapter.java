package com.example.tindascan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {

    public interface OnProductActionListener {
        void onDelete(Product product);
        void onEdit(Product product);
    }

    private Context context;
    private List<Product> productList;
    private OnProductActionListener listener;

    public StockAdapter(Context context, List<Product> productList, OnProductActionListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stock_card, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.tvProductName.setText(product.getName());
        holder.tvProductCategory.setText(product.getCategory());
        holder.tvProductBarcode.setText(product.getBarcode() == null || product.getBarcode().isEmpty() ? "—" : product.getBarcode());
        holder.tvProductPrice.setText("₱" + String.format("%.2f", product.getPrice()));
        holder.tvStockCount.setText(String.valueOf(product.getStockQuantity()));

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(product);
        });


    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvProductCategory, tvProductBarcode, tvProductPrice, tvStockCount;
        MaterialButton  btnDelete;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductCategory = itemView.findViewById(R.id.tv_product_category);
            tvProductBarcode = itemView.findViewById(R.id.tv_product_barcode);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
            tvStockCount = itemView.findViewById(R.id.tv_stock_count);

            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
