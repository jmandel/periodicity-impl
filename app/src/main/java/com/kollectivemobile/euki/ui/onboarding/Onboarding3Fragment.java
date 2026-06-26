package com.kollectivemobile.euki.ui.onboarding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.databinding.FragmentOnboarding3Binding;
import com.kollectivemobile.euki.ui.common.BaseFragment;

public class Onboarding3Fragment extends BaseFragment {

    private FragmentOnboarding3Binding binding;

    public static Onboarding3Fragment newInstance() {
        Bundle args = new Bundle();
        Onboarding3Fragment fragment = new Onboarding3Fragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentOnboarding3Binding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return binding.getRoot(); // Return the root view from the binding
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up click listeners using the binding instance
        binding.btnPin.setOnClickListener(v -> pin());
        binding.btnNoPin.setOnClickListener(v -> noPin());
    }

    private void pin() {
        mInteractionListener.replaceFragment(PinSetupFragment.newInstance(), true);
    }

    private void noPin() {
        mInteractionListener.replaceFragment(TermsAndCondsFragment.newInstance(), true);
    }
}
