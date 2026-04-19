package com.clawdroid.feature.chat.ui;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.feature.chat.R;
import com.clawdroid.feature.chat.adapter.ConversationAdapter;
import com.clawdroid.feature.chat.databinding.FragmentConversationListBinding;
import com.clawdroid.feature.chat.viewmodel.ConversationListViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ConversationListFragment extends Fragment {

    private FragmentConversationListBinding binding;
    private ConversationListViewModel viewModel;
    private ConversationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConversationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ConversationListViewModel.class);

        setupRecyclerView();
        setupSwipeActions();
        setupObservers();
        setupActions();
    }

    private void setupRecyclerView() {
        adapter = new ConversationAdapter();
        binding.recyclerConversations.setAdapter(adapter);

        adapter.setOnClickListener(new ConversationAdapter.OnConversationClickListener() {
            @Override
            public void onClick(ConversationEntity conversation) {
                Bundle args = new Bundle();
                args.putString("conversationId", conversation.getId());
                Navigation.findNavController(requireView())
                        .navigate(com.clawdroid.core.ui.R.id.action_conversationList_to_chat, args);
            }

            @Override
            public void onLongClick(ConversationEntity conversation) {
                showContextMenu(conversation);
            }
        });
    }

    private void setupSwipeActions() {
        ItemTouchHelper touchHelper = new ItemTouchHelper(
                new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                ConversationEntity item = adapter.getItemAt(pos);
                if (item == null) return;

                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.archiveConversation(item.getId());
                    com.google.android.material.snackbar.Snackbar
                            .make(binding.getRoot(), "대화가 보관되었습니다", 3000)
                            .setAction("취소", v -> viewModel.unarchiveConversation(item.getId()))
                            .show();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("대화 삭제")
                            .setMessage("이 대화를 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (d, w) ->
                                    viewModel.deleteConversation(item.getId()))
                            .setNegativeButton("취소", (d, w) ->
                                    adapter.notifyItemChanged(pos))
                            .setOnCancelListener(d ->
                                    adapter.notifyItemChanged(pos))
                            .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                View itemView = viewHolder.itemView;
                Paint paint = new Paint();

                if (dX < 0) {
                    paint.setColor(Color.parseColor("#F59E0B"));
                    RectF bg = new RectF(itemView.getRight() + dX, itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                    c.drawRect(bg, paint);
                } else if (dX > 0) {
                    paint.setColor(Color.parseColor("#EF4444"));
                    RectF bg = new RectF(itemView.getLeft(), itemView.getTop(),
                            itemView.getLeft() + dX, itemView.getBottom());
                    c.drawRect(bg, paint);
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        touchHelper.attachToRecyclerView(binding.recyclerConversations);
    }

    private void showContextMenu(ConversationEntity conversation) {
        String[] items = conversation.getIsPinned() == 1
                ? new String[]{"고정 해제", "보관", "삭제"}
                : new String[]{"고정", "보관", "삭제"};

        new AlertDialog.Builder(requireContext())
                .setTitle(conversation.getTitle() != null ? conversation.getTitle() : "대화")
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            viewModel.togglePin(conversation.getId(), conversation.getIsPinned() == 0);
                            break;
                        case 1:
                            viewModel.archiveConversation(conversation.getId());
                            break;
                        case 2:
                            viewModel.deleteConversation(conversation.getId());
                            break;
                    }
                })
                .show();
    }

    private void setupObservers() {
        viewModel.getConversations().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list);
            binding.emptyState.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            binding.recyclerConversations.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void setupActions() {
        binding.fabNewConversation.setOnClickListener(v ->
                viewModel.createNewConversation("새 대화", null, null));

        binding.toolbar.inflateMenu(R.menu.menu_conversation_list);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                toggleSearch();
                return true;
            }
            return false;
        });
    }

    private void toggleSearch() {
        boolean visible = binding.searchBar.getVisibility() == View.VISIBLE;
        if (visible) {
            binding.searchBar.setVisibility(View.GONE);
            viewModel.clearSearch();
        } else {
            binding.searchBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
