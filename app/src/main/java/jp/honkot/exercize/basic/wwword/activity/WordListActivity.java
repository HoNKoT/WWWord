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
import android.widget.SearchView;

import java.util.ArrayList;

import javax.inject.Inject;

import jp.honkot.exercize.basic.wwword.R;
import jp.honkot.exercize.basic.wwword.dao.GroupDao;
import jp.honkot.exercize.basic.wwword.dao.PreferenceDao;
import jp.honkot.exercize.basic.wwword.dao.WordDao;
import jp.honkot.exercize.basic.wwword.databinding.ActivityListWordBinding;
import jp.honkot.exercize.basic.wwword.databinding.RowWordBinding;
import jp.honkot.exercize.basic.wwword.model.Group;
import jp.honkot.exercize.basic.wwword.model.OrmaDatabase;
import jp.honkot.exercize.basic.wwword.model.Preference;
import jp.honkot.exercize.basic.wwword.model.Word;
import jp.honkot.exercize.basic.wwword.model.Word_Selector;
import jp.honkot.exercize.basic.wwword.service.NotificationService;
import jp.honkot.exercize.basic.wwword.util.Debug;
import jp.honkot.exercize.basic.wwword.util.ImportCSVUtil;

public class WordListActivity extends BaseActivity {

    RecyclerAdapter adapter;
    ActivityListWordBinding binding;
    ItemTouchHelper itemTouchHelper;

    private static final int REQUEST_CODE_ADD = 1;
    private static final int REQUEST_CODE_IMPORT = 2;
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
        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                RecyclerAdapter adapter = (RecyclerAdapter) binding.list.getAdapter();
                adapter.search(newText);
                return true;
            }
        });
        binding.list.requestFocus();

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

        // Set Action bar title
        Group group = groupDao.findById(groupId);
        if (group != null) {
            getSupportActionBar().setTitle(
                    getString(R.string.activity_list_word_label, group.getName()));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD && resultCode == RESULT_OK) {
            initialize();
        } else if (requestCode == REQUEST_CODE_IMPORT && resultCode == RESULT_OK) {
            String filePath = data.getExtras().getString(SelectCSVActivity.EXTRA_FILE_PATH);
            ImportCSVUtil util = new ImportCSVUtil(this, wordDao);
            util.readCSV(filePath, new ImportCSVUtil.OnReadFinishListener() {
                @Override
                public void onError() {

                }

                @Override
                public void onFinish() {
                    initialize();
                }
            }, groupDao.findById(groupId));
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
        private String searchWord = "";

        RecyclerAdapter() {
            refreshData();
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
            holder.binding.rowDetailGroup.setVisibility(item.showDetail ? View.VISIBLE : View.GONE);
            holder.binding.rowListId.setVisibility(Debug.isDBG ? View.VISIBLE : View.GONE);
            holder.binding.rowRoot.setOnClickListener(
                    v -> onRecyclerClicked(holder.binding.getRoot(), holder.getLayoutPosition()));
            holder.binding.rowRoot.setOnLongClickListener(
                    v -> onRecyclerLongClicked(holder.binding.getRoot(), holder.getLayoutPosition()));
            holder.setPosition(position);

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
            Word word = getItemForPosition(position);
            word.showDetail = !word.showDetail;
            for (MyViewHolder holder : mHolderArray) {
                if (holder.binding.getRoot().equals(view)) {
                    holder.binding.rowDetailGroup.setVisibility(
                            word.showDetail ? View.VISIBLE : View.GONE);
                    break;
                }
            }

            Message msg = mHandler.obtainMessage(MSG_SCROLL_TO);
            msg.arg1 = position;
            mHandler.sendMessageDelayed(msg, 100);
        }

        private void scrollTo(int position) {
            MyViewHolder holder = null;
            for (MyViewHolder tmp : mHolderArray) {
                if (tmp.position == position) {
                    holder = tmp;
                    break;
                }
            }

            if (holder == null) return;

            View view = holder.binding.getRoot();
            int offset = 12; // margin
            int listHeight = binding.list.getHeight();
            if (view.getTop() < binding.list.getScrollY()) {
                // the clicked view is hiding on top
                binding.list.smoothScrollBy(0, view.getTop() - offset);
            } else if (view.getBottom() > binding.list.getScrollY() + listHeight) {
                // the clicked view is hiding on bottom
                binding.list.smoothScrollBy(0, view.getBottom() - listHeight + offset);
            }
        }

        private boolean onRecyclerLongClicked(View view, final int position) {
            startActivity(WordEditActivity.createIntent(
                    getApplicationContext(),
                    groupId,
                    getItemForPosition(position).getId()));
            return true;
        }

        class MyViewHolder extends RecyclerView.ViewHolder {

            private final RowWordBinding binding;
            private long position;

            private MyViewHolder(RowWordBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void setPosition(int position) {
                this.position = position;
            }
        }

        private void remove(final int position) {
            orma.transactionNonExclusiveSync(
                    () -> wordDao.remove(getItemForPosition(position)));

            refreshData();
            notifyItemRemoved(position);

            // The database selector does not change soon,
            // so the time to sync display info should be delayed just a second.
            mHandler.sendEmptyMessageDelayed(MSG_CHANGE_DISPLAY_LISTID, 500);
        }

        private void refreshData() {
            if (searchWord.isEmpty()) {
                selector = wordDao.findAllByGroupId(groupId);
            } else {
                selector = wordDao.likeQuery(searchWord, groupId);
            }
            count = selector.count();
            mCash.clear();
            notifyDataSetChanged();

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

        private ItemTouchHelper.SimpleCallback getCallback() {
            return callback;
        }

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                new AlertDialog.Builder(WordListActivity.this)
                        .setTitle(R.string.dialog_confirm_title)
                        .setMessage(R.string.dialog_confirm_msg_delete)
                        .setPositiveButton(R.string.delete,
                                (dialog1, which) -> {
                            int swipedPosition = viewHolder.getAdapterPosition();
                            remove(swipedPosition);
                        })
                        .setNegativeButton(R.string.cancel,
                                (dialog12, which) -> {
                            // nothing to do
                            notifyDataSetChanged();
                        })
                        .show();
            }
        };

        private final int MSG_CHANGE_DISPLAY_LISTID = 0;
        private final int MSG_SCROLL_TO = 1;
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case MSG_CHANGE_DISPLAY_LISTID:
                        refreshListId();
                        break;

                    case MSG_SCROLL_TO:
                        scrollTo(msg.arg1);
                        break;
                }
            }
        };

        private void search(String value) {
            searchWord = value;
            refreshData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_word_menu, menu);
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
                        REQUEST_CODE_ADD);
                return true;

            case R.id.menu_preference:
                startActivity(PreferenceActivity.createIntent(this));
                return true;

            case R.id.menu_import:
                startActivityForResult(
                        SelectCSVActivity.createIntent(this),
                        REQUEST_CODE_IMPORT);
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
