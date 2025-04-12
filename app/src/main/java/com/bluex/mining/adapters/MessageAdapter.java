package com.bluex.mining.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluex.mining.R;
import com.bluex.mining.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private final List<Message> messages;
    private final SimpleDateFormat dateFormat;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.titleText.setText(message.getTitle());
        holder.contentText.setText(message.getContent());
        holder.timeText.setText(dateFormat.format(new Date(message.getTimestamp())));
        
        // Set unread indicator
        holder.unreadIndicator.setVisibility(message.isRead() ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView contentText;
        TextView timeText;
        View unreadIndicator;

        MessageViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.messageTitle);
            contentText = itemView.findViewById(R.id.messageContent);
            timeText = itemView.findViewById(R.id.messageTime);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
        }
    }
} 