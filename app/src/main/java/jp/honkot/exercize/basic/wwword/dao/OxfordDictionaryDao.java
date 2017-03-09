package jp.honkot.exercize.basic.wwword.dao;

import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;

import jp.honkot.exercize.basic.wwword.model.OrmaDatabase;
import jp.honkot.exercize.basic.wwword.model.OxfordDictionary;
import jp.honkot.exercize.basic.wwword.model.OxfordDictionary_Relation;
import jp.honkot.exercize.basic.wwword.util.Debug;

@Singleton
public class OxfordDictionaryDao {
    OrmaDatabase orma;

    @Inject
    public OxfordDictionaryDao(OrmaDatabase orma) {
        this.orma = orma;
    }

    public OxfordDictionary_Relation relation() {
        return orma.relationOfOxfordDictionary();
    }

    @Nullable
    public OxfordDictionary findByWord(String word) {
        OxfordDictionary_Relation relation = relation().wordEq(word);
        if (relation.isEmpty()) {
            return null;
        } else {
            OxfordDictionary ret = relation.selector().value();
            if (Debug.isDBG) {
                Debug.Log("select oxfordDictionary '" + ret.getWord() + "'");
            }
            ret.serialize();
            return ret;
        }
    }

    public long insertOrUpdate(final OxfordDictionary value) {
        OxfordDictionary_Relation relation = relation().wordEq(value.getWord());
        if (relation.isEmpty()) {
            // insert
            if (Debug.isDBG) {
                Debug.Log("insert oxfordDictionary '" + value.getWord() + "'");
            }
            return orma.insertIntoOxfordDictionary(value);
        } else {
            // update
            if (Debug.isDBG) {
                Debug.Log("update oxfordDictionary '" + value.getWord() + "'");
            }
            return orma.updateOxfordDictionary()
                    .wordEq(value.getWord())
                    .rawJson(value.getRawJson())
                    .execute();
        }
    }
}
