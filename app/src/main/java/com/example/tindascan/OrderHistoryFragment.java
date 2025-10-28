package com.example.tindascan;

import android.os.Bundle;
import android.view.View;
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

        dbHelper = new DatabaseHelper(requireContext());
        rvHistory = view.findViewById(R.id.rv_order_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_order_history);
        toolbar.setNavigationOnClickListener(v -> {
            NavHostFragment.findNavController(this).popBackStack();
        });

        loadHistory();
    }

    private void loadHistory() {
        List<Transaction> transactions = dbHelper.getAllTransactions();
        adapter = new OrderHistoryAdapter(transactions, this);
        rvHistory.setAdapter(adapter);
    }

    @Override
    public void onItemClick(Transaction transaction) {
        // Navigate to details screen, passing the transaction ID
        Bundle args = new Bundle();
        args.putLong("transactionId", transaction.getId());
        // ✅ FIX: Send as a float to match nav_graph.xml
        args.putFloat("totalAmount", (float) transaction.getTotalAmount());

        NavHostFragment.findNavController(this)
                .navigate(R.id.action_nav_history_to_nav_details, args);
    }
}