package com.example.stodo;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private OnTaskClickListener listener;
    private Handler countdownHandler = new Handler(Looper.getMainLooper());
    private Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            notifyDataSetChanged();
            countdownHandler.postDelayed(this, 1000);
        }
    };

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskStatusChanged(Task task);
        void onTaskEdit(Task task);
        void onTaskDelete(Task task);
    }

    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    public void startCountdown() {
        countdownHandler.removeCallbacks(countdownRunnable);
        countdownHandler.post(countdownRunnable);
    }

    public void stopCountdown() {
        countdownHandler.removeCallbacks(countdownRunnable);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.title.setText(task.getTitle());
        holder.checkBox.setChecked(task.isCompleted());

        // Countdown logic
        if (task.isCompleted() && task.getUncheckTimestamp() > 0) {
            long remainingMillis = task.getUncheckTimestamp() - System.currentTimeMillis();
            if (remainingMillis > 0) {
                long totalSeconds = remainingMillis / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                holder.countdown.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                holder.countdown.setVisibility(View.VISIBLE);
            } else {
                holder.countdown.setVisibility(View.GONE);
            }
        } else {
            holder.countdown.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
        holder.checkBox.setOnClickListener(v -> {
            task.setCompleted(holder.checkBox.isChecked());
            listener.onTaskStatusChanged(task);
        });

        holder.buttonMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.item_task_menu);
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_edit) {
                    listener.onTaskEdit(task);
                    return true;
                } else if (itemId == R.id.action_delete) {
                    listener.onTaskDelete(task);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        CheckBox checkBox;
        ImageButton buttonMore;
        TextView countdown;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewTaskTitle);
            checkBox = itemView.findViewById(R.id.checkBoxTask);
            buttonMore = itemView.findViewById(R.id.buttonMore);
            countdown = itemView.findViewById(R.id.textViewCountdown);
        }
    }
}