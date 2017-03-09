package jp.honkot.exercize.basic.wwword.dao;

import android.database.Cursor;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

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

    @Nullable
    public Word findById(long id) {
        return relation().selector().idEq(id).valueOrNull();
    }

    public Word_Selector findAll() {
        return relation().selector().orderByListIdAsc();
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
                        + " WHERE " + Word_Schema.INSTANCE.listId.getEscapedName() + " >= '" + value.getListId() + "'");
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
                .detail(value.getDetail())
                .memo(value.getMemo())
                .audioFile(value.getAudioFile())
                .execute();
    }
}
