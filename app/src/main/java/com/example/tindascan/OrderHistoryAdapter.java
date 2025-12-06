package com.example.tindascan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.HistoryViewHolder> {

    private List<Transaction> transactionList;
    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onItemClick(Transaction transaction);
    }

    public OrderHistoryAdapter(List<Transaction> transactionList, OnHistoryItemClickListener listener) {
        this.transactionList = transactionList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Transaction transaction = transactionList.get(position);
        holder.bind(transaction, listener);
    }

    @Override
    public int getItemCount() {
        return transactionList.size();
    }

    public void updateList(List<Transaction> newList) {
        this.transactionList = newList;
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvId, tvDate, tvTotal;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvId = itemView.findViewById(R.id.tv_transaction_id);
            tvDate = itemView.findViewById(R.id.tv_transaction_date);
            tvTotal = itemView.findViewById(R.id.tv_transaction_total);
        }

        public void bind(final Transaction transaction, final OnHistoryItemClickListener listener) {
            tvId.setText("Sale #" + transaction.getId());

            tvDate.setText(formatDate(transaction.getTimestamp()));

            tvTotal.setText(String.format(Locale.US, "₱%.2f", transaction.getTotalAmount()));

            itemView.setOnClickListener(v -> listener.onItemClick(transaction));
        }

        private String formatDate(String dbDate) {
            if (dbDate == null) return "";
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(dbDate);

                if (date != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.US);
                    return outputFormat.format(date);
                }
            } catch (ParseException e) {
            }
            return dbDate;
        }
    }
}