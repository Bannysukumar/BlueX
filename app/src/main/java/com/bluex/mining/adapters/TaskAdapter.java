package com.bluex.mining.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bluex.mining.R;
import com.bluex.mining.models.Task;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
    private List<Task> tasks;
    private TaskClickListener listener;

    public TaskAdapter(List<Task> tasks, TaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.titleText.setText(task.getTitle());
        holder.descriptionText.setText(task.getDescription());
        holder.rewardText.setText(task.getReward() + " BXC");
        
        if (task.isCompleted()) {
            holder.verifyButton.setEnabled(false);
            holder.verifyButton.setText("Completed");
        } else {
            holder.verifyButton.setEnabled(true);
            holder.verifyButton.setText("Verify");
            holder.verifyButton.setOnClickListener(v -> listener.onVerifyTask(task));
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView descriptionText;
        TextView rewardText;
        Button verifyButton;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            rewardText = itemView.findViewById(R.id.rewardText);
            verifyButton = itemView.findViewById(R.id.verifyButton);
        }
    }

    public interface TaskClickListener {
        void onVerifyTask(Task task);
    }
} 