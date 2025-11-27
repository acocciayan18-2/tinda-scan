package com.example.tindascan;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class OrderHistoryFragment extends Fragment implements OrderHistoryAdapter.OnHistoryItemClickListener {

    private RecyclerView rvHistory;
    private DatabaseHelper dbHelper;
    private OrderHistoryAdapter adapter;

    public OrderHistoryFragment() {
        super(R.layout.fragment_order_history);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize RecyclerView
        rvHistory = view.findViewById(R.id.rv_order_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        // Initialize DatabaseHelper
        dbHelper = new DatabaseHelper(requireContext());

        // Toolbar title is fine, optional reference
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_order_history);

        // Back button inside toolbar
        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        // Load history
        loadHistory();
    }

    private void loadHistory() {
        List<Transaction> transactions = dbHelper.getAllTransactions();
        adapter = new OrderHistoryAdapter(transactions, this);
        rvHistory.setAdapter(adapter);
    }

    @Override
    public void onItemClick(Transaction transaction) {
        Bundle args = new Bundle();
        args.putLong("transactionId", transaction.getId());
        args.putFloat("totalAmount", (float) transaction.getTotalAmount());
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_nav_history_to_nav_details, args);
    }
}
