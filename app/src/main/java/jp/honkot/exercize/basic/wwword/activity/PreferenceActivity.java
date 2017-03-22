package jp.honkot.exercize.basic.wwword.activity;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.widget.EditText;

import javax.inject.Inject;

import jp.honkot.exercize.basic.wwword.R;
import jp.honkot.exercize.basic.wwword.dao.PreferenceDao;
import jp.honkot.exercize.basic.wwword.model.Preference;
import jp.honkot.exercize.basic.wwword.service.NotificationService;

public class PreferenceActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(
                android.R.id.content, new MySettingFragment()).commit();

        // Set Action bar title
        getSupportActionBar().setTitle(
                getString(R.string.activity_preference_label));
    }

    public static class MySettingFragment extends PreferenceFragment {

        @Inject
        PreferenceDao preferenceDao;

        Preference mPref;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            ((BaseActivity) getActivity()).getComponent().inject(this);

            // Get preference data
            mPref = preferenceDao.getPreference();

            // Read preference layout
            addPreferencesFromResource(R.xml.preference);

            // Initialize layout
            initView();
        }

        private void initView() {
            SwitchPreference notifyPref =
                    (SwitchPreference)findPreference(getString(
                            R.string.activity_preference_switch_notification_title));
            notifyPref.setChecked(mPref.isNotify());
            notifyPref.setOnPreferenceChangeListener(notificationIntervalClickListener);

            SwitchPreference wakeupPref =
                    (SwitchPreference)findPreference(getString(
                            R.string.activity_preference_switch_wakeup));
            wakeupPref.setChecked(mPref.isWakeup());
            wakeupPref.setOnPreferenceChangeListener(wakeupClickListener);

            SwitchPreference popupPref =
                    (SwitchPreference)findPreference(getString(
                            R.string.activity_preference_switch_popup));
            popupPref.setChecked(mPref.isPopup());
            popupPref.setOnPreferenceChangeListener(popupClickListener);

            SwitchPreference vibPref =
                    (SwitchPreference)findPreference(getString(
                            R.string.activity_preference_switch_vib_title));
            vibPref.setChecked(mPref.isVib());
            vibPref.setOnPreferenceChangeListener(vibClickListener);

//            SwitchPreference ringPref =
//                    (SwitchPreference)findPreference(getString(
//                            R.string.activity_preference_switch_ring_title));
//            ringPref.setChecked(mPref.isRing());
//            ringPref.setOnPreferenceChangeListener(ringClickListener);

            findPreference(getString(R.string.activity_preference_libs))
                    .setOnPreferenceClickListener(libsClickListener);

            findPreference(getString(R.string.activity_preference_apps))
                    .setOnPreferenceClickListener(appsClickListener);

            findPreference(getString(R.string.activity_preference_about))
                    .setOnPreferenceClickListener(aboutClickListener);

            updatePrefs();
        }

        private void updatePrefs() {
            SwitchPreference notifyPref = (SwitchPreference)findPreference(getString(R.string.activity_preference_switch_notification_title));
            notifyPref.setSummary(mPref.isNotify()
                    ? (mPref.getNotificationInterval() / (60 * 1000)) + " minutes interval"
                    : "Does not work");

            findPreference(getString(R.string.activity_preference_switch_wakeup)).setEnabled(mPref.isNotify());
            findPreference(getString(R.string.activity_preference_switch_popup)).setEnabled(mPref.isNotify());
            findPreference(getString(R.string.activity_preference_switch_vib_title)).setEnabled(mPref.isNotify() && mPref.isPopup());
//            findPreference(getString(R.string.activity_preference_switch_ring_title)).setEnabled(mPref.isNotify() && mPref.isPopup());
        }

        private android.preference.Preference.OnPreferenceChangeListener notificationIntervalClickListener =
                new android.preference.Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(final android.preference.Preference preference, Object newValue) {
                        boolean checked = ((boolean)newValue);
                        if (checked) {
                            final EditText edit = new EditText(getActivity());
                            edit.setText(Long.toString(Preference.DEFAULT_INTERVAL / (60 * 1000)));
                            edit.setInputType(InputType.TYPE_CLASS_NUMBER);

                            new AlertDialog.Builder(getActivity())
                                    .setTitle("Notification interval")
                                    .setMessage("Type notification interval minutes")
                                    .setView(edit)
                                    .setPositiveButton("DONE", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            long temp = Long.parseLong(edit.getText().toString());
                                            setNotificationInterval(temp * 60 * 1000);
                                        }
                                    })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialogInterface) {
                                            ((SwitchPreference)preference).setChecked(false);
                                        }
                                    })
                                    .show();
                        } else {
                            setNotificationInterval(0);
                        }
                        return true;
                    }

                    private void setNotificationInterval(long time) {
                        mPref.setNotificationInterval(time);
                        if (mPref.isNotify()) {
                            NotificationService.startService(getActivity());
                        } else {
                            NotificationService.stopService(getActivity());
                        }
                        preferenceDao.updateNotificationInterval(mPref);
                        updatePrefs();
                    }
                };

        private android.preference.Preference.OnPreferenceChangeListener wakeupClickListener =
                new android.preference.Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(final android.preference.Preference preference, Object newValue) {
                        mPref.setWakeup((boolean)newValue);
                        preferenceDao.updateWakeup(mPref);
                        return true;
                    }
                };

        private android.preference.Preference.OnPreferenceChangeListener popupClickListener =
                new android.preference.Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(final android.preference.Preference preference, Object newValue) {
                        mPref.setPopup((boolean)newValue);
                        preferenceDao.updatePopup(mPref);
                        updatePrefs();
                        return true;
                    }
                };

        private android.preference.Preference.OnPreferenceChangeListener vibClickListener =
                new android.preference.Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(final android.preference.Preference preference, Object newValue) {
                        mPref.setVib((boolean)newValue ? 1 : 0);
                        preferenceDao.updateVib(mPref);
                        return true;
                    }
                };

        private android.preference.Preference.OnPreferenceChangeListener ringClickListener =
                new android.preference.Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(final android.preference.Preference preference, Object newValue) {
                        mPref.setRing((boolean)newValue ? 1 : 0);
                        preferenceDao.updateRing(mPref);
                        return true;
                    }
                };

        private android.preference.Preference.OnPreferenceClickListener libsClickListener =
                preference -> {
                    startActivity(new Intent(getActivity(), LibsActivity.class));
                    return false;
                };

        private android.preference.Preference.OnPreferenceClickListener appsClickListener =
                preference -> {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=honkot")));
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=honkot")));
                    }
                    return false;
                };

        private android.preference.Preference.OnPreferenceClickListener aboutClickListener =
                preference -> {
                    startActivity(new Intent(getActivity(), AboutActivity.class));
                    return false;
                };
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, PreferenceActivity.class);
        return intent;
    }
}
