package com.clawdroid.feature.tools.di;

import android.content.Context;

import com.clawdroid.feature.tools.builtin.AlarmTool;
import com.clawdroid.feature.tools.builtin.AppLauncherTool;
import com.clawdroid.feature.tools.builtin.BrowserTool;
import com.clawdroid.feature.tools.builtin.CalculatorTool;
import com.clawdroid.feature.tools.builtin.CalendarTool;
import com.clawdroid.feature.tools.builtin.CameraTool;
import com.clawdroid.feature.tools.builtin.ContactsTool;
import com.clawdroid.feature.tools.builtin.FileManagerTool;
import com.clawdroid.feature.tools.builtin.LocationTool;
import com.clawdroid.feature.tools.tool.ToolRegistry;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;

@Module
@InstallIn(SingletonComponent.class)
public class ToolModule {

    @Provides
    @Singleton
    public ToolRegistry provideToolRegistry(@ApplicationContext Context context,
                                             OkHttpClient httpClient) {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new BrowserTool(httpClient));
        registry.register(new CalculatorTool());
        registry.register(new CalendarTool(context));
        registry.register(new ContactsTool(context));
        registry.register(new CameraTool(context));
        registry.register(new LocationTool(context));
        registry.register(new AlarmTool(context));
        registry.register(new AppLauncherTool(context));
        registry.register(new FileManagerTool(context));
        return registry;
    }
}
