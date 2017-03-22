package jp.honkot.exercize.basic.wwword.activity;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import javax.inject.Inject;

import jp.honkot.exercize.basic.wwword.R;
import jp.honkot.exercize.basic.wwword.dao.GroupDao;
import jp.honkot.exercize.basic.wwword.dao.OxfordDictionaryDao;
import jp.honkot.exercize.basic.wwword.databinding.ActivityEditGroupBinding;
import jp.honkot.exercize.basic.wwword.model.Group;

public class GroupEditActivity extends BaseActivity implements View.OnClickListener {

    public static final String EXTRA_GROUP_ID = "EXTRA_GROUP_ID";
    private ActivityEditGroupBinding binding;
    private Group mGroup;

    @Inject
    GroupDao groupDao;

    @Inject
    OxfordDictionaryDao oxfordDictionaryDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getComponent().inject(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_group);
        initialize();
    }

    private void initialize() {
        // Get and set initial values
        long groupId = getIntent().getLongExtra(EXTRA_GROUP_ID, 0);
        if (groupId != 0) {
            mGroup = groupDao.findById(groupId);
        }
        if (mGroup == null) {
            mGroup = new Group();
        }
        binding.setGroup(mGroup);
        updateButtonState();

        // Set some listeners
        binding.nameEditText.addTextChangedListener(mTextWatcher);
        binding.registerButton.setOnClickListener(this);

        // Set Action bar title
        if (mGroup.getId() > 0) {
            getSupportActionBar().setTitle(
                    getString(R.string.activity_edit_group_label_edit, mGroup.getName()));
        } else {
            getSupportActionBar().setTitle(
                    getString(R.string.activity_edit_group_label_add));

        }
    }

    private void updateButtonState() {
        binding.registerButton.setEnabled(
                !binding.nameEditText.getText().toString().isEmpty());
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
            case R.id.registerButton:
                register();
                break;
        }
    }

    private void register() {
        mGroup.setName(binding.nameEditText.getText().toString());
        mGroup.setListId(groupDao.getMaximumListId() + 1);
        mGroup.setNotify(true);

        // Check the error just in case
        if (mGroup.allowRegister()) {
            long result = 0;
            if (mGroup.getId() == 0) {
                result = groupDao.insert(mGroup);
            } else {
                result = groupDao.update(mGroup);
            }

            if (result != 0) {
                setResult(RESULT_OK);
                finish();
            } else {
                // error
            }
        }
    }

    public static Intent createIntent(Context context) {
        Intent intent = new Intent(context, GroupEditActivity.class);
        return intent;
    }
}
