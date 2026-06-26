package com.kollectivemobile.euki.ui.quiz;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentQuizIntroBinding;
import com.kollectivemobile.euki.model.Quiz;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.utils.Utils;


public class IntroFragment extends BaseFragment {

    private FragmentQuizIntroBinding binding;
    private Quiz mQuiz;

    public static IntroFragment newInstance(Quiz quiz) {
        Bundle args = new Bundle();
        IntroFragment fragment = new IntroFragment();
        fragment.mQuiz = quiz;
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setUIElements();
    }

    @Override
    protected ViewBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        binding = FragmentQuizIntroBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_intro, container, false);
    }

    private void setUIElements() {
        binding.tvTitle.setText(Utils.getLocalized(mQuiz.getInstructions()));
    }
}
