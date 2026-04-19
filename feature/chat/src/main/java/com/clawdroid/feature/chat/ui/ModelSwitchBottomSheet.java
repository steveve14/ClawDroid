package com.clawdroid.feature.chat.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.feature.chat.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class ModelSwitchBottomSheet extends BottomSheetDialogFragment {

    public static class ModelOption {
        public final String providerId;
        public final String providerName;
        public final String modelId;
        public final String modelName;

        public ModelOption(String providerId, String providerName, String modelId, String modelName) {
            this.providerId = providerId;
            this.providerName = providerName;
            this.modelId = modelId;
            this.modelName = modelName;
        }
    }

    public interface OnModelSelectedListener {
        void onModelSelected(String providerId, String modelId);
    }

    private OnModelSelectedListener listener;
    private List<ModelOption> models;
    private String currentModelId;

    public static ModelSwitchBottomSheet newInstance() {
        return new ModelSwitchBottomSheet();
    }

    public void setModels(List<ModelOption> models) {
        this.models = models;
    }

    public void setCurrentModelId(String currentModelId) {
        this.currentModelId = currentModelId;
    }

    public void setOnModelSelectedListener(OnModelSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_model_switch, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recycler = view.findViewById(R.id.recyclerModels);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        ModelAdapter adapter = new ModelAdapter();
        recycler.setAdapter(adapter);

        if (models != null) {
            adapter.submitList(models);
        }
    }

    private class ModelAdapter extends ListAdapter<ModelOption, ModelAdapter.VH> {

        ModelAdapter() {
            super(new DiffUtil.ItemCallback<ModelOption>() {
                @Override
                public boolean areItemsTheSame(@NonNull ModelOption a, @NonNull ModelOption b) {
                    return a.modelId.equals(b.modelId) && a.providerId.equals(b.providerId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull ModelOption a, @NonNull ModelOption b) {
                    return a.modelId.equals(b.modelId);
                }
            });
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_model_option, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            ModelOption item = getItem(position);
            holder.tvModelName.setText(item.modelName);
            holder.tvProviderName.setText(item.providerName);
            holder.ivSelected.setVisibility(
                    item.modelId.equals(currentModelId) ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onModelSelected(item.providerId, item.modelId);
                }
                dismiss();
            });
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvModelName;
            final TextView tvProviderName;
            final ImageView ivSelected;

            VH(@NonNull View itemView) {
                super(itemView);
                tvModelName = itemView.findViewById(R.id.tvModelName);
                tvProviderName = itemView.findViewById(R.id.tvProviderName);
                ivSelected = itemView.findViewById(R.id.ivSelected);
            }
        }
    }
}
