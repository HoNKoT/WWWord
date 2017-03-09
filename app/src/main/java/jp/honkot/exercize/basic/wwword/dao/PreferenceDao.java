package jp.honkot.exercize.basic.wwword.dao;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import jp.honkot.exercize.basic.wwword.model.OrmaDatabase;
import jp.honkot.exercize.basic.wwword.model.Preference;
import jp.honkot.exercize.basic.wwword.model.Preference_Relation;

@Singleton
public class PreferenceDao {
    OrmaDatabase orma;

    @Inject
    public PreferenceDao(OrmaDatabase orma) {
        this.orma = orma;
    }

    public Preference_Relation relation() {
        return orma.relationOfPreference();
    }

    @Nullable
    public Preference getPreference() {
        return relation().selector().idEq(1).valueOrNull();
    }

    public long insert(final Preference value) {
        return orma.insertIntoPreference(value);
    }

    public long remove(final Preference value) {
        return relation().deleter().idEq(value.getId()).execute();
    }

    public void updateNotificationInterval(final Preference value) {
        orma.transactionNonExclusiveSync(new Runnable() {
            @Override
            public void run() {
                orma.updatePreference()
                        .idEq(value.getId())
                        .notificationInterval(value.getNotificationInterval())
                        .execute();
            }
        });
    }

    public void updateWakeup(final Preference value) {
        orma.transactionNonExclusiveSync(new Runnable() {
            @Override
            public void run() {
                orma.updatePreference()
                        .idEq(value.getId())
                        .wakeup(value.isWakeup())
                        .execute();
            }
        });
    }

    public void updatePopup(final Preference value) {
        orma.transactionNonExclusiveSync(new Runnable() {
            @Override
            public void run() {
                orma.updatePreference()
                        .idEq(value.getId())
                        .popup(value.isPopup())
                        .execute();
            }
        });
    }

    public void updateVib(final Preference value) {
        orma.transactionNonExclusiveSync(new Runnable() {
            @Override
            public void run() {
                orma.updatePreference()
                        .idEq(value.getId())
                        .vib(value.getVib())
                        .execute();
            }
        });
    }

    public void updateRing(final Preference value) {
        orma.transactionNonExclusiveSync(new Runnable() {
            @Override
            public void run() {
                orma.updatePreference()
                        .idEq(value.getId())
                        .ring(value.getRing())
                        .execute();
            }
        });
    }
}
