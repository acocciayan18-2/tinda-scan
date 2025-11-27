package com.example.tindascan;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class OrderDetailsFragment extends Fragment {

    private RecyclerView rvDetails;
    private DatabaseHelper dbHelper;
    private OrderDetailsAdapter adapter;
    private TextView tvTitle, tvTotal;
    private ImageButton btnBack;

    public OrderDetailsFragment() {
        super(R.layout.fragment_order_details);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());
        rvDetails = view.findViewById(R.id.rv_order_details);
        tvTitle = view.findViewById(R.id.tv_details_title);
        tvTotal = view.findViewById(R.id.tv_details_total); // Already found here
        btnBack = view.findViewById(R.id.btn_back_details);

        rvDetails.setLayoutManager(new LinearLayoutManager(getContext()));

        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        // Get the arguments
        if (getArguments() != null) {
            long transactionId = getArguments().getLong("transactionId");

            // ✅ FIX: Use getFloat to match the sending type
            float totalAmount = getArguments().getFloat("totalAmount");

            tvTitle.setText("Sale #" + transactionId);
            tvTotal.setText(String.format(Locale.US, "₱%.2f", totalAmount)); // Use the float here

            loadDetails(transactionId);
        }
    }

    private void loadDetails(long transactionId) {
        List<TransactionDetail> details = dbHelper.getTransactionDetails(transactionId);
        adapter = new OrderDetailsAdapter(details);
        rvDetails.setAdapter(adapter);
    }
}