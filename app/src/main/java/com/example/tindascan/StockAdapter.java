package com.example.tindascan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> implements Filterable {

    public interface OnProductActionListener {
        void onDelete(Product product);
        void onEdit(Product product);
    }

    private Context context;
    private List<Product> initialProductList;
    private List<Product> currentProductList;
    private OnProductActionListener listener;
    private ProductFilter productFilter;

    public StockAdapter(Context context, List<Product> productList, OnProductActionListener listener) {
        this.context = context;
        this.initialProductList = productList;
        this.currentProductList = productList;
        this.listener = listener;
    }

    // ... (onCreateViewHolder and onBindViewHolder methods remain the same) ...

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_stock_card, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        Product product = currentProductList.get(position);

        holder.tvProductName.setText(product.getName());
        holder.tvProductCategory.setText(product.getCategory());
        holder.tvProductBarcode.setText(product.getBarcode() == null || product.getBarcode().isEmpty() ? "—" : product.getBarcode());
        holder.tvProductPrice.setText("₱" + String.format("%.2f", product.getPrice()));
        holder.tvStockCount.setText(String.valueOf(product.getStockQuantity()));

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(product);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(product);
        });

    }

    @Override
    public int getItemCount() {
        return currentProductList.size();
    }

    // --- Implementation of Filterable Interface ---

    @Override
    public Filter getFilter() {
        if (productFilter == null) {
            productFilter = new ProductFilter();
        }
        return productFilter;
    }

    private class ProductFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<Product> filteredList = new ArrayList<>();

            String filterString = constraint.toString();
            String selectedCategory = "";
            String searchQuery = "";

            // 1. Parse the compound constraint string
            if (filterString.startsWith("COMPOUND:")) {
                String[] parts = filterString.split(":", 3);
                if (parts.length == 3) {
                    selectedCategory = parts[1];
                    searchQuery = parts[2].toLowerCase().trim();
                }
            } else if (filterString.startsWith("SEARCH_ONLY:")) {
                String[] parts = filterString.split(":", 2);
                if (parts.length == 2) {
                    searchQuery = parts[1].toLowerCase().trim();
                }
            } else {
                // Fallback for simple searches (in case there's an issue)
                searchQuery = filterString.toLowerCase().trim();
            }

            // 2. Perform Filtering
            if (searchQuery.isEmpty() && selectedCategory.isEmpty()) {
                // No filters applied
                filteredList.addAll(initialProductList);
            } else {
                for (Product product : initialProductList) {
                    boolean matchesCategory = true;
                    boolean matchesSearch = true;

                    // A. Check Category Match
                    if (!selectedCategory.isEmpty()) {
                        // Category must match EXACTLY
                        if (!product.getCategory().equals(selectedCategory)) {
                            matchesCategory = false;
                        }
                    }

                    // B. Check Search Query Match (by Name OR Barcode)
                    if (!searchQuery.isEmpty()) {
                        if (!(product.getName().toLowerCase().contains(searchQuery) ||
                                product.getBarcode().contains(searchQuery))) {
                            matchesSearch = false;
                        }
                    }

                    // C. Combine results: Must match BOTH category AND search query
                    if (matchesCategory && matchesSearch) {
                        filteredList.add(product);
                    }
                }
            }

            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            currentProductList = (List<Product>) results.values;
            notifyDataSetChanged();
        }
    }

    // --- END of Filterable Implementation ---

    static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvProductCategory, tvProductBarcode, tvProductPrice, tvStockCount;
        MaterialButton  btnDelete;
        MaterialButton  btnEdit;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductCategory = itemView.findViewById(R.id.tv_product_category);
            tvProductBarcode = itemView.findViewById(R.id.tv_product_barcode);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
            tvStockCount = itemView.findViewById(R.id.tv_stock_count);

            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnEdit = itemView.findViewById(R.id.btn_edit) ;
        }
    }
}