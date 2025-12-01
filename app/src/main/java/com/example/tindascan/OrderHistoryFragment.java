package com.example.tindascan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class OrderHistoryFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrderHistoryAdapter adapter;
    private DatabaseHelper dbHelper;
    private TextView tvOrderCount;
    private ImageButton btnBack;

    public OrderHistoryFragment() {
        super(R.layout.fragment_order_history);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        // Initialize Views
        recyclerView = view.findViewById(R.id.rv_order_history);
        tvOrderCount = view.findViewById(R.id.tv_order_count);
        btnBack = view.findViewById(R.id.btn_back);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new OrderHistoryAdapter(new ArrayList<>(), this::onTransactionClicked);
        recyclerView.setAdapter(adapter);

        // Load Data
        loadOrderHistory();

        // Back Button Logic
        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
    }

    private void loadOrderHistory() {
        List<Transaction> transactions = dbHelper.getAllTransactions();

        if (transactions.isEmpty()) {
            tvOrderCount.setText("No orders yet");
        } else {
            tvOrderCount.setText(transactions.size() + " Orders");
        }

        adapter.updateList(transactions);
    }

    private void onTransactionClicked(Transaction transaction) {
        // TODO: Navigate to Order Details if needed
        Toast.makeText(getContext(), "Selected Order #" + transaction.getId(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrderHistory();
    }
}