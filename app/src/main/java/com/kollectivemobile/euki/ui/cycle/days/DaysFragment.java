package com.kollectivemobile.euki.ui.cycle.days;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentCycleDaysBinding;
import com.kollectivemobile.euki.manager.CycleManager;
import com.kollectivemobile.euki.model.CycleDayItem;
import com.kollectivemobile.euki.model.CyclePeriodData;
import com.kollectivemobile.euki.networking.EukiCallback;
import com.kollectivemobile.euki.networking.ServerError;
import com.kollectivemobile.euki.ui.common.BaseFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;


public class DaysFragment extends BaseFragment implements DaysFragmentListener {
    @Inject
    CycleManager mCycleManager;

    private FragmentCycleDaysBinding binding;
    private List<CycleDayItem> mItems = new ArrayList<>();
    private DaysFragmentListener mListener;
    private Boolean mIsFirstTime = true;

    public static DaysFragment newInstance(DaysFragmentListener listener) {
        Bundle args = new Bundle();
        DaysFragment fragment = new DaysFragment();
        fragment.mListener = listener;
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ((App) getActivity().getApplication()).getAppComponent().inject(this);
        }

        setUIElements();
        requestData();
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentCycleDaysBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cycle_days, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mIsFirstTime) {
            mIsFirstTime = false;
            return;
        }

        refreshData();
    }

    private void requestData() {
        mCycleManager.requestCycleItems(new EukiCallback<List<CycleDayItem>>() {
            @Override
            public void onSuccess(final List<CycleDayItem> cycleDayItems) {
                mItems = cycleDayItems;
                binding.rvMain.updateData(cycleDayItems);

                // Fetch CyclePeriodData to get currentDayCycle and cycleStartDate
                fetchCyclePeriodData();


                // Request CyclePeriodData
                fetchCyclePeriodData();

                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> {
                    if (cycleDayItems != null) {
                        binding.rvMain.scrollToPosition(cycleDayItems.size());
                        binding.rvMain.smoothScrollToPosition(cycleDayItems.size() + 1);
                    }
                }, 100);
            }

            @Override
            public void onError(ServerError serverError) {
                // Handle error
            }
        });
    }

    private void fetchCyclePeriodData() {
        mCycleManager.requestCyclePeriodData(new EukiCallback<CyclePeriodData>() {
            @Override
            public void onSuccess(CyclePeriodData cyclePeriodData) {
                Integer currentDayCycle = cyclePeriodData.getCurrentDayCycle();
                binding.rvMain.setCurrentDayCycle(currentDayCycle);

                if (currentDayCycle != null && currentDayCycle > 0) {
                    // Calculate the start date of the current cycle period
                    Date today = new Date();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(today);
                    calendar.add(Calendar.DAY_OF_YEAR, - (currentDayCycle - 1)); // Subtract days
                    Date cycleStartDate = calendar.getTime();

                    // Pass the cycleStartDate to the RecyclerView
                    binding.rvMain.setCurrentCycleStartDate(cycleStartDate);
                }
            }

            @Override
            public void onError(ServerError serverError) {
                // Handle error
            }
        });
    }

    private void refreshData() {
        mCycleManager.requestCycleItems(new EukiCallback<List<CycleDayItem>>() {
            @Override
            public void onSuccess(final List<CycleDayItem> cycleDayItems) {
                mItems = cycleDayItems;
                binding.rvMain.updateData(cycleDayItems);

                // Fetch CyclePeriodData to get currentDayCycle and cycleStartDate
                fetchCyclePeriodData();


                mCycleManager.requestCyclePeriodData(new EukiCallback<CyclePeriodData>() {
                    @Override
                    public void onSuccess(CyclePeriodData cyclePeriodData) {
                        Integer currentDayCycle = cyclePeriodData.getCurrentDayCycle();
                        binding.rvMain.setCurrentDayCycle(currentDayCycle);

                        mListener.itemChanged(mItems.get(binding.rvMain.getCurrentIndex()));
                        binding.rvMain.smoothScrollToPosition(binding.rvMain.getCurrentIndex() + 1);
                    }

                    @Override
                    public void onError(ServerError serverError) {
                        // Handle error appropriately
                    }
                });
            }

            @Override
            public void onError(ServerError serverError) {
                // Handle error appropriately
            }
        });
    }

    private void setUIElements() {

        HorizontalCarouselRecyclerView rvMain = binding.rvMain;
        ImageView ivLeft = binding.ivLeft;
        ImageView ivRight = binding.ivRight;

        rvMain.setListener(this);
        ivRight.setVisibility(View.GONE);

        ivLeft.setOnClickListener(view -> rvMain.smoothScrollToPosition(rvMain.getCurrentIndex()));
        ivRight.setOnClickListener(view -> rvMain.smoothScrollToPosition(rvMain.getCurrentIndex() + 2));
    }

    @Override
    public void itemChanged(CycleDayItem item) {
        binding.ivLeft.setVisibility(item == mItems.get(0) ? View.GONE : View.VISIBLE);
        binding.ivRight.setVisibility(item == mItems.get(mItems.size() - 1) ? View.GONE : View.VISIBLE);

        int resId = (item.getCalendarItem() != null && item.getCalendarItem().hasData()) ? R.string.day_summary_data_title : item.isToday() ? R.string.day_summary_empty_today_title : R.string.day_summary_empty_past_title;
        binding.tvInfo.setText(getString(resId));

        mListener.itemChanged(item);
    }
}
