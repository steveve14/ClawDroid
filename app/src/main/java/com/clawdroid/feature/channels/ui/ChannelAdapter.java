package com.clawdroid.feature.channels.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.core.data.db.entity.ChannelEntity;
import com.clawdroid.app.databinding.ItemChannelBinding;

public class ChannelAdapter extends ListAdapter<ChannelEntity, ChannelAdapter.ViewHolder> {

    public interface OnChannelClickListener {
        void onChannelClick(ChannelEntity channel);
    }

    private final OnChannelClickListener listener;

    public ChannelAdapter(OnChannelClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChannelBinding binding = ItemChannelBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemChannelBinding binding;

        ViewHolder(ItemChannelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChannelEntity channel) {
            binding.tvChannelName.setText(channel.getName());
            binding.tvChannelIcon.setText(getIcon(channel.getType()));

            String statusText;
            int statusColor;
            switch (channel.getStatus()) {
                case "connected":
                    statusText = "연결됨";
                    statusColor = 0xFF4CAF50;
                    break;
                case "error":
                    statusText = "오류";
                    statusColor = 0xFFF44336;
                    break;
                default:
                    statusText = "연결 해제";
                    statusColor = 0xFF9E9E9E;
                    break;
            }
            binding.tvStatus.setText(statusText);
            binding.statusDot.getBackground().setTint(statusColor);

            itemView.setOnClickListener(v -> listener.onChannelClick(channel));
        }

        private String getIcon(String type) {
            switch (type) {
                case "telegram": return "✈️";
                case "discord": return "🎮";
                case "slack": return "💼";
                case "gateway": return "🌐";
                default: return "📡";
            }
        }
    }

    private static final DiffUtil.ItemCallback<ChannelEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChannelEntity o, @NonNull ChannelEntity n) {
                    return o.getId().equals(n.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChannelEntity o, @NonNull ChannelEntity n) {
                    return o.getName().equals(n.getName())
                            && o.getStatus().equals(n.getStatus())
                            && o.getType().equals(n.getType());
                }
            };
}
