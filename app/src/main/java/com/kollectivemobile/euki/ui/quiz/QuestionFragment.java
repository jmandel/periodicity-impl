package com.kollectivemobile.euki.ui.quiz;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewbinding.ViewBinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.FragmentQuizOptionBinding;
import com.kollectivemobile.euki.model.Quiz;
import com.kollectivemobile.euki.ui.common.BaseFragment;
import com.kollectivemobile.euki.ui.common.adapter.QuizOptionAdapter;
import com.kollectivemobile.euki.utils.Utils;


public class QuestionFragment extends BaseFragment implements QuizOptionAdapter.QuizOptionListener {

    private FragmentQuizOptionBinding binding;
    private QuizOptionAdapter mAdapter;
    private Quiz mQuiz;
    private int mPosition;

    public static QuestionFragment newInstance(Quiz quiz, int position) {
        Bundle args = new Bundle();
        QuestionFragment fragment = new QuestionFragment();
        fragment.mQuiz = quiz;
        fragment.mPosition = position;
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
        binding = FragmentQuizOptionBinding.inflate(inflater, container, false);
        return binding;
    }

    @Override
    protected View onCreateViewCalled(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_option, container, false);
    }

    @Override
    public String getTitle() {
        return getString(R.string.quiz);
    }

    private void setUIElements() {
        binding.tvTitle.setText(Utils.getLocalized(mQuiz.getQuestions().get(mPosition).getTitle()));
        mAdapter = new QuizOptionAdapter(getActivity(), mQuiz, mPosition, this);
        binding.rvMain.setAdapter(mAdapter);
        binding.rvMain.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void optionSelected(int answerPosition) {
        mQuiz.setAnswer(mPosition, answerPosition);
        mAdapter.notifyDataSetChanged();
    }
}
