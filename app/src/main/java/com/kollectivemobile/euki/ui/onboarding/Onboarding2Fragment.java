package com.kollectivemobile.euki.ui.onboarding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.databinding.FragmentOnboarding2Binding;
import com.kollectivemobile.euki.ui.common.BaseFragment;


public class Onboarding2Fragment extends BaseFragment {

    private FragmentOnboarding2Binding binding;

    public static Onboarding2Fragment newInstance() {
        Bundle args = new Bundle();
        Onboarding2Fragment fragment = new Onboarding2Fragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentOnboarding2Binding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return binding.getRoot(); // Return the root view from the binding
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Use binding to set up listeners
        binding.btnNice.setOnClickListener(v -> nice());
    }

    private void nice() {
        mInteractionListener.replaceFragment(Onboarding3Fragment.newInstance(), true);
    }
}

