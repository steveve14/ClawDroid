package com.clawdroid.feature.chat.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clawdroid.core.data.db.entity.ConversationEntity;
import com.clawdroid.core.data.db.entity.PersonaEntity;
import com.clawdroid.core.data.repository.ConversationRepository;
import com.clawdroid.core.data.repository.PersonaRepository;
import com.clawdroid.core.data.repository.SettingsRepository;

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
    private final SettingsRepository settingsRepository;
    private final PersonaRepository personaRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    private final MutableLiveData<List<ConversationEntity>> conversations = new MutableLiveData<>();
    private final MutableLiveData<List<PersonaEntity>> personas = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(true);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<String> navigateToConversationId = new MutableLiveData<>();

    private Disposable searchDisposable;

    @Inject
    public ConversationListViewModel(ConversationRepository repository,
                                     SettingsRepository settingsRepository,
                                     PersonaRepository personaRepository) {
        this.repository = repository;
        this.settingsRepository = settingsRepository;
        this.personaRepository = personaRepository;
        loadConversations();
        loadPersonas();
    }

    public LiveData<List<ConversationEntity>> getConversations() { return conversations; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<String> getNavigateToConversationId() { return navigateToConversationId; }

    public String getPersonaName() { return settingsRepository.getPersonaName(); }
    public String getPersonaSystemPrompt() { return settingsRepository.getSystemPrompt(); }
    public LiveData<List<PersonaEntity>> getPersonas() { return personas; }

    public void clearNavigateToConversation() { navigateToConversationId.setValue(null); }

    private void loadPersonas() {
        disposables.add(
            personaRepository.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    list -> personas.setValue(list),
                    e -> {}
                )
        );
    }

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

    public void createNewConversation(String title, String modelProvider, String modelId,
                                        String systemPrompt) {
        // 명시된 모델이 없으면 설정의 기본값 사용
        String provider = (modelProvider != null && !modelProvider.isEmpty())
                ? modelProvider
                : settingsRepository.getActiveProvider();
        String model = (modelId != null && !modelId.isEmpty())
                ? modelId
                : settingsRepository.getDefaultModelId(provider != null ? provider : "");

        disposables.add(
            repository.createConversation(title, provider, model, systemPrompt)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    conv -> navigateToConversationId.setValue(conv.getId()),
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

    public void renameConversation(String id, String newTitle) {
        disposables.add(
            repository.renameConversation(id, newTitle)
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
