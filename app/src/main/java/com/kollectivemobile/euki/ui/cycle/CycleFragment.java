package com.kollectivemobile.euki.ui.cycle;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewbinding.ViewBinding;
import androidx.viewpager.widget.ViewPager;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentCycleBinding;
import com.kollectivemobile.euki.manager.AppSettingsManager;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.adapter.CycleFragmentAdapter;
import com.kollectivemobile.euki.ui.common.views.SegmentedButtonListener;
import com.kollectivemobile.euki.ui.cycle.settings.SettingsActivity;

import javax.inject.Inject;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class CycleFragment extends BaseFragment {
    @Inject
    AppSettingsManager mAppSettingsManager;

    private FragmentCycleBinding binding;
    private Boolean isShowingTutorial = false;

    public static CycleFragment newInstance() {
        Bundle args = new Bundle();
        CycleFragment fragment = new CycleFragment();
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
        setHasOptionsMenu(true);
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentCycleBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cycle, container, false);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_cycle, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (isShowingTutorial) {
            return super.onOptionsItemSelected(item);
        }

        if (item.getItemId() == R.id.item_settings) {
            startActivity(SettingsActivity.makeIntent(getActivity()));
        }

        return super.onOptionsItemSelected(item);
    }

    public void showTutorial() {
        isShowingTutorial = true;
        mInteractionListener.showTutorial(getString(R.string.cycle_settings_tutorial_title), getString(R.string.cycle_settings_tutorial_title), R.id.item_settings, new MaterialTapTargetPrompt.PromptStateChangeListener() {
            @Override
            public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state) {
                if (state == MaterialTapTargetPrompt.STATE_FINISHED || state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                    isShowingTutorial = false;
                }
            }
        });
    }

    private void verifyCycleSummaryTutorial() {
        if (mAppSettingsManager.shouldShowCycleSummaryTutorial()) {
            binding.vTutorial.setVisibility(View.VISIBLE);
            binding.vTutorial.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    binding.vTutorial.setVisibility(View.GONE);
                }
            });
        } else {
            binding.vTutorial.setVisibility(View.GONE);
        }
    }

    private void setUIElements() {
        binding.vTutorial.setVisibility(View.GONE);

        CycleFragmentAdapter adapter = new CycleFragmentAdapter(getActivity(), getChildFragmentManager());
        binding.vpMain.setAdapter(adapter);
        binding.vpMain.setPagingEnabled(false);
        binding.vpMain.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
            }

            @Override
            public void onPageSelected(int i) {
            }

            @Override
            public void onPageScrollStateChanged(int index) {
                if (index == 2) {
                    verifyCycleSummaryTutorial();
                }
            }
        });

        binding.sbSections.setListener(index -> binding.vpMain.setCurrentItem(index));
    }
}
