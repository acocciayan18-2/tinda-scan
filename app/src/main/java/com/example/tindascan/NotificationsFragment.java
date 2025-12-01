package com.example.tindascan;

import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private DatabaseHelper dbHelper;
    private TextView tvEmpty;

    public NotificationsFragment() {
        super(R.layout.fragment_notifications); // Ensure layout exists (code below)
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DatabaseHelper(requireContext());

        ImageButton btnBack = view.findViewById(R.id.btn_back_notif);
        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        tvEmpty = view.findViewById(R.id.tv_empty_notif);
        recyclerView = view.findViewById(R.id.rv_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Load Data
        List<AppNotification> list = dbHelper.getAllNotifications();
        adapter = new NotificationAdapter(requireContext(), list);
        recyclerView.setAdapter(adapter);

        updateEmptyState(list);

        // Mark all as read since we opened the screen
        dbHelper.markAllNotificationsAsRead();

        // 🔥 SWIPE TO DELETE IMPLEMENTATION
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv, @NonNull RecyclerView.ViewHolder vh, @NonNull RecyclerView.ViewHolder target) {
                return false; // We don't support drag-drop reordering
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                AppNotification notif = adapter.getItem(position);

                // Delete from DB
                dbHelper.deleteNotification(notif.getId());

                // Remove from UI
                adapter.removeItem(position);

                // Show Undo Option
                Snackbar.make(recyclerView, "Notification deleted", Snackbar.LENGTH_LONG).show();

                // Check empty state again
                if (adapter.getItemCount() == 0) {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void updateEmptyState(List<AppNotification> list) {
        if (list.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}