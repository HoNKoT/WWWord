package jp.honkot.exercize.basic.wwword.activity;

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
import jp.honkot.exercize.basic.wwword.databinding.ActivityListGroupBinding;
import jp.honkot.exercize.basic.wwword.databinding.RowGroupBinding;
import jp.honkot.exercize.basic.wwword.model.Group;
import jp.honkot.exercize.basic.wwword.model.Group_Selector;
import jp.honkot.exercize.basic.wwword.model.OrmaDatabase;
import jp.honkot.exercize.basic.wwword.model.Preference;
import jp.honkot.exercize.basic.wwword.util.CheckPermissionUtil;
import jp.honkot.exercize.basic.wwword.util.Debug;
import jp.honkot.exercize.basic.wwword.util.ImportCSVUtil;
import jp.honkot.exercize.basic.wwword.util.SharedPreferenceUtil;

public class GroupListActivity extends BaseActivity {

    RecyclerAdapter adapter;
    ActivityListGroupBinding binding;
    ItemTouchHelper itemTouchHelper;

    private static final int REQUEST_CODE_ADD = 1;

    @Inject
    GroupDao groupDao;

    @Inject
    WordDao wordDao;

    @Inject
    PreferenceDao preferenceDao;

    @Inject
    OrmaDatabase orma;

    ImportCSVUtil importCSVUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getComponent().inject(this);

        CheckPermissionUtil.checkPermission(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_list_group);

        // initialize if needed
        if (SharedPreferenceUtil.isFirstBoot(this)) {
            // make preference
            Preference pref = preferenceDao.getPreference();
            if (pref == null) {
                // generate initial preference
                Preference newPref = new Preference();
                newPref.setNotificationInterval(Preference.DEFAULT_INTERVAL);
                newPref.setPopup(true);
                preferenceDao.insert(newPref);
            }

            // make default group
            Group group = groupDao.relation().selector().valueOrNull();
            if (group == null) {
                group = new Group();
                group.setListId(1);
                group.setName("Useful phrasal verbs");
                group.setNotify(true);
                long id = groupDao.insert(group);
                group.setId(id);
            }

            // make default words
            importCSVUtil = new ImportCSVUtil(this, wordDao);
            importCSVUtil.readCSV(R.raw.testcsv, new ImportCSVUtil.OnReadFinishListener() {
                @Override
                public void onError() {
                    // nothing to do.
                }

                @Override
                public void onFinish() {
                    SharedPreferenceUtil.doneFirstBoot(getApplicationContext());
                    initialize();
                }
            }, group);

        } else {
            initialize();
        }
    }

    private void initialize() {
        adapter = new RecyclerAdapter();

        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        // set swipe animation
        itemTouchHelper = new ItemTouchHelper(adapter.getCallback());
        itemTouchHelper.attachToRecyclerView(binding.list);

        // Set Action bar title
        getSupportActionBar().setTitle(
                getString(R.string.activity_list_group_label));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD && resultCode == RESULT_OK) {
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

        private Group_Selector selector;
        private int count = 0;
        private SparseArray<Group> mCash = new SparseArray<>();
        private ArrayList<MyViewHolder> mHolderArray = new ArrayList<>();

        private RecyclerAdapter() {
            selector = groupDao.findAll();
            count = selector.count();
        }

        @Override
        public RecyclerAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            RowGroupBinding itemBinding = RowGroupBinding.inflate(layoutInflater, parent, false);
            return new MyViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            Group item = getItemForPosition(position);
            holder.binding.setGroup(item);
            holder.binding.rowListId.setVisibility(Debug.isDBG ? View.VISIBLE : View.GONE);
            holder.binding.rowRoot.setOnClickListener(
                    v -> onRecyclerClicked(holder.binding.getRoot(), holder.getLayoutPosition()));

            if (!mHolderArray.contains(holder)) {
                mHolderArray.add(holder);
            }
        }

        @Override
        public int getItemCount() {
            //TODO fix タイミングによってずれちゃう
//            if (selector != null) {
//                return count;
//            } else {
//                return 0;
//            }
            return selector.count();
        }

        @Nullable
        private Group getItemForPosition(int position) {
            //TODO fix タイミングによってずれちゃう
            if (position < selector.count()) {
                Group cashWord = mCash.get(position);
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
            startActivity(WordListActivity.createIntent(
                    getApplicationContext(),
                    getItemForPosition(position).getId()));
        }

        protected class MyViewHolder extends RecyclerView.ViewHolder {

            private final RowGroupBinding binding;

            private MyViewHolder(RowGroupBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        private void remove(final int position) {
            orma.transactionNonExclusiveSync(
                    () -> groupDao.remove(getItemForPosition(position)));

            refreshData();
            notifyItemRemoved(position);

            // The database selector does not change soon,
            // so the time to sync display info should be delayed just a second.
            mHandler.sendEmptyMessageDelayed(MEG_CHANGE_DISPLAY_LISTID, 500);
        }

        private void refreshData() {
            selector = groupDao.findAll();
            count = selector.count();
            mCash.clear();
        }

        private void refreshListId() {
            for (MyViewHolder holder : mHolderArray) {
                int position = holder.getLayoutPosition();
                Debug.Log("refreshListId " + position);
                if (position >= 0) {
                    Group group = getItemForPosition(position);
                    if (group != null) {
                        holder.binding.rowListId.setText(
                                group.getDisplayListId());
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
                new AlertDialog.Builder(GroupListActivity.this)
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
        inflater.inflate(R.menu.list_group_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_add:
                startActivityForResult(GroupEditActivity.createIntent(this), REQUEST_CODE_ADD);
                return true;

            case R.id.menu_preference:
                startActivity(PreferenceActivity.createIntent(this));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
