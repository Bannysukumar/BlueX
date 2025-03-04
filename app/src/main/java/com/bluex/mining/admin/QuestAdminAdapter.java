package com.bluex.mining.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bluex.mining.R;
import com.bluex.mining.models.Quest;

import java.util.ArrayList;
import java.util.List;

public class QuestAdminAdapter extends RecyclerView.Adapter<QuestAdminAdapter.QuestViewHolder> {
    private List<Quest> quests = new ArrayList<>();
    private QuestActionListener listener;

    public interface QuestActionListener {
        void onEditQuest(Quest quest);
        void onDeleteQuest(Quest quest);
    }

    public QuestAdminAdapter(QuestActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public QuestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quest_admin, parent, false);
        return new QuestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestViewHolder holder, int position) {
        Quest quest = quests.get(position);
        
        holder.titleText.setText(quest.getTitle());
        holder.rewardText.setText(String.format("%.1f $bxc", quest.getReward()));

        // Set platform icon
        int iconRes = getPlatformIcon(quest.getPlatform());
        holder.platformIcon.setImageResource(iconRes);

        holder.editButton.setOnClickListener(v -> listener.onEditQuest(quest));
        holder.deleteButton.setOnClickListener(v -> listener.onDeleteQuest(quest));
    }

    private int getPlatformIcon(String platform) {
        switch (platform.toLowerCase()) {
            case "twitter":
                return R.drawable.ic_twitter;
            case "youtube":
                return R.drawable.ic_youtube;
            default:
                return R.drawable.ic_link;
        }
    }

    @Override
    public int getItemCount() {
        return quests.size();
    }

    public void setQuests(List<Quest> quests) {
        this.quests = quests;
        notifyDataSetChanged();
    }

    static class QuestViewHolder extends RecyclerView.ViewHolder {
        ImageView platformIcon;
        TextView titleText;
        TextView rewardText;
        ImageButton editButton;
        ImageButton deleteButton;

        QuestViewHolder(View itemView) {
            super(itemView);
            platformIcon = itemView.findViewById(R.id.platformIcon);
            titleText = itemView.findViewById(R.id.titleText);
            rewardText = itemView.findViewById(R.id.rewardText);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
} 