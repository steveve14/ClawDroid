package com.clawdroid.feature.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.core.data.db.entity.MessageEntity;
import com.clawdroid.feature.chat.R;
import com.google.android.material.chip.Chip;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class MessageAdapter extends ListAdapter<MessageAdapter.MessageItem, MessageAdapter.ViewHolder> {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    public MessageAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageItem item = getItem(position);
        holder.bind(item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout userBubble;
        private final LinearLayout aiBubble;
        private final TextView tvUserContent;
        private final TextView tvUserTime;
        private final TextView tvAiContent;
        private final TextView tvAiMeta;
        private final Chip chipToolCall;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            userBubble = itemView.findViewById(R.id.userBubble);
            aiBubble = itemView.findViewById(R.id.aiBubble);
            tvUserContent = itemView.findViewById(R.id.tvUserContent);
            tvUserTime = itemView.findViewById(R.id.tvUserTime);
            tvAiContent = itemView.findViewById(R.id.tvAiContent);
            tvAiMeta = itemView.findViewById(R.id.tvAiMeta);
            chipToolCall = itemView.findViewById(R.id.chipToolCall);
        }

        void bind(MessageItem item) {
            if (item.isStreaming) {
                // Streaming AI response
                userBubble.setVisibility(View.GONE);
                aiBubble.setVisibility(View.VISIBLE);
                tvAiContent.setText(item.streamingContent);
                tvAiMeta.setText("생성 중...");
                chipToolCall.setVisibility(View.GONE);
                return;
            }

            MessageEntity msg = item.entity;
            if (msg == null) return;

            boolean isUser = "user".equals(msg.getRole());

            userBubble.setVisibility(isUser ? View.VISIBLE : View.GONE);
            aiBubble.setVisibility(isUser ? View.GONE : View.VISIBLE);

            if (isUser) {
                tvUserContent.setText(msg.getContent());
                try {
                    tvUserTime.setText(TIME_FORMAT.format(Instant.parse(msg.getCreatedAt())));
                } catch (Exception e) {
                    tvUserTime.setText("");
                }
            } else {
                tvAiContent.setText(msg.getContent());
                // Meta info
                StringBuilder meta = new StringBuilder();
                if (msg.getDurationMs() != null) {
                    meta.append("⏱️ ").append(String.format("%.1fs", msg.getDurationMs() / 1000.0));
                }
                if (msg.getOutputTokens() != null) {
                    if (meta.length() > 0) meta.append(" · ");
                    meta.append(msg.getOutputTokens()).append(" tokens");
                }
                tvAiMeta.setText(meta.toString());
                tvAiMeta.setVisibility(meta.length() > 0 ? View.VISIBLE : View.GONE);
                chipToolCall.setVisibility(View.GONE);
            }
        }
    }

    public static class MessageItem {
        @Nullable public final MessageEntity entity;
        public final boolean isStreaming;
        @Nullable public final String streamingContent;

        public MessageItem(@NonNull MessageEntity entity) {
            this.entity = entity;
            this.isStreaming = false;
            this.streamingContent = null;
        }

        public MessageItem(@NonNull String streamingContent) {
            this.entity = null;
            this.isStreaming = true;
            this.streamingContent = streamingContent;
        }

        public String getId() {
            return isStreaming ? "__streaming__" : (entity != null ? entity.getId() : "");
        }
    }

    private static final DiffUtil.ItemCallback<MessageItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MessageItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull MessageItem o, @NonNull MessageItem n) {
                    return o.getId().equals(n.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MessageItem o, @NonNull MessageItem n) {
                    if (o.isStreaming && n.isStreaming) {
                        return o.streamingContent != null && o.streamingContent.equals(n.streamingContent);
                    }
                    if (o.entity != null && n.entity != null) {
                        return o.entity.getContent().equals(n.entity.getContent());
                    }
                    return false;
                }
            };
}
