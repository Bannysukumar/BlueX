package com.bluex.mining.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bluex.mining.R;
import com.bluex.mining.models.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.TeamViewHolder> {
    private List<User> teamMembers = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_team_member, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        User member = teamMembers.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return teamMembers.size();
    }

    public void setTeamMembers(List<User> members) {
        this.teamMembers = members;
        notifyDataSetChanged();
    }

    class TeamViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameText;
        private final TextView miningRateText;
        private final TextView joinDateText;
        private final TextView bonusText;

        TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.usernameText);
            miningRateText = itemView.findViewById(R.id.miningRateText);
            joinDateText = itemView.findViewById(R.id.joinDateText);
            bonusText = itemView.findViewById(R.id.bonusText);
        }

        void bind(User member) {
            usernameText.setText(member.getUsername());
            miningRateText.setText(String.format(Locale.getDefault(),
                "Mining Rate: %.3f BXC/sec", member.getMiningRate()));
            joinDateText.setText(String.format("Joined: %s",
                dateFormat.format(new Date(member.getJoinDate()))));
            bonusText.setText(String.format(Locale.getDefault(),
                "+%.2f BXC", member.getReferralBonus()));
        }
    }
} 