package jp.honkot.exercize.basic.wwword;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.multidex.MultiDex;

import jp.honkot.exercize.basic.wwword.di.AppComponent;
import jp.honkot.exercize.basic.wwword.di.AppModule;
import jp.honkot.exercize.basic.wwword.di.DaggerAppComponent;

public class BaseApplication extends Application {
    AppComponent appComponent;

    @NonNull
    public AppComponent getComponent() {
        return appComponent;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
