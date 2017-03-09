package jp.honkot.exercize.basic.wwword.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

import jp.honkot.exercize.basic.wwword.BaseApplication;
import jp.honkot.exercize.basic.wwword.dao.PreferenceDao;
import jp.honkot.exercize.basic.wwword.model.Preference;
import jp.honkot.exercize.basic.wwword.service.NotificationService;

public class BootReceiver extends BroadcastReceiver {

    @Inject
    PreferenceDao preferenceDao;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
                || intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED)) {
            ((BaseApplication)context.getApplicationContext()).getComponent().inject(this);

            Preference pref = preferenceDao.getPreference();
            if (pref != null && pref.isNotify()) {
                NotificationService.startService(context);
            }

            return;
        }
        throw new UnsupportedOperationException("Not yet implemented " + intent.getAction());
    }
}
