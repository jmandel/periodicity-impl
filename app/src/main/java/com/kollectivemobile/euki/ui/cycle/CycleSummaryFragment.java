package com.kollectivemobile.euki.ui.cycle;

import android.graphics.Canvas;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentCycleSummaryBinding;
import com.kollectivemobile.euki.manager.AppSettingsManager;
import com.kollectivemobile.euki.manager.CycleManager;
import com.kollectivemobile.euki.model.CyclePeriodData;
import com.kollectivemobile.euki.model.CyclePeriodItem;
import com.kollectivemobile.euki.networking.EukiCallback;
import com.kollectivemobile.euki.networking.ServerError;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.SwipeController;
import com.kollectivemobile.euki.ui.common.SwipeControllerActions;
import com.kollectivemobile.euki.ui.common.adapter.CycleSummaryAdapter;
import com.kollectivemobile.euki.utils.Utils;

import java.util.Locale;

import javax.inject.Inject;

public class CycleSummaryFragment extends BaseFragment {
    @Inject CycleManager mCycleManager;
    @Inject AppSettingsManager mAppSettingsManager;

    private FragmentCycleSummaryBinding binding;
    private CycleSummaryAdapter mAdapter;
    private CyclePeriodData mCyclePeriodData;
    private SwipeController swipeController = null;

    public static CycleSummaryFragment newInstance() {
        Bundle args = new Bundle();
        CycleSummaryFragment fragment = new CycleSummaryFragment();
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
        binding = FragmentCycleSummaryBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    public void onResume() {
        super.onResume();
        requestData();
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cycle_summary, container, false);
    }

    private void setUIElements() {
        mAdapter = new CycleSummaryAdapter(getContext());
        binding.rvMain.setAdapter(mAdapter);
        binding.rvMain.setLayoutManager(new LinearLayoutManager(getContext()));

        swipeController = new SwipeController(new SwipeControllerActions() {
            @Override
            public void onRightClicked(final int position) {
                CyclePeriodItem item = mCyclePeriodData.getItems().get(position);
                mCycleManager.deletePeriod(item, new EukiCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        requestData();
                    }

                    @Override
                    public void onError(ServerError serverError) {
                    }
                });
            }
        });

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(swipeController);
        itemTouchhelper.attachToRecyclerView(binding.rvMain);

        binding.rvMain.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
                swipeController.onDraw(c);
            }
        });
    }

    private void updateUIElements() {

        final String defaultValue = "-";
        final String formattedValueFormat = "%.1f";

        String cycleLength = (mCyclePeriodData != null && mCyclePeriodData.getAverageCycleLength() != null)
                ? String.format(Locale.US, formattedValueFormat, mCyclePeriodData.getAverageCycleLength())
                : defaultValue;

        String variation = (mCyclePeriodData != null && mCyclePeriodData.getVariation() != null)
                ? String.valueOf(mCyclePeriodData.getVariation())
                : defaultValue;

        String periodLength = (mCyclePeriodData != null && mCyclePeriodData.getAveragePeriodLength() != null)
                ? String.format(Locale.US, formattedValueFormat, mCyclePeriodData.getAveragePeriodLength())
                : defaultValue;

        String currentDay = (mCyclePeriodData != null && mCyclePeriodData.getCurrentDayCycle() != null)
                ? String.valueOf(mCyclePeriodData.getCurrentDayCycle())
                : defaultValue;

        binding.tvCycleLength.setText(String.format(
                Locale.US,
                getString(R.string.cycle_summary_days_format),
                cycleLength,
                Utils.suffix(cycleLength)
        ));

        binding.tvVariation.setText(String.format(
                Locale.US,
                getString(R.string.cycle_summary_days_format),
                variation,
                Utils.suffix(variation)
        ));

        binding.tvPeriodLength.setText(String.format(
                Locale.US,
                getString(R.string.cycle_summary_days_format),
                periodLength,
                Utils.suffix(periodLength)
        ));
        binding.tvCurrentCycle.setText(String.format(getString(R.string.your_current_cycle_day), currentDay));
    }
    private void requestData() {
        mCycleManager.requestCyclePeriodData(new EukiCallback<CyclePeriodData>() {
            @Override
            public void onSuccess(CyclePeriodData cyclePeriodData) {
                mCyclePeriodData = cyclePeriodData;
                updateUIElements();
                mAdapter.update(cyclePeriodData);
            }

            @Override
            public void onError(ServerError serverError) {
            }
        });
    }
}
