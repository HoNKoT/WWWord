package jp.honkot.exercize.basic.wwword.di;

import android.app.Application;
import android.content.Context;

import com.github.gfx.android.orma.AccessThreadConstraint;
import com.github.gfx.android.orma.migration.BuildConfig;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import jp.honkot.exercize.basic.wwword.model.OrmaDatabase;

@Module
public class AppModule {

    public static final String DATABASE_NAME = "orma.db";

    private Context context;

    public AppModule(Application app) {
        context = app;
    }

    @Provides
    public Context provideContext() {
        return context;
    }

    @Singleton
    @Provides
    public OrmaDatabase provideOrmaDatabase(Context context) {
        // TODO: メインスレッドでの読み書きは適切に変更する

        return OrmaDatabase.builder(context)
                .readOnMainThread(AccessThreadConstraint.NONE)
                .writeOnMainThread(BuildConfig.DEBUG ? AccessThreadConstraint.WARNING : AccessThreadConstraint.NONE)
                .name(AppModule.DATABASE_NAME)
                .trace(true)
                .build();
    }
}
