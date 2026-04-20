package com.clawdroid.feature.chat.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.app.R;
import com.google.android.material.chip.Chip;

import java.time.Instant;

public class ConversationAdapter extends ListAdapter<ConversationEntity, ConversationAdapter.ViewHolder> {

    public interface OnConversationClickListener {
        void onClick(ConversationEntity conversation);
        void onLongClick(ConversationEntity conversation);
        void onDeleteClick(ConversationEntity conversation);
        void onRenameClick(ConversationEntity conversation);
    }

    private OnConversationClickListener listener;

    public ConversationAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOnClickListener(OnConversationClickListener listener) {
        this.listener = listener;
    }

    @Nullable
    public ConversationEntity getItemAt(int position) {
        if (position >= 0 && position < getCurrentList().size()) {
            return getItem(position);
        }
        return null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationEntity item = getItem(position);
        holder.bind(item);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View pinIndicator;
        private final TextView tvTitle;
        private final TextView tvTime;
        private final TextView tvPreview;
        private final Chip chipModel;
        private final Chip chipChannel;
        private final ImageButton btnDelete;
        private final ImageButton btnRename;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            pinIndicator = itemView.findViewById(R.id.pinIndicator);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            chipModel = itemView.findViewById(R.id.chipModel);
            chipChannel = itemView.findViewById(R.id.chipChannel);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnRename = itemView.findViewById(R.id.btnRename);
        }

        void bind(ConversationEntity item) {
            tvTitle.setText(item.getTitle() != null ? item.getTitle() : "새 대화");
            tvPreview.setText(item.getLastMessagePreview() != null
                    ? item.getLastMessagePreview() : "");

            // Pin indicator
            if (item.getIsPinned() == 1) {
                pinIndicator.setVisibility(View.VISIBLE);
            } else {
                pinIndicator.setVisibility(View.GONE);
            }

            // Channel chip
            if (item.getChannelId() != null) {
                chipChannel.setVisibility(View.VISIBLE);
                chipChannel.setText("📡 채널");
            } else {
                chipChannel.setVisibility(View.GONE);
            }

            // Model chip
            if (item.getModelProvider() != null) {
                chipModel.setText(item.getModelProvider());
                chipModel.setVisibility(View.VISIBLE);
            } else {
                chipModel.setVisibility(View.GONE);
            }

            // Time
            try {
                long timestamp = Instant.parse(item.getUpdatedAt()).toEpochMilli();
                tvTime.setText(DateUtils.getRelativeTimeSpanString(
                        timestamp, System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS));
            } catch (Exception e) {
                tvTime.setText("");
            }

            // Click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onLongClick(item);
                return true;
            });
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(item);
            });
            btnRename.setOnClickListener(v -> {
                if (listener != null) listener.onRenameClick(item);
            });
        }
    }

    private static final DiffUtil.ItemCallback<ConversationEntity> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ConversationEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ConversationEntity oldItem,
                                                @NonNull ConversationEntity newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ConversationEntity oldItem,
                                                   @NonNull ConversationEntity newItem) {
                    return oldItem.getUpdatedAt().equals(newItem.getUpdatedAt())
                            && oldItem.getIsPinned() == newItem.getIsPinned();
                }
            };
}
