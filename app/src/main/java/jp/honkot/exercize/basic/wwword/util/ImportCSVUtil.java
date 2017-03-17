package jp.honkot.exercize.basic.wwword.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import jp.honkot.exercize.basic.wwword.R;
import jp.honkot.exercize.basic.wwword.dao.WordDao;
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

    public void readCSV(final int resId, final OnReadFinishListener listener) {

        final ProgressDialog.Builder builder = new ProgressDialog.Builder(mContext);
        builder.setTitle(R.string.dialog_import_title);
        final AlertDialog dialog = builder.show();

        // Here is observable what doing in background.
        Observable<String> observable = Observable.create((ObservableEmitter<String> emitter) -> {
            try {
                innerReadCSV(resId);
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

    private void innerReadCSV(int resId) {
        //this requires there to be a dictionary.csv file in the raw directory
        //in this case you can swap in whatever you want
        InputStream inputStream = mContext.getResources().openRawResource(resId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        long startListId = mWordDao.getMaximumListId() + 1;

        try {
            String word;

            while ((word = reader.readLine()) != null) {
                String[] words = word.split(",");
                if (words.length == 0) continue;

                for (int i = 0; i < words.length; i++) {
                    words[i] = words[i].trim(); // eliminate space of start and end
                    if (words[i].startsWith("\"") && words[i].endsWith("\"")) {
                        words[i] = words[i].substring(1, words[i].length() - 2);
                    }
                }

                Word newWord = new Word();
                if (words.length > 0) newWord.setWord(words[0]);
                if (words.length > 1) newWord.setMeaning(words[1]);
                if (words.length > 2) newWord.setExample(words[2]);
                newWord.setListId(startListId++);
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
