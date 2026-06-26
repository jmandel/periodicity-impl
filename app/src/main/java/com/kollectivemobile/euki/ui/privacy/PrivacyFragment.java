package com.kollectivemobile.euki.ui.privacy;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentPrivacyBinding;
import com.kollectivemobile.euki.manager.AppSettingsManager;
import com.kollectivemobile.euki.manager.PrivacyContentManager;
import com.kollectivemobile.euki.manager.PrivacyManager;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.Dialogs;
import com.kollectivemobile.euki.utils.Constants;

import javax.inject.Inject;

public class PrivacyFragment extends BaseFragment {
    @Inject
    PrivacyContentManager mPrivacyContentManager;
    @Inject
    PrivacyManager mPrivacyManager;
    @Inject
    AppSettingsManager mAppSettingsManager;

    private FragmentPrivacyBinding binding;
    private Integer mRecurringCounter = 4;
    private Boolean isFirstTime = true;

    public static PrivacyFragment newInstance() {
        Bundle args = new Bundle();
        PrivacyFragment fragment = new PrivacyFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ((App) getActivity().getApplication()).getAppComponent().inject(this);
        }
        setUIElements();
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentPrivacyBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_privacy, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePinOptions();
    }

    @Override
    public String getTitle() {
        return getString(R.string.privacy);
    }

    private void setUIElements() {
        binding.dbRecurring.setMax(mRecurringCounter);
        binding.dbRecurring.incrementProgressBy(1);
        binding.dbRecurring.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        binding.swchRecurring.setOnCheckedChangeListener(mOnCheckedChangeListener);
        isFirstTime = mPrivacyManager.getRecurringType() != null;

        binding.bDeletteAllData.setOnClickListener(v -> deleteAllData());
        binding.bSetPin.setOnClickListener(v -> setPin());
        binding.bRemovePin.setOnClickListener(v -> removePin());
        binding.bPricavyFaqs.setOnClickListener(v -> showPrivacyFaqs());
        binding.bPrivacyStatement.setOnClickListener(v -> showPrivacyStatement());
        binding.bPrivacyBestPractices.setOnClickListener(v -> showPrivacyBestPractices());

        updateUIElements();
    }

    private void deleteAllData() {
        Dialogs.createSimpleDialog(getActivity(), null, getString(R.string.confirm_delete_all_now), getString(R.string.ok), true, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mPrivacyManager.removeAllData();
            }
        }).show();
    }

    private void setPin() {
        startActivity(PrivacyPinSetupActivity.makeIntent(getActivity()));
    }

    private void removePin() {
        Dialogs.createSimpleDialog(getActivity(), null, getString(R.string.confirm_remove_pin), getString(R.string.ok), true, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                mAppSettingsManager.savePinCode(null);
                updatePinOptions();
            }
        }).show();
    }

    private void showPrivacyFaqs() {
        showContentItem(mPrivacyContentManager.getPrivacyFAQs());
    }

    private void showPrivacyStatement() {
        showContentItem(mPrivacyContentManager.getPrivacyStatement());
    }

    private void showPrivacyBestPractices() {
        showContentItem(mPrivacyContentManager.getPrivacyBestPractices());
    }

    private void updateUIElements() {
        Constants.DeleteRecurringType recurringType = mPrivacyManager.getRecurringType();
        binding.tvRecurring.setText(getTitle(recurringType));
        binding.swchRecurring.setChecked(recurringType != null);
        binding.dbRecurring.setProgress(recurringType == null ? 0 : recurringType.ordinal());
        binding.llRecurringContainer.setVisibility(binding.swchRecurring.isChecked() ? View.VISIBLE : View.GONE);
    }

    private String getTitle(Integer index) {
        Constants.DeleteRecurringType recurringType = Constants.DeleteRecurringType.values[index];
        return getTitle(recurringType);
    }

    private String getTitle(Constants.DeleteRecurringType type) {
        if (type == null) {
            return "";
        }
        return getString(type.getTextRestId());
    }

    private void updatePinOptions() {
        Boolean hasPinCode = mAppSettingsManager.getPinCode() != null;
        binding.bSetPin.setText(hasPinCode ? getString(R.string.reset_pin) : getString(R.string.set_pin));
        binding.llRemovePin.setVisibility(hasPinCode ? View.VISIBLE : View.GONE);
        binding.vRemovePin.setVisibility(hasPinCode ? View.VISIBLE : View.GONE);
    }

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                binding.tvRecurring.setText(getTitle(progress));

                Constants.DeleteRecurringType recurringType = Constants.DeleteRecurringType.values[progress];
                if (recurringType != mPrivacyManager.getRecurringType()) {
                    mPrivacyManager.saveRecurringData(recurringType);
                }
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    };

    private CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
            if (isFirstTime) {
                isFirstTime = false;
                return;
            }

            if (isChecked) {
                Dialogs.createTwoOptionsDialog(getActivity(), null, getString(R.string.weekly_recurring_info), getString(R.string.ok), getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mPrivacyManager.saveRecurringData(Constants.DeleteRecurringType.WEEKLY);
                        updateUIElements();
                    }
                }, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        updateUIElements();
                    }
                }).show();
            } else {
                mPrivacyManager.saveRecurringData(null);
                updateUIElements();
            }
        }
    };
}
