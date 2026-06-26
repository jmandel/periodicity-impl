package com.kollectivemobile.euki.ui.onboarding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentPinConfirmationBinding;
import com.kollectivemobile.euki.ui.common.BaseFragment;

public class PinConfirmationFragment extends BaseFragment {

    private FragmentPinConfirmationBinding binding;
    private Boolean mHasPin;

    public static PinConfirmationFragment newInstance(Boolean hasPin) {
        Bundle args = new Bundle();
        PinConfirmationFragment fragment = new PinConfirmationFragment();
        fragment.mHasPin = hasPin;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUIElements();

        // Set up click listener using the binding instance
        binding.btnGotIt.setOnClickListener(v -> gotIt());
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentPinConfirmationBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return binding.getRoot(); // Return the root view from the binding
    }

    private void setUIElements() {
        int textRes = mHasPin ? R.string.pin_code_message : R.string.no_pin_code_message;
        binding.tvMessage.setText(getString(textRes));
    }

    private void gotIt() {
        mInteractionListener.replaceFragment(OnboardingFakeCodeFragment.newInstance(), true);
    }
}
