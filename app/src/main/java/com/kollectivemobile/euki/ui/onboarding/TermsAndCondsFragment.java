package com.kollectivemobile.euki.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentOnboardingTermsBinding;
import com.kollectivemobile.euki.manager.AppSettingsManager;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.Dialogs;
import com.kollectivemobile.euki.ui.main.MainActivity;

import javax.inject.Inject;

public class TermsAndCondsFragment extends BaseFragment {
    @Inject AppSettingsManager mAppSettingsManager;

    private FragmentOnboardingTermsBinding binding;

    public static TermsAndCondsFragment newInstance() {
        Bundle args = new Bundle();
        TermsAndCondsFragment fragment = new TermsAndCondsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_terms, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() != null) {
            ((App)getActivity().getApplication()).getAppComponent().inject(this);
        }

        binding.btnAgree.setOnClickListener(v -> agree());
        binding.btnCancel.setOnClickListener(v -> cancel());
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentOnboardingTermsBinding.inflate(inflater, container, false);
        return binding;
    }

    private void agree() {
        mAppSettingsManager.saveShowOnboardinScreens(false);
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    private void cancel() {
        Dialogs.createSimpleDialog(getActivity(), null, getString(R.string.terms_of_use_accept), null).show();
    }

}
