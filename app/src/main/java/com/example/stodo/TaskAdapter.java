package com.example.stodo;

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

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> tasks;
    private OnTaskClickListener listener;

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

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textViewTaskTitle);
            checkBox = itemView.findViewById(R.id.checkBoxTask);
            buttonMore = itemView.findViewById(R.id.buttonMore);
        }
    }
}