package com.clawdroid.feature.channels.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.clawdroid.core.data.db.dao.ChannelDao;
import com.clawdroid.core.data.db.entity.ChannelEntity;
import com.clawdroid.feature.channels.channel.ChannelManager;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

@HiltViewModel
public class ChannelListViewModel extends ViewModel {

    private final ChannelDao channelDao;
    private final ChannelManager channelManager;
    private final MutableLiveData<List<ChannelEntity>> channelsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    public ChannelListViewModel(ChannelDao channelDao, ChannelManager channelManager) {
        this.channelDao = channelDao;
        this.channelManager = channelManager;
        loadChannels();
    }

    public LiveData<List<ChannelEntity>> getChannels() {
        return channelsLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void loadChannels() {
        disposables.add(
                channelDao.getAll()
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                channelsLiveData::postValue,
                                e -> errorLiveData.postValue(e.getMessage())
                        )
        );
    }

    public void deleteChannel(ChannelEntity channel) {
        disposables.add(
                channelManager.disconnectChannel(channel.getId())
                        .andThen(channelDao.delete(channel))
                        .subscribeOn(Schedulers.io())
                        .subscribe(() -> {}, e -> errorLiveData.postValue(e.getMessage()))
        );
    }

    public void toggleConnection(ChannelEntity channel) {
        if ("connected".equals(channel.getStatus())) {
            disposables.add(
                    channelManager.disconnectChannel(channel.getId())
                            .subscribeOn(Schedulers.io())
                            .subscribe(() -> {}, e -> errorLiveData.postValue(e.getMessage()))
            );
        } else {
            disposables.add(
                    channelManager.connectChannel(channel)
                            .subscribeOn(Schedulers.io())
                            .subscribe(() -> {}, e -> errorLiveData.postValue(e.getMessage()))
            );
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
