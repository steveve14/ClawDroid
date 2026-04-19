package com.clawdroid.feature.voice.adapter;

import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.feature.voice.viewmodel.VoiceChatViewModel;

import java.util.ArrayList;
import java.util.List;

public class VoiceLogAdapter extends RecyclerView.Adapter<VoiceLogAdapter.ViewHolder> {

    private List<VoiceChatViewModel.VoiceLogEntry> items = new ArrayList<>();

    public void submitList(List<VoiceChatViewModel.VoiceLogEntry> newItems) {
        this.items = newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout layout = new LinearLayout(parent.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 8, 24, 8);
        layout.setLayoutParams(new RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout container;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            container = (LinearLayout) itemView;
        }

        void bind(VoiceChatViewModel.VoiceLogEntry entry) {
            container.removeAllViews();

            boolean isUser = "user".equals(entry.role);
            container.setGravity(isUser ? Gravity.END : Gravity.START);

            TextView tvText = new TextView(container.getContext());
            tvText.setText(entry.text);
            tvText.setTextSize(14);
            tvText.setPadding(16, 12, 16, 12);
            tvText.setTextColor(isUser ? 0xFFFFFFFF : 0xFF000000);
            tvText.setBackgroundColor(isUser ? 0xFF6200EE : 0xFFE0E0E0);

            TextView tvTime = new TextView(container.getContext());
            tvTime.setText(DateUtils.getRelativeTimeSpanString(
                    entry.timestamp, System.currentTimeMillis(),
                    DateUtils.SECOND_IN_MILLIS));
            tvTime.setTextSize(10);
            tvTime.setTextColor(0xFF999999);

            container.addView(tvText);
            container.addView(tvTime);
        }
    }
}
