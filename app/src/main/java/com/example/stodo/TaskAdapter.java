package com.example.stodo;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stodo.service.TaskService;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private OnTaskClickListener listener;
    private TaskService taskService;
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

    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener, TaskService taskService) {
        this.tasks = tasks;
        this.listener = listener;
        this.taskService = taskService;
    }

    public void startCountdown() {
        countdownHandler.removeCallbacks(countdownRunnable);
        countdownHandler.post(countdownRunnable);
    }

    public void stopCountdown() {
        countdownHandler.removeCallbacks(countdownRunnable);
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(tasks, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(tasks, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        
        // Update positions in database
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            t.setPosition(i);
            taskService.updateTask(t);
        }
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

        bindCountdown(holder.countdown, task);
        bindCompletionCount(holder.completionCount, holder.title, task);

        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
        holder.checkBox.setOnClickListener(v -> {
            task.setCompleted(holder.checkBox.isChecked());
            listener.onTaskStatusChanged(task);
        });

        holder.buttonMore.setOnClickListener(v -> showPopupMenu(v, task));
    }

    private void bindCountdown(TextView textView, Task task) {
        if (task.isCompleted() && task.getUncheckTimestamp() > 0) {
            long remainingMillis = task.getUncheckTimestamp() - System.currentTimeMillis();
            if (remainingMillis > 0) {
                long totalSeconds = remainingMillis / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                textView.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
                textView.setVisibility(View.VISIBLE);
                return;
            }
        }
        textView.setVisibility(View.GONE);
    }

    private void bindCompletionCount(TextView countView, TextView titleView, Task task) {
        int count = task.getCompletionCount();
        if (count > 0) {
            countView.setText(String.format(Locale.getDefault(), "%dx", count));
            countView.setVisibility(View.VISIBLE);
        } else {
            countView.setVisibility(View.GONE);
        }
        applyTaskStyle(titleView, count);
    }

    private void applyTaskStyle(TextView textView, int count) {
        if (count >= 10) {
            textView.setTypeface(null, Typeface.BOLD_ITALIC);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        } else if (count >= 5) {
            textView.setTypeface(null, Typeface.BOLD);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        } else {
            textView.setTypeface(null, Typeface.NORMAL);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        }
    }

    private void showPopupMenu(View v, Task task) {
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
        TextView completionCount;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewTaskTitle);
            checkBox = itemView.findViewById(R.id.checkBoxTask);
            buttonMore = itemView.findViewById(R.id.buttonMore);
            countdown = itemView.findViewById(R.id.textViewCountdown);
            completionCount = itemView.findViewById(R.id.textViewCompletionCount);
        }
    }
}