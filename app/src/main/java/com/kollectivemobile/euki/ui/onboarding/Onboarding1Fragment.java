package com.kollectivemobile.euki.ui.onboarding;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.databinding.FragmentOnboarding1Binding;
import com.kollectivemobile.euki.ui.common.BaseFragment;


public class Onboarding1Fragment extends BaseFragment {

    private FragmentOnboarding1Binding binding;

    public static Onboarding1Fragment newInstance() {
        Bundle args = new Bundle();
        Onboarding1Fragment fragment = new Onboarding1Fragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Use binding to set up listeners
        binding.btnStart.setOnClickListener(v -> start());
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentOnboarding1Binding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    private void start() {
        mInteractionListener.replaceFragment(new Onboarding2Fragment(), true);
    }
}
