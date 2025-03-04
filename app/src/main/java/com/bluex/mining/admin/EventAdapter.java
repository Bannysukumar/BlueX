package com.bluex.mining.admin;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bluex.mining.R;
import com.bluex.mining.models.MiningEvent;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private List<MiningEvent> events = new ArrayList<>();
    private final EventClickListener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            notifyDataSetChanged();
            handler.postDelayed(this, 1000); // Update every second
        }
    };

    public EventAdapter(EventClickListener listener) {
        this.listener = listener;
        handler.post(updateTimeRunnable);
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        MiningEvent event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void setEvents(List<MiningEvent> events) {
        this.events = events;
        notifyDataSetChanged();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView eventNameText;
        private final TextView multiplierText;
        private final TextView timeRemainingText;
        private final Chip statusChip;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            eventNameText = itemView.findViewById(R.id.eventNameText);
            multiplierText = itemView.findViewById(R.id.multiplierText);
            timeRemainingText = itemView.findViewById(R.id.timeRemainingText);
            statusChip = itemView.findViewById(R.id.statusChip);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEventClick(events.get(position));
                }
            });
        }

        void bind(MiningEvent event) {
            eventNameText.setText(event.getName());
            multiplierText.setText(String.format(Locale.getDefault(), 
                "Multiplier: %.2fx", event.getMultiplier()));

            long remainingTime = event.getRemainingTime();
            if (remainingTime > 0) {
                long hours = TimeUnit.MILLISECONDS.toHours(remainingTime);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(remainingTime) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(remainingTime) % 60;
                
                timeRemainingText.setText(String.format(Locale.getDefault(),
                    "Time Remaining: %02d:%02d:%02d", hours, minutes, seconds));
                statusChip.setText("Active");
                statusChip.setChipBackgroundColorResource(R.color.green);
            } else {
                timeRemainingText.setText("Event Ended");
                statusChip.setText("Ended");
                statusChip.setChipBackgroundColorResource(R.color.gray);
            }
        }
    }

    interface EventClickListener {
        void onEventClick(MiningEvent event);
    }

    public void onDestroy() {
        handler.removeCallbacks(updateTimeRunnable);
    }
} 