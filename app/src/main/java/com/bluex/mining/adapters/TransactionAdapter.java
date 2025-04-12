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

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
    private List<Withdrawal> transactions = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Withdrawal withdrawal = transactions.get(position);
        holder.userIdText.setText("User ID: " + withdrawal.getUserId());
        holder.amountText.setText("Amount: " + withdrawal.getAmount());
        holder.statusText.setText("Status: " + withdrawal.getStatus());
        holder.mobileText.setText("Mobile: " + withdrawal.getMobileNumber());
        holder.dateText.setText(dateFormat.format(new Date(withdrawal.getTimestamp())));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void setTransactions(List<Withdrawal> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amountText;
        TextView userIdText;
        TextView statusText;
        TextView mobileText;
        TextView dateText;

        ViewHolder(View itemView) {
            super(itemView);
            amountText = itemView.findViewById(R.id.amountText);
            userIdText = itemView.findViewById(R.id.userIdText);
            statusText = itemView.findViewById(R.id.statusText);
            mobileText = itemView.findViewById(R.id.mobileText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
} 