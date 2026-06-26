package com.kollectivemobile.euki.ui.calendar;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentCalendarBinding;
import com.kollectivemobile.euki.manager.CalendarManager;
import com.kollectivemobile.euki.manager.ReminderManager;
import com.kollectivemobile.euki.model.CalendarFilter;
import com.kollectivemobile.euki.model.database.entity.ReminderItem;
import com.kollectivemobile.euki.networking.EukiCallback;
import com.kollectivemobile.euki.networking.ServerError;
import com.kollectivemobile.euki.ui.calendar.filter.FilterActivity;
import com.kollectivemobile.euki.ui.calendar.reminders.RemindersActivity;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.adapter.CalendarFragmentAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import javax.inject.Inject;

public class CalendarFragment extends BaseFragment {

    @Inject CalendarManager mCalendarManager;
    @Inject ReminderManager mReminderManager;

    private FragmentCalendarBinding binding;

    public static CalendarFragment newInstance() {
        Bundle args = new Bundle();
        CalendarFragment fragment = new CalendarFragment();
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
        setHasOptionsMenu(true); // Ensure this fragment participates in the options menu
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();  // This will trigger onCreateOptionsMenu
        }
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_calendar, menu);

        final com.kollectivemobile.euki.ui.common.views.MenuItem filterItem =
                (com.kollectivemobile.euki.ui.common.views.MenuItem) menu.findItem(R.id.item_filter).getActionView();
        if (filterItem != null) {
            filterItem.setTitle(getString(R.string.filter));
            filterItem.updateCount(mCalendarManager.getCalendarFilter().getFiltersCount());
            filterItem.setOnClickListener(view -> startActivity(FilterActivity.makeIntent(getActivity(), mCalendarManager.getCalendarFilter())));
        }

        final com.kollectivemobile.euki.ui.common.views.MenuItem reminderItem =
                (com.kollectivemobile.euki.ui.common.views.MenuItem) menu.findItem(R.id.item_reminders).getActionView();
        if (reminderItem != null) {
            reminderItem.setTitle(getString(R.string.reminders));
            reminderItem.setOnClickListener(view -> startActivity(RemindersActivity.makeIntent(getActivity())));

            mReminderManager.getReminders(new EukiCallback<>() {
                @Override
                public void onSuccess(List<ReminderItem> reminderItems) {
                    if (reminderItem != null) {
                        reminderItem.updateCount(reminderItems.size());
                    }
                }

                @Override
                public void onError(ServerError serverError) {
                    if (reminderItem != null) {
                        reminderItem.updateCount(0);
                    }
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.item_reminders) {
            startActivity(RemindersActivity.makeIntent(getActivity()));
            return true;
        } else if (itemId == R.id.item_filter) {
            startActivity(FilterActivity.makeIntent(getActivity(), mCalendarManager.getCalendarFilter()));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public String getTitle() {
        return getString(R.string.calendar);
    }

    private void setUIElements() {
        CalendarFragmentAdapter adapter = new CalendarFragmentAdapter(getActivity(), getChildFragmentManager(), mCalendarManager.getCalendarFilter());
        binding.vpMain.setAdapter(adapter);
        binding.vpMain.setPagingEnabled(false);

        binding.sbSections.setListener(index -> binding.vpMain.setCurrentItem(index));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void update(CalendarFilter calendarFilter) {
        mCalendarManager.updateCalendarFilter(calendarFilter);
    }
}

