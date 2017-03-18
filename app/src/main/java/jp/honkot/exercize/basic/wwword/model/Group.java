package jp.honkot.exercize.basic.wwword.model;

import android.support.annotation.NonNull;

import com.github.gfx.android.orma.annotation.Column;
import com.github.gfx.android.orma.annotation.Getter;
import com.github.gfx.android.orma.annotation.Setter;
import com.github.gfx.android.orma.annotation.Table;

@Table
public class Group extends BaseModel {

    @Column(indexed = true)
    private long listId;

    @Column
    @NonNull
    private String name;

    @Column(indexed = true)
    private boolean notify;

    public Group() {
        name = "";
    }

    @Getter
    public long getListId() {
        return listId;
    }

    public String getDisplayListId() { return listId + ":";}

    @Setter
    public void setListId(long listId) {
        this.listId = listId;
    }

    @NonNull
    @Getter
    public String getName() {
        return name;
    }

    @Setter
    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Getter
    public boolean isNotify() {
        return notify;
    }

    @Setter
    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public boolean allowRegister() {
        return !getName().isEmpty();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Group{");
        super.append(sb);
        sb.append(", listId=").append(listId);
        sb.append(", name='").append(name).append('\'');
        sb.append(", notify=").append(notify);
        sb.append('}');
        return sb.toString();
    }
}
