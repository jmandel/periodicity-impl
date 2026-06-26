package com.kollectivemobile.euki.ui.cycle;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentCycleDaySummaryBinding;
import com.kollectivemobile.euki.manager.converter.SelectableValueConverter;
import com.kollectivemobile.euki.model.CycleDayItem;
import com.kollectivemobile.euki.model.SelectableValue;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.FragmentManagerHelper;
import com.kollectivemobile.euki.ui.common.adapter.CycleDaySummaryAdapter;
import com.kollectivemobile.euki.ui.cycle.days.DaysFragment;
import com.kollectivemobile.euki.ui.cycle.days.DaysFragmentListener;
import com.kollectivemobile.euki.ui.track.TrackActivity;

import java.util.List;

public class DaySummaryFragment extends BaseFragment implements DaysFragmentListener, CycleDaySummaryAdapter.OnItemClickListener {

    private CycleDaySummaryAdapter mAdapter;
    private CycleDayItem mCurrentItem;
    private FragmentCycleDaySummaryBinding binding;

    public static DaySummaryFragment newInstance() {
        Bundle args = new Bundle();
        DaySummaryFragment fragment = new DaySummaryFragment();
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
        binding = FragmentCycleDaySummaryBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cycle_day_summary, container, false);
    }

    private void setUIElements() {
        binding.llEmpty.setVisibility(View.GONE);
        FragmentManagerHelper mFragmentManagerHelper = new FragmentManagerHelper(getChildFragmentManager());
        mFragmentManagerHelper.replace(R.id.fl_header_content, DaysFragment.newInstance(this), false);

        mAdapter = new CycleDaySummaryAdapter(getContext(), this);
        binding.rvMain.setAdapter(mAdapter);

        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 3);
        binding.rvMain.setLayoutManager(layoutManager);

        binding.btnLogNow.setOnClickListener(v -> logNow());
    }

    @Override
    public void onItemClick(SelectableValue item) {
        logNow();
    }

    @Override
    public void itemChanged(CycleDayItem item) {
        mCurrentItem = item;
        List<SelectableValue> items = SelectableValueConverter.convert(item.getCalendarItem());
        mAdapter.update(items);

        binding.llEmpty.setVisibility(item.getCalendarItem() != null && item.getCalendarItem().hasData() ? View.GONE : View.VISIBLE);
        binding.ivEmpty.setImageResource(item.isToday() ? R.drawable.ic_day_summary_empty : R.drawable.icon_calendar_past);
    }

    private void logNow() {
        if (mCurrentItem == null) {
            return;
        }

        CalendarItem calendarItem = mCurrentItem.getCalendarItem();
        if (calendarItem == null) {
            calendarItem = new CalendarItem(mCurrentItem.getDate());
        }

        startActivity(TrackActivity.makeIntent(getContext(), mCurrentItem.getDate(), calendarItem));
    }
}
