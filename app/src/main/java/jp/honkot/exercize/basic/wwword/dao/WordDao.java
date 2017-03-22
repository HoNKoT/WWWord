package jp.honkot.exercize.basic.wwword.dao;

import android.database.Cursor;
import android.support.annotation.Nullable;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import jp.honkot.exercize.basic.wwword.model.Group;
import jp.honkot.exercize.basic.wwword.model.Group_Schema;
import jp.honkot.exercize.basic.wwword.model.OrmaDatabase;
import jp.honkot.exercize.basic.wwword.model.Word;
import jp.honkot.exercize.basic.wwword.model.Word_Relation;
import jp.honkot.exercize.basic.wwword.model.Word_Schema;
import jp.honkot.exercize.basic.wwword.model.Word_Selector;

@Singleton
public class WordDao {
    OrmaDatabase orma;

    @Inject
    public WordDao(OrmaDatabase orma) {
        this.orma = orma;
    }

    public Word_Relation relation() {
        return orma.relationOfWord();
    }

    public long getMaximumListId(Group group) {
        Long maximumListId = relation().selector().groupEq(group).maxByListId();
        return maximumListId == null ? 0 : maximumListId;
    }

    @Nullable
    public Word findById(long id) {
        return relation().selector().idEq(id).valueOrNull();
    }

    public Word_Selector findAllByGroupId(long groupId) {
        return relation().selector().groupEq(groupId).orderByListIdAsc();
    }

    public Word_Selector findAllOfNeedNotify() {
        Cursor groupCursor = orma.relationOfGroup().selector()
                .notifyEq(true)
                .executeWithColumns(Group_Schema.INSTANCE.id.getEscapedName());

        if (!groupCursor.moveToFirst()) {
            return null;
        }

        StringBuilder groupInClauses = new StringBuilder()
                .append(Word_Schema.INSTANCE.group.getEscapedName())
                .append(" IN (");
        int index = 0;
        do {
            groupInClauses.append(groupCursor.getLong(0));
            if (index < groupCursor.getCount() - 1) {
                index++;
                groupInClauses.append(", ");
            }
        } while (groupCursor.moveToNext());
        groupCursor.close();
        groupInClauses.append(")");

        return relation()
                .selector()
                .where(groupInClauses.toString(), new ArrayList<>());
    }

    public long insert(final Word value) {
        if (!relation().listIdEq(value.getListId()).isEmpty()) {
            // increment the listId in the records which have the listId greater than value.getListId()
            Cursor c = orma.getConnection().rawQuery(
                    "UPDATE `" + Word_Schema.INSTANCE.getTableName()
                            + "` SET " + Word_Schema.INSTANCE.listId.getEscapedName()
                            + " = (" + Word_Schema.INSTANCE.listId.getEscapedName() + " + 1)"
                            + " WHERE " + Word_Schema.INSTANCE.listId.getEscapedName() + " >= '" + value.getListId() + "'");
            c.moveToFirst();
            c.close();
        }

        return orma.insertIntoWord(value);
    }

    public long remove(final Word value) {
        // decrement the listId in the records which have the listId greater than value.getListId()
        Cursor c = orma.getConnection().rawQuery(
                "UPDATE `" + Word_Schema.INSTANCE.getTableName()
                        + "` SET " + Word_Schema.INSTANCE.listId.getEscapedName()
                        + " = (" + Word_Schema.INSTANCE.listId.getEscapedName() + " - 1)"
                        + " WHERE " + Word_Schema.INSTANCE.listId.getEscapedName() + " >= '" + value.getListId() + "'"
                        + " AND " + Word_Schema.INSTANCE.group.getEscapedName() + " = '" + value.getGroup().getId() + "'");
        c.moveToFirst();
        c.close();

        return relation().deleter()
                .idEq(value.getId())
                .execute();
    }

    public long update(final Word value) {
        return orma.updateWord()
                .idEq(value.getId())
                .word(value.getWord())
                .meaning(value.getMeaning())
                .example(value.getExample())
                .detail(value.getExample())
                .memo(value.getMemo())
                .audioFile(value.getAudioFile())
                .group(value.getGroup())
                .execute();
    }

    public Word_Selector likeQuery(String value, final long groupId) {
        return relation().selector().where(
                Word_Schema.INSTANCE.word.getEscapedName() + " LIKE ?", "%" + value + "%")
                .groupEq(groupId)
                .orderByListIdAsc();
    }
}
