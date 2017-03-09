package jp.honkot.exercize.basic.wwword.model;

import android.provider.BaseColumns;

import com.github.gfx.android.orma.annotation.Column;
import com.github.gfx.android.orma.annotation.Getter;
import com.github.gfx.android.orma.annotation.PrimaryKey;
import com.github.gfx.android.orma.annotation.Setter;

/**
 * Created by hhonda_admin on 2017-02-06.
 */

public class BaseModel {
    @Column(value = BaseColumns._ID)
    @PrimaryKey(autoincrement = true)
    private long id;

    @Column
    private int status;

    @Getter
    public long getId() {
        return id;
    }

    @Setter
    public void setId(long id) {
        this.id = id;
    }

    @Getter
    public int getStatus() {
        return status;
    }

    @Setter
    public void setStatus(int status) {
        this.status = status;
    }

    protected void append(StringBuffer sb) {
        sb.append("id=").append(id);
        sb.append(", status=").append(status);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("BaseModel{");
        sb.append("id=").append(id);
        sb.append(", status=").append(status);
        sb.append('}');
        return sb.toString();
    }
}
