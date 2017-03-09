package jp.honkot.exercize.basic.wwword.model;

import com.github.gfx.android.orma.annotation.Column;
import com.github.gfx.android.orma.annotation.Getter;
import com.github.gfx.android.orma.annotation.Setter;
import com.github.gfx.android.orma.annotation.Table;

@Table
public class Preference extends BaseModel {

    public static final long DEFAULT_INTERVAL = 5 * 60 * 1000;
    private static final String INNER_DEFAULT_INTERVAL = "300000";

    @Column(defaultExpr = INNER_DEFAULT_INTERVAL)
    private long notificationInterval;

    @Column(defaultExpr = "0")
    private boolean wakeup;

    @Column(defaultExpr = "1")
    private boolean popup;

    @Column(defaultExpr = "0")
    private int vib;

    @Column(defaultExpr = "0")
    private int ring;

    @Getter
    public long getNotificationInterval() {
        return notificationInterval;
    }

    @Setter
    public void setNotificationInterval(long notifycationInterval) {
        this.notificationInterval = notifycationInterval;
    }

    @Getter
    public int getVib() {
        return vib;
    }

    @Setter
    public void setVib(int vib) {
        this.vib = vib;
    }

    @Getter
    public int getRing() {
        return ring;
    }

    @Setter
    public void setRing(int ring) {
        this.ring = ring;
    }

    @Getter
    public boolean isWakeup() {
        return wakeup;
    }

    @Setter
    public void setWakeup(boolean wakeup) {
        this.wakeup = wakeup;
    }

    @Getter
    public boolean isPopup() {
        return popup;
    }

    @Setter
    public void setPopup(boolean popup) {
        this.popup = popup;
    }

    public boolean isNotify() {
        return this.notificationInterval != 0;
    }

    public boolean isVib() {
        return this.vib != 0;
    }

    public boolean isRing() {
        return this.ring != 0;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Preference{");
        super.append(sb);
        sb.append(", notificationInterval=").append(notificationInterval);
        sb.append(", wakeup=").append(wakeup);
        sb.append(", popup=").append(popup);
        sb.append(", vib=").append(vib);
        sb.append(", ring=").append(ring);
        sb.append('}');
        return sb.toString();
    }
}
