package com.kollectivemobile.euki.ui.home.abortion;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentAbortionMedicalBinding;
import com.kollectivemobile.euki.manager.AbortionContentManager;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.Dialogs;
import com.kollectivemobile.euki.utils.DateUtils;
import com.kollectivemobile.euki.utils.TextUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class MedicalAbortionFragment extends BaseFragment {
    static private final String ScrollViewOffsetKey = "ScrollViewOffsetKey";

    @Inject AbortionContentManager mAbortionContentManager;

    private FragmentAbortionMedicalBinding binding;
    private Boolean mFirstDaySelected = false;
    private Boolean mPillOptionsSelected = false;
    private Date mDateSelected = new Date();
    private long mWeeks = -1;

    public static MedicalAbortionFragment newInstance() {
        Bundle args = new Bundle();
        MedicalAbortionFragment fragment = new MedicalAbortionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((App)getActivity().getApplication()).getAppComponent().inject(this);
        setUIElements();

        if (savedInstanceState != null) {
            final int position = savedInstanceState.getInt(ScrollViewOffsetKey);
            binding.svMain.post(() -> binding.svMain.scrollTo(0, position));
        }

        setupClickListeners();
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentAbortionMedicalBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_abortion_medical, container, false);
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (binding.svMain != null) {
            outState.putInt(ScrollViewOffsetKey, binding.svMain.getScrollY());
        }
    }

    @Override
    public boolean showBack() {
        return true;
    }

    private void setUIElements() {
        String content = getString(R.string.medical_abortion_content);
        Map<String, String> linksMap = new HashMap<>();
        linksMap.put(App.getContext().getString(R.string.world_health_organization), "https://www.who.int/publications/i/item/9789240039483");
        linksMap.put(App.getContext().getString(R.string.abortion_on_demand), "https://abortionondemand.org/");
        linksMap.put("ⓘ", "telehealth_popup_info");
        binding.tvContent.setText(TextUtils.getSpannable(content, linksMap, this, null));
        binding.tvContent.setMovementMethod(LinkMovementMethod.getInstance());

        linksMap = new HashMap<>();
        linksMap.put("Drugs.com", "http://www.drugs.com");
        linksMap.put(App.getContext().getString(R.string.women_help_women), "https://womenhelp.org/");
        linksMap.put(App.getContext().getString(R.string.women_on_waves), "https://www.womenonwaves.org/en/page/801/what-do-abortion-pills--mifepristone--ru486--mifeprex--mifegyne--look-like");
        binding.tvPillOptions.setText(TextUtils.getSpannable(getString(R.string.pills_content), linksMap, this, null));
        binding.tvPillOptions.setMovementMethod(LinkMovementMethod.getInstance());

        updateUIElements();
    }

    private void updateUIElements() {
        int resId = mFirstDaySelected ? R.drawable.icon_info_on : R.drawable.icon_info_off;
        binding.ivInfoFirstDay.setImageResource(resId);
        binding.tvInfoFirstDay.setVisibility(mFirstDaySelected ? View.VISIBLE : View.GONE);

        resId = mPillOptionsSelected ? R.drawable.icon_info_on : R.drawable.icon_info_off;
        binding.ivPillOptions.setImageResource(resId);
        binding.tvPillOptions.setVisibility(mPillOptionsSelected ? View.VISIBLE : View.GONE);

        binding.llPillsOptions.setVisibility(mWeeks == -1 ? View.GONE : View.VISIBLE);
        binding.tvWeeksPregnant.setVisibility(mWeeks == -1 ? View.GONE : View.VISIBLE);

        if (mWeeks != -1) {
            binding.tvPickDate.setText(DateUtils.toString(mDateSelected, DateUtils.DateLongFormat));
            String weeksText = String.format(getString(R.string.weeks_pregnand_format), DateUtils.weeksDiff(mDateSelected, new Date()));
            binding.tvWeeksPregnant.setText(weeksText);
        }
    }

    private void setupClickListeners() {
        binding.ivInfoFirstDay.setOnClickListener(v -> {
            mFirstDaySelected = !mFirstDaySelected;
            updateUIElements();
        });

        binding.ivPillOptions.setOnClickListener(v -> {
            mPillOptionsSelected = !mPillOptionsSelected;
            updateUIElements();
        });

        binding.rlPickDate.setOnClickListener(v -> pickDate());

        binding.llMiso.setOnClickListener(v -> misoSelected());

        binding.llMifeMiso.setOnClickListener(v -> mifeMisoSelected());
    }

    private void pickDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(mDateSelected);

        DatePickerDialog dpd = new DatePickerDialog(getContext(),
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar cal1 = Calendar.getInstance();
                    cal1.setTime(mDateSelected);
                    cal1.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    cal1.set(Calendar.MONTH, monthOfYear);
                    cal1.set(Calendar.YEAR, year);
                    mDateSelected = cal1.getTime();
                    mWeeks = DateUtils.weeksDiff(mDateSelected, new Date());
                    updateUIElements();

                    if (mWeeks > 12) {
                        showAbortionAfter12();
                    }
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dpd.getDatePicker().setMaxDate(new Date().getTime());
        dpd.show();
    }

    private void showAbortionAfter12() {
        Dialogs.createSimpleDialog(getActivity(), null, getString(R.string.abortion_over_12_weeks), getString(R.string.visit_website), true, (dialogInterface, i) -> linkClicked("https://www.who.int/publications/i/item/9789240039483")).show();
    }

    private void misoSelected() {
        if (mWeeks <= 12) {
            mInteractionListener.replaceFragment(AbortionInfoItemFragment.newInstance(mAbortionContentManager.getAbortionMiso12()), true);
        } else {
            showAbortionAfter12();
        }
    }

    private void mifeMisoSelected() {
        if (mWeeks <= 12) {
            mInteractionListener.replaceFragment(AbortionInfoItemFragment.newInstance(mAbortionContentManager.getAbortionMifeMiso12()), true);
        } else {
            showAbortionAfter12();
        }
    }
}

