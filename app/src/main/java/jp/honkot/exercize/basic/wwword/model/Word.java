package jp.honkot.exercize.basic.wwword.model;

import android.support.annotation.NonNull;

import com.github.gfx.android.orma.SingleAssociation;
import com.github.gfx.android.orma.annotation.Column;
import com.github.gfx.android.orma.annotation.Getter;
import com.github.gfx.android.orma.annotation.Setter;
import com.github.gfx.android.orma.annotation.Table;

@Table
public class Word extends BaseModel {

    @Column(indexed = true)
    private long listId;

    @Column(indexed = true, defaultExpr = "1")
    @NonNull
    private SingleAssociation<Group> group;

    @Column
    @NonNull
    private String word;

    @Column
    @NonNull
    private String meaning;

    @Column
    @NonNull
    private String example;

    @Column
    @NonNull
    private String detail;

    @Column
    @NonNull
    private String memo;

    @Column
    @NonNull
    private String audioFile;

    /**
     * For Word List whether showing up detail or not
     */
    public boolean showDetail = false;

    public Word() {
        this.word = "";
        this.meaning = "";
        this.example = "";
        this.detail = "";
        this.memo = "";
        this.audioFile = "";
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
    public SingleAssociation<Group> getGroup() {
        return group;
    }

    @Setter
    public void setGroup(@NonNull SingleAssociation<Group> group) {
        this.group = group;
    }

    @Getter
    public String getWord() {
        return word;
    }

    @Setter
    public void setWord(String word) {
        this.word = word;
    }

    @Getter
    public String getMeaning() {
        return meaning;
    }

    @Setter
    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    @Getter
    public String getExample() {
        return example;
    }

    @Setter
    public void setExample(String example) {
        this.example = example;
    }

    @NonNull
    @Getter
    public String getDetail() {
        return detail;
    }

    @Setter
    public void setDetail(@NonNull String detail) {
        this.detail = detail;
    }

    @Getter
    public String getMemo() {
        return memo;
    }

    @Setter
    public void setMemo(String memo) {
        this.memo = memo;
    }

    @Getter
    public String getAudioFile() {
        return audioFile;
    }

    @Setter
    public void setAudioFile(String audioFile) {
        this.audioFile = audioFile;
    }

    public boolean allowRegister() {
        return !getWord().isEmpty() && !getMeaning().isEmpty();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Word{");
        super.append(sb);
        sb.append(", listId=").append(listId);
        sb.append(", group=").append(group);
        sb.append(", word='").append(word).append('\'');
        sb.append(", meaning='").append(meaning).append('\'');
        sb.append(", example='").append(example).append('\'');
        sb.append(", detail='").append(detail).append('\'');
        sb.append(", memo='").append(memo).append('\'');
        sb.append(", audioFile='").append(audioFile).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
