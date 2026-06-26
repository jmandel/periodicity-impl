package com.kollectivemobile.euki.ui.calendar;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentCalendarDayBinding;
import com.kollectivemobile.euki.manager.CalendarManager;
import com.kollectivemobile.euki.model.FilterItem;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.networking.EukiCallback;
import com.kollectivemobile.euki.networking.ServerError;
import com.kollectivemobile.euki.ui.calendar.appointments.AppointmentsActivity;
import com.kollectivemobile.euki.ui.calendar.appointments.FutureAppointmentActivity;
import com.kollectivemobile.euki.ui.calendar.reminders.RemindersActivity;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.adapter.CalendarDayAdapter;
import com.kollectivemobile.euki.ui.track.TrackActivity;
import com.kollectivemobile.euki.utils.DateUtils;
import com.kollectivemobile.euki.utils.strings.StringUtils;

import java.util.Date;

import javax.inject.Inject;

public class CalendarDayFragment extends BaseFragment {
    @Inject CalendarManager mCalendarManager;

    private FragmentCalendarDayBinding binding;
    private CalendarItem mCalendarItem;
    private CalendarDayAdapter mCalendarDayAdapter;

    public static CalendarDayFragment newInstance(CalendarItem calendarItem) {
        Bundle args = new Bundle();
        CalendarDayFragment fragment = new CalendarDayFragment();
        fragment.mCalendarItem = calendarItem;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((App)getActivity().getApplication()).getAppComponent().inject(this);
        setUIElements();
        updateUIElements();
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentCalendarDayBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    public void onResume() {
        super.onResume();
        requestCalendarItem();
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar_day, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Date date = mCalendarItem.getDate();
        Date nowDate = new Date();
        Boolean isFutureDate = true;
        if (DateUtils.isSameDate(date, nowDate) || nowDate.after(date)) {
            isFutureDate = false;
        }

        if (!isFutureDate && mCalendarItem.hasData()) {
            inflater.inflate(R.menu.menu_edit, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.item_edit) {
            startActivity(TrackActivity.makeIntent(getContext(), mCalendarItem.getDate(), mCalendarItem));
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean showBack() {
        return true;
    }

    @Override
    public String getTitle() {
        return " ";
    }


    private void setUIElements() {
        mCalendarDayAdapter = new CalendarDayAdapter(getContext());
        binding.rvMain.setAdapter(mCalendarDayAdapter);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3, RecyclerView.VERTICAL, false);
        binding.rvMain.setLayoutManager(layoutManager);

        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int i) {
                Object object = mCalendarDayAdapter.getObjects().get(i);
                if (object instanceof String) {
                    String value = (String)object;
                    if (!value.isEmpty()) {
                        return 3;
                    }
                }
                if (object instanceof FilterItem) {
                    return 3;
                }
                return 1;
            }
        });

        binding.btnSetReminder.setOnClickListener(v -> setReminder());
        binding.btnSetReminder1.setOnClickListener(v -> setReminder());
        binding.btnSetAppointment.setOnClickListener(v -> setAppointment());
        binding.btnSetAppointment1.setOnClickListener(v -> setAppointment());
        binding.btnLogNow.setOnClickListener(v -> logNow());
    }

    private void setReminder() {
        startActivity(RemindersActivity.makeIntent(getActivity()));
    }

    private void setAppointment() {
        if (mCalendarItem.getAppointments().isEmpty()) {
            startActivity(FutureAppointmentActivity.makeIntent(getActivity(), mCalendarItem.getDate()));
        } else {
            startActivity(AppointmentsActivity.makeIntent(getContext(), mCalendarItem.getDate()));
        }
    }

    private void logNow() {
        startActivity(TrackActivity.makeIntent(getContext(), mCalendarItem.getDate(), mCalendarItem));
    }

    private void updateUIElements() {
        if (mCalendarItem == null) {
            return;
        }

        Date date = mCalendarItem.getDate();
        Date nowDate = new Date();
        Boolean isFutureDate = true;
        if (DateUtils.isSameDate(date, nowDate) || nowDate.after(date)) {
            isFutureDate = false;
        }

        String dateTitle = StringUtils.capitalizeAll(DateUtils.toString(date, DateUtils.eeeMMMMdd));
        binding.tvDay.setText(dateTitle);

        binding.rlLoggedData.setVisibility(View.GONE);
        binding.llMainBottom.setVisibility(View.GONE);
        binding.llPast.setVisibility(View.GONE);
        binding.rlFuture.setVisibility(View.GONE);

        if (isFutureDate) {
            if (mCalendarItem.hasData()) {
                binding.rlLoggedData.setVisibility(View.VISIBLE);
                binding.llMainBottom.setVisibility(View.VISIBLE);
            } else {
                binding.rlFuture.setVisibility(View.VISIBLE);
            }
        } else {
            if (mCalendarItem.hasData()) {
                binding.rlLoggedData.setVisibility(View.VISIBLE);
            } else {
                binding.llPast.setVisibility(View.VISIBLE);
            }
        }

        mCalendarDayAdapter.updateObjects(mCalendarItem);

        setHasOptionsMenu(true);
        getActivity().invalidateOptionsMenu();
    }

    private void requestCalendarItem() {
        mCalendarManager.getCalendarItem(mCalendarItem.getDate(), new EukiCallback<CalendarItem>() {
            @Override
            public void onSuccess(CalendarItem calendarItem) {
                mCalendarItem = calendarItem;
                updateUIElements();
            }

            @Override
            public void onError(ServerError serverError) {
            }
        });
    }
}
