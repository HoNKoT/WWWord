package jp.honkot.exercize.basic.wwword.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import java.util.Calendar;
import java.util.Random;

import javax.inject.Inject;

import jp.honkot.exercize.basic.wwword.BaseApplication;
import jp.honkot.exercize.basic.wwword.R;
import jp.honkot.exercize.basic.wwword.activity.WordListActivity;
import jp.honkot.exercize.basic.wwword.dao.PreferenceDao;
import jp.honkot.exercize.basic.wwword.dao.WordDao;
import jp.honkot.exercize.basic.wwword.di.AppComponent;
import jp.honkot.exercize.basic.wwword.model.Preference;
import jp.honkot.exercize.basic.wwword.model.Word;
import jp.honkot.exercize.basic.wwword.model.Word_Selector;
import jp.honkot.exercize.basic.wwword.util.Debug;

public class NotificationService extends Service {

    private AppComponent component;
    private static final String ACTION_SHOW_WORD = "ACTION_SHOW_WORD";
    private static int NOTIFY_ID = 777;
    private PendingIntent alarmIntent;
    private long mLastAlertSetTime = 0;

    private static boolean working = false;

    @Inject
    WordDao wordDao;

    @Inject
    PreferenceDao preferenceDao;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Debug.Log("Service receive " + intent.getAction());
            Preference pref = preferenceDao.getPreference();
            if (pref == null) return;

            switch (intent.getAction()) {
                case Intent.ACTION_SCREEN_ON:
                    if (!pref.isWakeup()) {
                        if (System.currentTimeMillis() - mLastAlertSetTime
                                > pref.getNotificationInterval()) {
                            showWord();
                        }

                        setAlarm();
                    }
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    if (!pref.isWakeup()) {
                        stopAlarm();
                    }
                    break;
                case ACTION_SHOW_WORD:
                    setAlarm();
                    showWord();
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getComponent().inject(this);

        // set broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(ACTION_SHOW_WORD);
        registerReceiver(receiver, filter);

        // start alarm manager
        setAlarm();

        // notify service
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        Notification notification;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            Intent intent = new Intent(this, WordListActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(
                    this, 0, intent, 0);

            notification = new NotificationCompat.Builder(this)
                    .setAutoCancel(true)
                    .setContentTitle(getString(R.string.app_name))
                    .setSmallIcon(R.mipmap.notification)
                    .setContentIntent(contentIntent)
                    .build();

        } else {
            Notification.Builder builder = new Notification.Builder(this);

            builder.setTicker(getString(R.string.app_name)); // show status bar text
            builder.setContentTitle(getString(R.string.app_name)); // show notification title
            builder.setContentText(getString(R.string.app_name)); // show notification subtitle (1)  (2)isSubTitle
            builder.setSmallIcon(R.mipmap.notification); //icon
            Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
            builder.setLargeIcon(bm);

            Intent intent = new Intent(this, WordListActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(
                    this, 0, intent, 0);
            builder.setContentIntent(contentIntent);
            builder.setAutoCancel(true);

            notification = builder.build();
        }
        manager.notify(NOTIFY_ID, notification);

        stopForeground(false);
        startForeground(NOTIFY_ID, notification);
    }

    @NonNull
    public AppComponent getComponent() {
        if (component == null) {
            BaseApplication hackApplication = (BaseApplication) getApplication();
            component = hackApplication.getComponent();
        }
        return component;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // remove broadcast receiver
        unregisterReceiver(receiver);

        // stop alarm manager
        stopAlarm();

        // remove notification
        stopForeground(true);

        receiver = null;
        alarmIntent = null;

        super.onDestroy();
    }

    public static void startService(Context context) {
        Intent i = new Intent(context, NotificationService.class);
        context.startService(i);
    }

    public static void stopService(Context context) {
        Intent stopIntent = new Intent(context, NotificationService.class);
        context.stopService(stopIntent);
    }

    private void setAlarm() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent();
        intent.setAction(ACTION_SHOW_WORD);
        if (alarmIntent == null) {
            alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        }

        Preference pref = preferenceDao.getPreference();
        Debug.Log("set alarm " + pref);
        if (pref != null) {
            Calendar calendar = Calendar.getInstance();
            long now = System.currentTimeMillis();
            calendar.setTimeInMillis(now);
            calendar.add(Calendar.MILLISECOND, (int)pref.getNotificationInterval());
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                manager.set(AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        alarmIntent);
            } else {
                manager.setExact(AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        alarmIntent);
            }

            mLastAlertSetTime = now;
        }
    }

    private void stopAlarm() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        manager.cancel(alarmIntent);
        Debug.Log("stop alarm ");
    }

    private void showWord() {
        Word_Selector relation = wordDao.findAll();

        if (relation.isEmpty()) {
            Random random = new Random(System.currentTimeMillis());
            int showListId = random.nextInt(relation.count() - 1);
            Word word = relation.get(showListId);

            NotificationManagerCompat manager = NotificationManagerCompat.from(this);
            Notification notification;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                Intent intent = new Intent(this, WordListActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(
                        this, 0, intent, 0);

                notification = new NotificationCompat.Builder(this)
                        .setContentTitle(word.getWord())
                        .setContentText(word.getMeaning())
                        .setSmallIcon(R.mipmap.notification)
                        .setContentIntent(contentIntent)
                        .build();

            } else {
                Notification.Builder builder = new Notification.Builder(this);
                builder.setPriority(Notification.PRIORITY_HIGH);

                builder.setTicker(word.getWord()); // show status bar text
                builder.setContentTitle(word.getWord()); // show notification title
                builder.setContentText(word.getMeaning()); // show notification subtitle (1)  (2)isSubTitle
                builder.setSmallIcon(R.mipmap.notification); //icon
                Bitmap bm = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
                builder.setLargeIcon(bm);

                builder.setAutoCancel(false);
                Preference pref = preferenceDao.getPreference();
                if (pref != null) {
                    if (pref.isPopup() && !pref.isVib() && !pref.isRing()) {
                        builder.setVibrate(new long[]{1, 0});

                    } else if (pref.isPopup() && pref.isVib()) {
                        builder.setVibrate(new long[]{0, 1});

                    } else if (pref.isPopup() && pref.isRing()) {
                        Uri path = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.silence_1sec);
                        builder.setSound(path);
                    }
                }

                notification = builder.build();
            }

            manager.notify(NOTIFY_ID, notification);
        }
    }

}
