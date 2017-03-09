package jp.honkot.exercize.basic.wwword.model;

import com.android.annotations.NonNull;
import com.github.gfx.android.orma.annotation.Column;
import com.github.gfx.android.orma.annotation.Getter;
import com.github.gfx.android.orma.annotation.Setter;
import com.github.gfx.android.orma.annotation.Table;

@Table
public class Word extends BaseModel {

    @Column(indexed = true)
    private long listId;

    @Column
    @NonNull
    private String word;

    @Column
    @NonNull
    private String meaning;

    @Column
    @NonNull
    private String detail;

    @Column
    @NonNull
    private String memo;

    @Column
    @NonNull
    private String audioFile;

    public Word() {
        this.word = "";
        this.meaning = "";
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
    public String getDetail() {
        return detail;
    }

    @Setter
    public void setDetail(String detail) {
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
        final StringBuffer sb = new StringBuffer("Word{");
        super.append(sb);
        sb.append("listId=").append(listId);
        sb.append(", word='").append(word).append('\'');
        sb.append(", meaning='").append(meaning).append('\'');
        sb.append(", detail='").append(detail).append('\'');
        sb.append(", memo='").append(memo).append('\'');
        sb.append(", audioFile='").append(audioFile).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
