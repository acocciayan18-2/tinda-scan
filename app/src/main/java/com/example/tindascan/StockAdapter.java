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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
    private String lastFilter = "";

    public StockAdapter(Context context, List<Product> productList, OnProductActionListener listener) {
        this.context = context;
        this.initialProductList = new ArrayList<>(productList);
        this.currentProductList = new ArrayList<>(productList);
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
        Product product = currentProductList.get(position);

        holder.tvProductName.setText(product.getName());
        holder.tvProductCategory.setText(product.getCategory());
        holder.tvProductBarcode.setText(product.getBarcode() == null || product.getBarcode().isEmpty() ? "—" : product.getBarcode());
        holder.tvProductPrice.setText("₱" + String.format("%.2f", product.getPrice()));
        holder.tvStockCount.setText(String.valueOf(product.getStockQuantity()));

        String exp = product.getExpirationDate();
        if (exp == null || exp.isEmpty()) {
            holder.tvProductExpiration.setText("No Expiration");
        } else {
            String formattedDate = formatDate(exp);
            holder.tvProductExpiration.setText(formattedDate);

            long expMillis = getExpirationMillis(exp);
            long today = System.currentTimeMillis();

            if (expMillis < today) {
                holder.tvProductExpiration.setTextColor(context.getColor(android.R.color.holo_red_dark));
            } else {
                holder.tvProductExpiration.setTextColor(context.getColor(android.R.color.holo_green_dark));
            }
        }

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

    private String formatDate(String dateStr) {
        try {
            // Assumes database stores as yyyy-MM-dd
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = inputFormat.parse(dateStr);

            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy", Locale.US);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private long getExpirationMillis(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = sdf.parse(dateStr);
            if (d != null) return d.getTime();
        } catch (Exception ignored) {}
        return Long.MAX_VALUE;
    }

    public void setInitialProductList(List<Product> list) {
        if (list == null) list = new ArrayList<>();
        initialProductList = new ArrayList<>(list);
        currentProductList = new ArrayList<>(list);

        if (!lastFilter.isEmpty()) {
            getFilter().filter(lastFilter);
        } else {
            notifyDataSetChanged();
        }
    }

    @Override
    public Filter getFilter() {
        if (productFilter == null) productFilter = new ProductFilter();
        return productFilter;
    }

    public void updateList(List<Product> newList) {
        currentProductList = newList;
        notifyDataSetChanged();
    }

    // --- Filter class ---
    private class ProductFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {

            lastFilter = constraint.toString();

            List<Product> filtered = new ArrayList<>();

            String filter = constraint.toString();
            String category = "";
            String search = "";

            // NEW: Expiration filter
            if (filter.equals("EXPIRING")) {
                long today = System.currentTimeMillis();

                for (Product p : initialProductList) {
                    String exp = p.getExpirationDate();

                    if (exp == null || exp.isEmpty()) continue;

                    long expMillis = getExpirationMillis(exp);

                    if (expMillis <= today) {
                        filtered.add(p);
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            if (filter.startsWith("COMPOUND:")) {
                String[] parts = filter.split(":", 3);
                if (parts.length == 3) {
                    category = parts[1];
                    search = parts[2].toLowerCase().trim();
                }
            } else if (filter.startsWith("SEARCH_ONLY:")) {
                String[] parts = filter.split(":", 2);
                if (parts.length == 2) search = parts[1].toLowerCase().trim();
            } else {
                search = filter.toLowerCase().trim();
            }

            for (Product p : initialProductList) {
                boolean matchCat = category.isEmpty() || p.getCategory().equals(category);
                boolean matchSearch =
                        search.isEmpty() ||
                                p.getName().toLowerCase().contains(search) ||
                                p.getBarcode().contains(search);

                if (matchCat && matchSearch) filtered.add(p);
            }

            FilterResults results = new FilterResults();
            results.values = filtered;
            results.count = filtered.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            currentProductList = (List<Product>) results.values;
            notifyDataSetChanged();
        }
    }

    static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvProductName, tvProductCategory, tvProductBarcode, tvProductPrice, tvStockCount;
        TextView tvProductExpiration;
        MaterialButton btnDelete, btnEdit;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductCategory = itemView.findViewById(R.id.tv_product_category);
            tvProductBarcode = itemView.findViewById(R.id.tv_product_barcode);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
            tvStockCount = itemView.findViewById(R.id.tv_stock_count);
            tvProductExpiration = itemView.findViewById(R.id.tv_product_expiration);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnEdit = itemView.findViewById(R.id.btn_edit);
        }
    }
}