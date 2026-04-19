package com.clawdroid.feature.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.core.data.repository.ConversationRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ConversationListViewModel extends ViewModel {

    private final ConversationRepository repository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<ConversationEntity>> conversations = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private Disposable searchDisposable;

    @Inject
    public ConversationListViewModel(ConversationRepository repository) {
        this.repository = repository;
        loadConversations();
    }

    public LiveData<List<ConversationEntity>> getConversations() { return conversations; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    private void loadConversations() {
        disposables.add(
            repository.getActiveConversations()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    list -> {
                        conversations.setValue(list);
                        isLoading.setValue(false);
                    },
                    e -> {
                        error.setValue(e.getMessage());
                        isLoading.setValue(false);
                    }
                )
        );
    }

    public void searchConversations(String query) {
        if (searchDisposable != null) {
            searchDisposable.dispose();
        }
        searchDisposable = repository.searchConversations(query)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    conversations::setValue,
                    e -> error.setValue(e.getMessage())
                );
        disposables.add(searchDisposable);
    }

    public void clearSearch() {
        if (searchDisposable != null) {
            searchDisposable.dispose();
            searchDisposable = null;
        }
        loadConversations();
    }

    public void createNewConversation(String title, String modelProvider, String modelId) {
        disposables.add(
            repository.createConversation(title, modelProvider, modelId, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    conv -> { },
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    public void deleteConversation(String id) {
        disposables.add(
            repository.deleteConversation(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {},
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    public void archiveConversation(String id) {
        disposables.add(
            repository.archiveConversation(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {},
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    public void unarchiveConversation(String id) {
        disposables.add(
            repository.unarchiveConversation(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {},
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    public void togglePin(String id, boolean pinned) {
        disposables.add(
            repository.pinConversation(id, pinned)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    () -> {},
                    e -> error.setValue(e.getMessage())
                )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
