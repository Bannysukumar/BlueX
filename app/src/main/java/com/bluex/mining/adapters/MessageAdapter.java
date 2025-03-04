package com.bluex.mining.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bluex.mining.R;
import com.bluex.mining.models.AdminMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<AdminMessage> messages = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminMessage message = messages.get(position);
        holder.titleText.setText(message.getTitle());
        holder.messageText.setText(message.getMessage());
        holder.dateText.setText(dateFormat.format(new Date(message.getTimestamp())));

        if (message.isImportant()) {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.getContext().getResources().getColor(R.color.light_red));
            holder.titleText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_warning, 0, 0, 0);
        } else {
            holder.cardView.setCardBackgroundColor(
                holder.itemView.getContext().getResources().getColor(R.color.white));
            holder.titleText.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ic_info, 0, 0, 0);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public void setMessages(List<AdminMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView titleText;
        TextView messageText;
        TextView dateText;

        ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            titleText = itemView.findViewById(R.id.titleText);
            messageText = itemView.findViewById(R.id.messageText);
            dateText = itemView.findViewById(R.id.dateText);
        }
    }
} 