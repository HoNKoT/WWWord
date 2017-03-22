package jp.honkot.exercize.basic.wwword.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import javax.inject.Inject;

import jp.honkot.exercize.basic.wwword.R;
import jp.honkot.exercize.basic.wwword.dao.GroupDao;
import jp.honkot.exercize.basic.wwword.dao.PreferenceDao;
import jp.honkot.exercize.basic.wwword.dao.WordDao;
import jp.honkot.exercize.basic.wwword.databinding.ActivityListWordBinding;
import jp.honkot.exercize.basic.wwword.databinding.RowWordBinding;
import jp.honkot.exercize.basic.wwword.model.OrmaDatabase;
import jp.honkot.exercize.basic.wwword.model.Preference;
import jp.honkot.exercize.basic.wwword.model.Word;
import jp.honkot.exercize.basic.wwword.model.Word_Selector;
import jp.honkot.exercize.basic.wwword.service.NotificationService;
import jp.honkot.exercize.basic.wwword.util.ImportCSVUtil;

public class WordListActivity extends BaseActivity {

    RecyclerAdapter adapter;
    ActivityListWordBinding binding;
    ItemTouchHelper itemTouchHelper;

    private static final int REQUEST_CODE = 1;
    public static final int RESULT_SUCCEEDED = 1;
    private static final String EXTRA_GROUP_ID = "EXTRA_GROUP_ID";
    private static final String EXTRA_LIST_ID = "EXTRA_LIST_ID";
    private long groupId;

    @Inject
    WordDao wordDao;

    @Inject
    GroupDao groupDao;

    @Inject
    PreferenceDao preferenceDao;

    @Inject
    OrmaDatabase orma;

    ImportCSVUtil importCSVUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getComponent().inject(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_list_word);

        Preference pref = preferenceDao.getPreference();
        if (pref == null) {
            // generate initial preference
            Preference newPref = new Preference();
            newPref.setNotificationInterval(Preference.DEFAULT_INTERVAL);
            newPref.setPopup(true);
            preferenceDao.insert(newPref);

        }

        initialize();
    }

    private void initialize() {
        groupId = getIntent().getLongExtra(EXTRA_GROUP_ID, 0);
        getSupportActionBar().setTitle(groupDao.findById(groupId).getName());

        adapter = new RecyclerAdapter();

        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        // scroll to list id the intent has
        long listId = getIntent().getLongExtra(EXTRA_LIST_ID, 0);
        if (listId > 0){
            binding.list.scrollToPosition((int)(listId - 1));
        }

        // set swipe animation
        itemTouchHelper = new ItemTouchHelper(adapter.getCallback());
        itemTouchHelper.attachToRecyclerView(binding.list);

        // set notification service
        Preference pref = preferenceDao.getPreference();
        if (pref != null && pref.isNotify()) {
            Word_Selector selector = wordDao.findAllByGroupId(groupId);
            if (!selector.isEmpty()) {
                NotificationService.startService(this);
            } else {
                NotificationService.stopService(this);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE && resultCode == RESULT_SUCCEEDED) {
            initialize();
        }
    }

    @Override
    protected void onDestroy() {
        if (importCSVUtil != null) {
            importCSVUtil.clear();
        }
        super.onDestroy();
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHolder> {

        private Word_Selector selector;
        private int count = 0;
        private SparseArray<Word> mCash = new SparseArray<>();
        private ArrayList<MyViewHolder> mHolderArray = new ArrayList<>();

        private RecyclerAdapter() {
            selector = wordDao.findAllByGroupId(groupId);
            count = selector.count();
        }

        @Override
        public RecyclerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            RowWordBinding itemBinding = RowWordBinding.inflate(layoutInflater, parent, false);
            return new MyViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            Word item = getItemForPosition(position);
            holder.binding.setWord(item);
            holder.binding.rowRoot.setOnClickListener(
                    v -> onRecyclerClicked(holder.binding.getRoot(), holder.getLayoutPosition()));
            holder.binding.rowRoot.setOnLongClickListener(
                    v -> onRecyclerLongClicked(holder.binding.getRoot(), holder.getLayoutPosition()));

            if (!mHolderArray.contains(holder)) {
                mHolderArray.add(holder);
            }
        }

        @Override
        public int getItemCount() {
            if (selector != null) {
                return count;
            } else {
                return 0;
            }
        }

        @Nullable
        private Word getItemForPosition(int position) {
            if (position < count) {
                Word cashWord = mCash.get(position);
                if (cashWord == null) {
                    cashWord = selector.get(position);
                    mCash.append(position, cashWord);
                }
                return cashWord;

            } else {
                return null;
            }
        }

        private void onRecyclerClicked(View view, int position) {
            startActivity(WordEditActivity.createIntent(
                    getApplicationContext(),
                    groupId,
                    getItemForPosition(position).getId()));
        }

        private boolean onRecyclerLongClicked(View view, final int position) {
            new AlertDialog.Builder(WordListActivity.this)
                    .setTitle("ACTION")
                    .setMessage("Choose menu you want to do")
                    .setPositiveButton("Add", (dialog, which) -> addItem(position))
                    .setNegativeButton("Delete", (dialog, which) -> remove(position))
                    .show();
            return true;
        }

        protected class MyViewHolder extends RecyclerView.ViewHolder {

            private final RowWordBinding binding;

            private MyViewHolder(RowWordBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        private void addItem(int position) {
            final Word word = new Word();
            word.setListId(position + 1);
            word.setWord("Word #" + Integer.toHexString(this.hashCode()));
            word.setMeaning("Meaning #" + Integer.toHexString(this.hashCode()));
            word.setExample("Detail #" + Integer.toHexString(this.hashCode()));
            word.setMemo("Memo #" + Integer.toHexString(this.hashCode()));
            orma.transactionNonExclusiveSync(() -> wordDao.insert(word));

            refreshData();
            notifyItemInserted(position);

            // The database selector does not change soon,
            // so the time to sync display info should be delayed just a second.
            mHandler.sendEmptyMessageDelayed(MEG_CHANGE_DISPLAY_LISTID, 500);
        }

        public void remove(final int position) {
            orma.transactionNonExclusiveSync(() -> wordDao.remove(getItemForPosition(position)));

            refreshData();
            notifyItemRemoved(position);

            // The database selector does not change soon,
            // so the time to sync display info should be delayed just a second.
            mHandler.sendEmptyMessageDelayed(MEG_CHANGE_DISPLAY_LISTID, 500);
        }

        private void refreshData() {
            selector = wordDao.findAllByGroupId(groupId);
            count = selector.count();
            mCash.clear();
        }

        private void refreshListId() {
            for (MyViewHolder holder : mHolderArray) {
                int position = holder.getLayoutPosition();
                if (position >= 0) {
                    Word word = getItemForPosition(position);
                    if (word != null) {
                        holder.binding.rowListId.setText(
                                word.getDisplayListId());
                    }
                }
            }
        }

        public ItemTouchHelper.SimpleCallback getCallback() {
            return callback;
        }

        private ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                new AlertDialog.Builder(WordListActivity.this)
                        .setTitle("CONFIRM")
                        .setMessage("Are you sure what you want to delete?")
                        .setPositiveButton("DELETE",
                                (dialog1, which) -> {
                            int swipedPosition = viewHolder.getAdapterPosition();
                            remove(swipedPosition);
                        })
                        .setNegativeButton("CANCEL",
                                (dialog12, which) -> {
                            // nothing to do
                            notifyDataSetChanged();
                        })
                        .show();
            }
        };

        private final int MEG_CHANGE_DISPLAY_LISTID = 0;
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MEG_CHANGE_DISPLAY_LISTID:
                        refreshListId();
                        break;
                }
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_add:
                startActivityForResult(WordEditActivity.createIntent(
                        getApplicationContext(),
                        groupId),
                        REQUEST_CODE);
                return true;

            case R.id.menu_preference:
                Intent intentPreference = new Intent(this, PreferenceActivity.class);
                startActivity(intentPreference);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static Intent createIntent(Context context, long groupId) {
        Intent intent = new Intent(context, WordListActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        return intent;
    }

    public static Intent createIntent(Context context, long groupId, long listId) {
        Intent intent = new Intent(context, WordListActivity.class);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        intent.putExtra(EXTRA_LIST_ID, listId);
        return intent;
    }
}
