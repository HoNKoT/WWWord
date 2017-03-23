package jp.honkot.exercize.basic.wwword.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.github.gfx.android.orma.SingleAssociation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import jp.honkot.exercize.basic.wwword.R;
import jp.honkot.exercize.basic.wwword.dao.WordDao;
import jp.honkot.exercize.basic.wwword.model.Group;
import jp.honkot.exercize.basic.wwword.model.Word;

/**
 * Created by hiroki on 2017-03-17.
 */

public class ImportCSVUtil {
    private CompositeDisposable mDisposable = new CompositeDisposable();
    private Context mContext;

    public interface OnReadFinishListener {
        void onError();
        void onFinish();
    }

    WordDao mWordDao;

    public ImportCSVUtil(Context context, WordDao wordDao) {
        mContext = context;
        mWordDao = wordDao;
    }

    public void readCSV(final String path, final OnReadFinishListener listener, final Group group) {
        final File file = new File(path);
        final ProgressDialog.Builder builder = new ProgressDialog.Builder(mContext);
        builder.setTitle(R.string.dialog_import_title);
        builder.setMessage(file.getName());
        final AlertDialog dialog = builder.show();

        // Here is observable what doing in background.
        Observable<String> observable = Observable.create((ObservableEmitter<String> emitter) -> {
            try {
                innerReadCSV(new FileInputStream(file), group);
                emitter.onNext("Done");

            } catch (Exception e) {
                emitter.onError(e);
            }
//            if (response.isSuccessful()) {
//                nextPage++;
//                List<VisitReport> visitReports = response.body();
//                emitter.onNext(visitReports);
//            } else {
//                emitter.onError(null);
//            }
        });

        mDisposable.add(observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        finish();
                        Log.e("ImportCSVUtil", "Something error occurred " + e.getMessage());
                        e.getStackTrace();
                    }

                    @Override
                    public void onNext(String msg) {
                        finish();
                    }

                    private void finish() {
                        dialog.dismiss();
                        Toast.makeText(mContext, R.string.dialog_import_done, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onFinish();
                    }
                }));
    }

    public void readCSV(final int resId, final OnReadFinishListener listener, final Group group) {

        final ProgressDialog.Builder builder = new ProgressDialog.Builder(mContext);
        builder.setTitle(R.string.dialog_import_title);
        final AlertDialog dialog = builder.show();

        // Here is observable what doing in background.
        Observable<String> observable = Observable.create((ObservableEmitter<String> emitter) -> {
            try {
                final InputStream inputStream = mContext.getResources().openRawResource(resId);
                innerReadCSV(inputStream, group);
                emitter.onNext("Done");

            } catch (Exception e) {
                emitter.onError(e);
            }
//            if (response.isSuccessful()) {
//                nextPage++;
//                List<VisitReport> visitReports = response.body();
//                emitter.onNext(visitReports);
//            } else {
//                emitter.onError(null);
//            }
        });

        mDisposable.add(observable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    public void onComplete() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        finish();
                        Log.e("ImportCSVUtil", "Something error occurred " + e.getMessage());
                        e.getStackTrace();
                    }

                    @Override
                    public void onNext(String msg) {
                        finish();
                    }

                    private void finish() {
                        dialog.dismiss();
                        Toast.makeText(mContext, R.string.dialog_import_done, Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onFinish();
                    }
                }));
    }

    private void innerReadCSV(final InputStream inputStream, final Group group) {
        //this requires there to be a dictionary.csv file in the raw directory
        //in this case you can swap in whatever you want

        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        long startListId = mWordDao.getMaximumListId(group) + 1;

        final String WORD = "WORD";
        final String MEANING = "MEANING";
        final String EXAMPLE = "EXAMPLE";
        final String DETAIL = "DETAIL";
        final String MEMO = "MEMO";
        ArrayList<String> defaultCaption = new ArrayList<>();
        defaultCaption.add(WORD);
        defaultCaption.add(MEANING);
        defaultCaption.add(EXAMPLE);
        defaultCaption.add(DETAIL);
        defaultCaption.add(MEMO);

        int wordIndex = -1;
        int meaningIndex = -1;
        int exampleIndex = -1;
        int detailIndex = -1;
        int memoIndex = -1;

        try {
            String word;

            boolean firstRow = true;

            while ((word = reader.readLine()) != null) {
                String[] words = word.split(",");
                if (words.length == 0) continue;

                if (firstRow) {
                    boolean isCaption = false;
                    for (int i = 0; i < words.length; i++) {
                        if (defaultCaption.contains(words[i])) {
                            isCaption = true;
                            switch (words[i]) {
                                case WORD: wordIndex = i; break;
                                case MEANING: meaningIndex = i; break;
                                case EXAMPLE: exampleIndex = i; break;
                                case DETAIL: detailIndex = i; break;
                                case MEMO: memoIndex = i; break;
                            }
                        }
                    }
                    firstRow = false;

                    if (isCaption) {
                        continue;

                    } else {
                        // input default
                        wordIndex = 0;
                        meaningIndex = 1;
                        exampleIndex = 2;
                        detailIndex = 3;
                        memoIndex = 4;
                    }
                }

                for (int i = 0; i < words.length; i++) {
                    words[i] = words[i].trim(); // eliminate space of start and end
                    if (words[i].startsWith("\"") && words[i].endsWith("\"")) {
                        words[i] = words[i].substring(1, words[i].length() - 2);
                    }
                }

                Word newWord = new Word();
                if (words.length > wordIndex && wordIndex >= 0) newWord.setWord(words[wordIndex]);
                if (words.length > meaningIndex && meaningIndex >= 0) newWord.setMeaning(words[meaningIndex]);
                if (words.length > exampleIndex && exampleIndex >= 0) newWord.setExample(words[exampleIndex]);
                if (words.length > detailIndex && detailIndex >= 0) newWord.setDetail(words[detailIndex]);
                if (words.length > memoIndex && memoIndex >= 0) newWord.setMemo(words[memoIndex]);
                newWord.setListId(startListId++);
                newWord.setGroup(SingleAssociation.just(group));
                mWordDao.insert(newWord);

                if (Debug.isDBG) {
                    Debug.Log(newWord.toString());
                }
            }

            //write the dictionary to a file
//            File file = new File("test_output");
//            BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream("test_output"));
//            ObjectOutputStream oos = new ObjectOutputStream(fos);
//            oos.writeObject(dictionaryHash);
//            oos.flush();
//            oos.close();
//            Log.v("alldone","done");

        } catch (Exception ex) {
            // handle exception
            Log.v(ex.getMessage(), "message");

        } finally {
            try {
                inputStream.close();

            } catch (IOException e) {
                // handle exception
                Log.v(e.getMessage(), "message");
            }
        }
    }

    public void clear() {
        if (mDisposable.isDisposed()) {
            mDisposable.clear();
        }
    }
}
