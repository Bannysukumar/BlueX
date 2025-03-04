package com.bluex.mining.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluex.mining.R;
import com.bluex.mining.models.Withdrawal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WithdrawalAdapter extends RecyclerView.Adapter<WithdrawalAdapter.ViewHolder> {
    private List<Withdrawal> withdrawals = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private WithdrawalActionListener actionListener;
    private boolean isAdminMode = false;

    public WithdrawalAdapter() {
        this.isAdminMode = false;
    }

    public WithdrawalAdapter(WithdrawalActionListener listener) {
        this.actionListener = listener;
        this.isAdminMode = true;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_withdrawal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Withdrawal withdrawal = withdrawals.get(position);
        holder.amountText.setText(String.format(Locale.US, "%.5f BXC", withdrawal.getAmount()));
        holder.dateText.setText(dateFormat.format(new Date(withdrawal.getTimestamp())));
        holder.statusText.setText(formatStatus(withdrawal.getStatus()));
        
        // Set status color
        int statusColor;
        switch (withdrawal.getStatus().toLowerCase()) {
            case "completed":
                statusColor = holder.itemView.getContext().getColor(android.R.color.holo_green_dark);
                break;
            case "processing":
                statusColor = holder.itemView.getContext().getColor(android.R.color.holo_orange_dark);
                break;
            case "failed":
                statusColor = holder.itemView.getContext().getColor(android.R.color.holo_red_dark);
                break;
            default:
                statusColor = holder.itemView.getContext().getColor(android.R.color.darker_gray);
        }
        holder.statusText.setTextColor(statusColor);

        // Setup click listeners for admin mode
        if (isAdminMode && actionListener != null) {
            holder.itemView.setOnClickListener(v -> actionListener.onWithdrawalClick(withdrawal));
            holder.itemView.setOnLongClickListener(v -> {
                actionListener.onWithdrawalLongClick(withdrawal);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return withdrawals.size();
    }

    public void setWithdrawals(List<Withdrawal> withdrawals) {
        this.withdrawals = withdrawals;
        notifyDataSetChanged();
    }

    private String formatStatus(String status) {
        if (status == null || status.isEmpty()) return "Pending";
        return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amountText;
        TextView dateText;
        TextView statusText;
        TextView userIdText; // For admin mode

        ViewHolder(View itemView) {
            super(itemView);
            amountText = itemView.findViewById(R.id.amountText);
            dateText = itemView.findViewById(R.id.dateText);
            statusText = itemView.findViewById(R.id.statusText);
            userIdText = itemView.findViewById(R.id.userIdText); // May be null in non-admin mode
        }
    }

    public interface WithdrawalActionListener {
        void onWithdrawalClick(Withdrawal withdrawal);
        void onWithdrawalLongClick(Withdrawal withdrawal);
        void onStatusChange(Withdrawal withdrawal, String newStatus);
    }

    // Admin helper methods
    public void updateWithdrawalStatus(Withdrawal withdrawal, String newStatus) {
        int position = withdrawals.indexOf(withdrawal);
        if (position != -1) {
            withdrawal.setStatus(newStatus);
            notifyItemChanged(position);
            if (actionListener != null) {
                actionListener.onStatusChange(withdrawal, newStatus);
            }
        }
    }

    public void removeWithdrawal(Withdrawal withdrawal) {
        int position = withdrawals.indexOf(withdrawal);
        if (position != -1) {
            withdrawals.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void addWithdrawal(Withdrawal withdrawal) {
        withdrawals.add(0, withdrawal);
        notifyItemInserted(0);
    }

    public void updateWithdrawal(Withdrawal withdrawal) {
        int position = withdrawals.indexOf(withdrawal);
        if (position != -1) {
            withdrawals.set(position, withdrawal);
            notifyItemChanged(position);
        }
    }
} 