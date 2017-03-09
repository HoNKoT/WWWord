package jp.honkot.exercize.basic.wwword.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;

import jp.honkot.exercize.basic.wwword.R;
import jp.honkot.exercize.basic.wwword.dao.OxfordDictionaryDao;
import jp.honkot.exercize.basic.wwword.dao.WordDao;
import jp.honkot.exercize.basic.wwword.databinding.ActivityEditWordBinding;
import jp.honkot.exercize.basic.wwword.model.OxfordDictionary;
import jp.honkot.exercize.basic.wwword.model.Word;
import jp.honkot.exercize.basic.wwword.util.Debug;

public class WordEditActivity extends BaseActivity implements View.OnClickListener {

    public static final String EXTRA_WORD_ID = "EXTRA_WORD_ID";
    private ActivityEditWordBinding binding;
    private Word mWord;
    private OxfordDictionary mOxfordDictionary;

    @Inject
    WordDao wordDao;

    @Inject
    OxfordDictionaryDao oxfordDictionaryDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getComponent().inject(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_word);
        initialize();
    }

    private void initialize() {
        // Get and set initial values
        long id = getIntent().getLongExtra(EXTRA_WORD_ID, 0);
        if (id != 0) {
            mWord = wordDao.findById(id);
        }
        if (mWord == null) {
            mWord = new Word();
        }
        binding.setWord(mWord);
        updateButtonState();

        // Set some listeners
        binding.wordEditText.addTextChangedListener(mTextWatcher);
        binding.meaningEditText.addTextChangedListener(mTextWatcher);
        binding.detailEditText.addTextChangedListener(mTextWatcher);
        binding.memoEditText.addTextChangedListener(mTextWatcher);
        binding.registerButton.setOnClickListener(this);
        binding.getButton.setOnClickListener(this);

        // Get OxfordDictionary if I have
        if (!mWord.getWord().isEmpty()) {
            mOxfordDictionary = oxfordDictionaryDao.findByWord(mWord.getWord());
        }
    }

    private void updateButtonState() {
        binding.registerButton.setEnabled(
                !binding.wordEditText.getText().toString().isEmpty()
                && !binding.meaningEditText.getText().toString().isEmpty());
        binding.getButton.setEnabled(!binding.wordEditText.getText().toString().isEmpty());
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            updateButtonState();
        }
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.getButton:
                getByOxfordDic();
                break;
            case R.id.registerButton:
                register();
                break;
        }
    }

    private void register() {
        mWord.setWord(binding.wordEditText.getText().toString());
        mWord.setMeaning(binding.meaningEditText.getText().toString());
        mWord.setDetail(binding.detailEditText.getText().toString());
        mWord.setMemo(binding.memoEditText.getText().toString());

        // Check the error just in case
        if (mWord.allowRegister()) {
            long result = 0;
            if (mWord.getId() == 0) {
                result = wordDao.insert(mWord);
            } else {
                result = wordDao.update(mWord);
            }

            if (result != 0) {
                setResult(WordListActivity.RESULT_SUCCEEDED);
                finish();
            } else {
                // error
            }
        }
    }

    private void getByOxfordDic() {
        if (binding.wordEditText.isEnabled()) {
            String inputWord = binding.wordEditText.getText().toString();
            mOxfordDictionary = oxfordDictionaryDao.findByWord(inputWord);

            if (mOxfordDictionary == null) {
                // get by web api
                Toast.makeText(getApplicationContext(), "Searching...", Toast.LENGTH_SHORT).show();
                new CallbackTask().execute(dictionaryEntries(), inputWord);
            } else {
                showSelectionDialog();
            }
        }
    }

    private String dictionaryEntries() {
        final String language = "en";
        final String word = binding.wordEditText.getText().toString();
        final String word_id = word.toLowerCase(); //word id is case sensitive and lowercase is required
        return "https://od-api.oxforddictionaries.com:443/api/v1/entries/" + language + "/" + word_id;
    }

    //in android calling network requests on the main thread forbidden by default
    //create class to do async job
    private class CallbackTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... params) {
            final String app_id = "50974be7";
            final String app_key = "38da4c6505d9dab21093068b480c7097";
            String ret = "";
            if (Debug.isDBG) {
                Debug.Log("doInBackground start");
            }
            try {
                URL url = new URL(params[0]);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept","application/json");
                urlConnection.setRequestProperty("app_id",app_id);
                urlConnection.setRequestProperty("app_key",app_key);

                // read the output from the server
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();

                String line = null;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }

                OxfordDictionary dic = new OxfordDictionary();
                dic.setWord(params[1]);
                dic.setRawJson(stringBuilder.toString());
                dic.serialize();
                oxfordDictionaryDao.insertOrUpdate(dic);
                mOxfordDictionary = dic;

                if (Debug.isDBG) {
                    Debug.Log("Done getting web dictionary! '" + mOxfordDictionary.getWord() + "'");
                }

            } catch (Exception e) {
                e.printStackTrace();
                ret = e.toString();
            }
            if (Debug.isDBG) {
                Debug.Log("doInBackground end " + ret);
            }
            return ret;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (Debug.isDBG) {
                Debug.Log("doInBackground start");
                System.out.println(result);
            }

            if (!result.isEmpty()) {
                Toast.makeText(getApplicationContext(),
                        "Search failed, make sure the word", Toast.LENGTH_SHORT).show();
            }

            if (mOxfordDictionary != null) {
                showSelectionDialog();
            }

            if (Debug.isDBG) {
                Debug.Log("doInBackground end");
            }
        }
    }

    private void showSelectionDialog() {
        if (mOxfordDictionary != null) {
            new AlertDialog.Builder(this)
                    .setTitle(mWord.getWord())
                    .setAdapter(new CustomAdapter(mOxfordDictionary.getSimpleDictionaries()), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            OxfordDictionary.SimpleDictionary dic =
                                    mOxfordDictionary.getSimpleDictionaries().get(which);
                            binding.meaningEditText.setText(dic.toString());
                            binding.detailEditText.setText(dic.example);
                        }
                    })
                    .show();
        }
    }

    private class CustomAdapter extends BaseAdapter {

        private ArrayList<OxfordDictionary.SimpleDictionary> simpleDictionaries;

        CustomAdapter(ArrayList<OxfordDictionary.SimpleDictionary> simpleDictionaries) {
            this.simpleDictionaries = simpleDictionaries;
        }

        @Override
        public int getCount() {
            return simpleDictionaries.size();
        }

        @Override
        public OxfordDictionary.SimpleDictionary getItem(int position) {
            return simpleDictionaries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
            } else {
                view = convertView;
            }

            ((TextView) view.findViewById(android.R.id.text1)).setText(getItem(position).toString());

            return view;
        }
    }
}
