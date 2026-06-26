package com.kollectivemobile.euki.ui.calendar;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentCalendarMonthBinding;
import com.kollectivemobile.euki.manager.CalendarManager;
import com.kollectivemobile.euki.model.CalendarFilter;
import com.kollectivemobile.euki.model.MonthItem;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.networking.EukiCallback;
import com.kollectivemobile.euki.networking.ServerError;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.adapter.CalendarMonthAdapter;
import com.kollectivemobile.euki.ui.common.listeners.CalendarDayListener;
import com.kollectivemobile.euki.utils.DateUtils;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class CalendarMonthFragment extends BaseFragment implements CalendarDayListener {
    @Inject
    CalendarManager mCalendarManager;

    private FragmentCalendarMonthBinding binding;
    private Map<String, CalendarItem> mCalendarItems;
    private List<MonthItem> mMonthItems;
    private MonthItem mTodayItem;
    private MonthItem mCurrentItem;
    private Integer mTodayPosition;
    private List<Range<Date>> predictionRange = new ArrayList<>();

    private CalendarMonthAdapter mAdapter;
    private CalendarMonthViewModel viewModel;

    public static CalendarMonthFragment newInstance(CalendarFilter calendarFilter) {
        Bundle args = new Bundle();
        CalendarMonthFragment fragment = new CalendarMonthFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ((App) getActivity().getApplication()).getAppComponent().inject(this);
        }

        viewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(requireActivity().getApplication(), this))
                .get(CalendarMonthViewModel.class);

        setUIElements();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentCalendarMonthBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_month, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        requestCalendarItems();
    }

    @Override
    public String getTitle() {
        return getString(R.string.calendar);
    }

    private void setUIElements() {
        mCalendarItems = new HashMap<>();
        Pair<List<MonthItem>, MonthItem> pair = MonthItem.getItems();
        mMonthItems = pair.first;
        mTodayItem = pair.second;
        mCurrentItem = pair.second;

        mAdapter = new CalendarMonthAdapter(getActivity(), this);
        binding.rvMain.setAdapter(mAdapter);
        final GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 7, RecyclerView.VERTICAL, false);
        binding.rvMain.setLayoutManager(layoutManager);

        binding.rvMain.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    int firstPosition = layoutManager.findFirstVisibleItemPosition();

                    MonthItem currentItem = mMonthItems.get(0);
                    for (MonthItem monthItem : mMonthItems) {
                        currentItem = monthItem;
                        if (monthItem.getMinIndex() - 2 > firstPosition) {
                            break;
                        }
                    }
                    mCurrentItem = currentItem;
                    showMonth(currentItem);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int i) {
                Object object = mAdapter.getObjects().get(i);
                if (object instanceof String) {
                    String value = (String) object;
                    if (!value.isEmpty()) {
                        return 7;
                    }
                }
                return 1;
            }
        });

        binding.tvToday.setOnClickListener(v -> todayAction());
        binding.ivMonthPrevious.setOnClickListener(v -> showPreviousMonth());
        binding.ivMonthNext.setOnClickListener(v -> showNextMonth());
    }

    private void requestCalendarItems() {
        mCalendarManager.getCalendarItems(new EukiCallback<>() {
            @Override
            public void onSuccess(final Map<String, CalendarItem> stringCalendarItemMap) {
                mCalendarManager.getPredictionRange(new EukiCallback<List<Range<Date>>>() {
                    @Override
                    public void onSuccess(List<Range<Date>> predictionRange) {
                        CalendarMonthFragment.this.predictionRange = predictionRange;
                        mCalendarItems.clear();
                        mCalendarItems.putAll(stringCalendarItemMap);
                        updateCalendar();
                    }

                    @Override
                    public void onError(ServerError serverError) {
                    }
                });
            }

            @Override
            public void onError(ServerError serverError) {
            }
        });
    }

    private void updateCalendar() {
        mAdapter.updateObjects(mMonthItems, mCalendarItems, mCalendarManager.getCalendarFilter(), predictionRange);
        Date selectedDay = viewModel.getSelectedDay();
        if (selectedDay != null) {
            showSelectedDay(selectedDay);
        } else {
            calculateTodayPosition();
            showToday();
        }
    }

    private void calculateTodayPosition() {
        Date today = new Date();
        for (int i = 0; i < mAdapter.getObjects().size(); i++) {
            Object object = mAdapter.getObjects().get(i);
            if (object instanceof Date) {
                Date date = (Date) object;
                if (DateUtils.isSameDate(today, date)) {
                    mTodayPosition = i - 14;
                    return;
                }
            }
        }
    }

    private void showToday() {
        if (mTodayPosition == null) {
            return;
        }
        ((GridLayoutManager) binding.rvMain.getLayoutManager()).scrollToPositionWithOffset(mTodayPosition, 0);
        showMonth(mTodayItem);
    }


    private void showSelectedDay(Date date) {
        MonthItem monthItem = getMonthItemForDate(date);
        if (monthItem != null) {
            int minIndex = monthItem.getMinIndex();
            int maxIndex = minIndex + monthItem.getNumDays() + monthItem.getFirstDayWeek() - 2;

            GridLayoutManager layoutManager = (GridLayoutManager) binding.rvMain.getLayoutManager();
            int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();

            if (minIndex >= firstVisiblePosition && maxIndex <= lastVisiblePosition) {
                return;
            }

            layoutManager.scrollToPositionWithOffset(minIndex, 0);
            showMonth(monthItem);
        }
    }



    private int getPositionForDate(Date date) {
        for (int i = 0; i < mAdapter.getObjects().size(); i++) {
            Object object = mAdapter.getObjects().get(i);
            if (object instanceof Date) {
                Date itemDate = (Date) object;
                if (DateUtils.isSameDate(itemDate, date)) {
                    return i;
                }
            }
        }
        return -1; // Return -1 if the date is not found
    }

    private MonthItem getMonthItemForDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        for (MonthItem monthItem : mMonthItems) {
            if (monthItem.getYear() == calendar.get(Calendar.YEAR) &&
                    monthItem.getMonth() == calendar.get(Calendar.MONTH)) {
                return monthItem;
            }
        }
        return null; // Return null if no matching MonthItem is found
    }


    private void changeCurrentMonth(Boolean isNext) {
        Integer currentIndex = mMonthItems.indexOf(mCurrentItem);
        Integer searchedIndex = currentIndex + (isNext ? 1 : -1);
        MonthItem item = mMonthItems.get(searchedIndex);
        showMonth(item);
        ((GridLayoutManager) binding.rvMain.getLayoutManager()).scrollToPositionWithOffset(item.getMinIndex(), 0);
    }

    private void showMonth(MonthItem item) {
        mCurrentItem = item;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, item.getYear());
        calendar.set(Calendar.MONTH, item.getMonth());
        String dateString = DateUtils.toString(calendar.getTime(), DateUtils.CalendarFormat);
        dateString = StringUtils.capitalize(dateString);
        binding.tvCurrentMonth.setText(dateString);
    }

    @Override
    public void daySelected(Date date) {
        viewModel.saveSelectedDay(date);
        mCalendarManager.getCalendarItem(date, new EukiCallback<>() {
            @Override
            public void onSuccess(CalendarItem calendarItem) {
                mInteractionListener.replaceFragment(CalendarDayFragment.newInstance(calendarItem), true);
            }

            @Override
            public void onError(ServerError serverError) {
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void update(CalendarFilter calendarFilter) {
        updateCalendar();
    }

    private void todayAction() {
        showToday();
    }

    private void showPreviousMonth() {
        changeCurrentMonth(false);
    }

    private void showNextMonth() {
        changeCurrentMonth(true);
    }
}
