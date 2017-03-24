package jp.honkot.exercize.basic.wwword.activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import jp.honkot.exercize.basic.wwword.R;
import jp.honkot.exercize.basic.wwword.util.Debug;

/**
 * Created by hiroki on 2017-03-22.
 */

public class SelectCSVActivity extends BaseActivity {

    public static final String EXTRA_FILE_PATH = "EXTRA_FILE_PATH";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simple_list);

        getSupportActionBar().setTitle(R.string.activity_select_csv_label);

        final String[] DOC_PROJECTION = new String[]{"_id", "_data", "mime_type", "_size", "date_added", "title"};
        Cursor cursor = getContentResolver().query(
                MediaStore.Files.getContentUri("external"),
                DOC_PROJECTION, (String)null, (String[])null, "date_added DESC");
        ArrayList<File> files = new ArrayList<>();
        while (cursor.moveToNext()) {
            int imageId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            String path = cursor.getString(cursor.getColumnIndexOrThrow("_data"));
            String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
            String type = cursor.getString(cursor.getColumnIndexOrThrow("mime_type"));

            if (path.contains(".csv")) {
                File file = new File(path);
                files.add(file);
                Debug.Log("imageId:" + imageId + ", path:" + path + ", title:" + title + ", type:" + type);
            }
        }
        cursor.close();

        ListView lv = (ListView)findViewById(R.id.list);
        final FileListAdapter adapter = new FileListAdapter(files);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_FILE_PATH, adapter.getItem(position).getPath());
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    private class FileListAdapter extends BaseAdapter {

        ArrayList<File> files;

        FileListAdapter(ArrayList<File> files) {
            this.files = files;
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public File getItem(int position) {
            return files.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = getLayoutInflater().inflate(android.R.layout.simple_expandable_list_item_2, null);
            } else {
                view = convertView;
            }

            TextView tv = (TextView)view.findViewById(android.R.id.text1);
            tv.setText(getItem(position).getName());
            TextView tv2 = (TextView)view.findViewById(android.R.id.text2);
            tv2.setText(getItem(position).getPath());

            return view;
        }
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, SelectCSVActivity.class);
        return intent;
    }
}
