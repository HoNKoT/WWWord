package jp.honkot.exercize.basic.wwword.dao;

import android.database.Cursor;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import jp.honkot.exercize.basic.wwword.model.Group;
import jp.honkot.exercize.basic.wwword.model.Group_Relation;
import jp.honkot.exercize.basic.wwword.model.Group_Schema;
import jp.honkot.exercize.basic.wwword.model.Group_Selector;
import jp.honkot.exercize.basic.wwword.model.OrmaDatabase;

@Singleton
public class GroupDao {
    OrmaDatabase orma;

    @Inject
    public GroupDao(OrmaDatabase orma) {
        this.orma = orma;
    }

    public Group_Relation relation() {
        return orma.relationOfGroup();
    }

    @Nullable
    public Group findById(long id) {
        return relation().selector().idEq(id).valueOrNull();
    }

    public Group_Selector findAll() {
        return relation().selector().orderByListIdAsc();
    }

    public long insert(final Group value) {
        if (!relation().listIdEq(value.getListId()).isEmpty()) {
            // increment the listId in the records which have the listId greater than value.getListId()
            Cursor c = orma.getConnection().rawQuery(
                    "UPDATE `" + Group_Schema.INSTANCE.getTableName()
                            + "` SET " + Group_Schema.INSTANCE.listId.getEscapedName()
                            + " = (" + Group_Schema.INSTANCE.listId.getEscapedName() + " + 1)"
                            + " WHERE " + Group_Schema.INSTANCE.listId.getEscapedName() + " >= '" + value.getListId() + "'");
            c.moveToFirst();
            c.close();
        }

        return relation().inserter().execute(value);
    }

    public long remove(final Group value) {
        // decrement the listId in the records which have the listId greater than value.getListId()
        Cursor c = orma.getConnection().rawQuery(
                "UPDATE `" + Group_Schema.INSTANCE.getTableName()
                        + "` SET " + Group_Schema.INSTANCE.listId.getEscapedName()
                        + " = (" + Group_Schema.INSTANCE.listId.getEscapedName() + " - 1)"
                        + " WHERE " + Group_Schema.INSTANCE.listId.getEscapedName() + " >= '" + value.getListId() + "'");
        c.moveToFirst();
        c.close();

        return relation().deleter()
                .idEq(value.getId())
                .execute();
    }

    public long update(final Group value) {
        return relation().updater()
                .idEq(value.getId())
                .listId(value.getListId())
                .name(value.getName())
                .notify(value.isNotify())
                .execute();
    }

    public long getMaximumListId() {
        Long maximumListId = relation().selector().maxByListId();
        return maximumListId == null ? 0 : maximumListId;
    }
}
