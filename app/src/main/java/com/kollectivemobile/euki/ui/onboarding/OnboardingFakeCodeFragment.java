package com.kollectivemobile.euki.ui.onboarding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentOnboardingPinBinding;
import com.kollectivemobile.euki.manager.AppSettingsManager;
import com.kollectivemobile.euki.ui.common.BaseFragment;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;


public class OnboardingFakeCodeFragment extends BaseFragment {
    @Inject AppSettingsManager mAppSettingsManager;

    private FragmentOnboardingPinBinding binding;

    public static OnboardingFakeCodeFragment newInstance() {
        OnboardingFakeCodeFragment fragment = new OnboardingFakeCodeFragment();
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initInjection();
        setUIElements();


    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentOnboardingPinBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_pin, container, false);
    }

    protected void initInjection() {
        if (getActivity() != null) {
            ((App)getActivity().getApplication()).getAppComponent().inject(this);
        }
    }

    private void setUIElements() {
        List<TextView> textViews = Arrays.asList(
                binding.tvDigit1,
                binding.tvDigit2,
                binding.tvDigit3,
                binding.tvDigit4
        );

        String code = mAppSettingsManager.getPinCode().equals("1111") ? "2222" : "1111";
        String letter = code.substring(0, 1);
        for (TextView textView : textViews) {
            textView.setText(letter);
        }

        // Set up the click listener for the next button
        binding.btnNext.setOnClickListener(v -> next());
    }

    private void next() {
        mInteractionListener.replaceFragment(TermsAndCondsFragment.newInstance(), true);
    }
}
