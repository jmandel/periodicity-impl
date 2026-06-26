package com.kollectivemobile.euki.ui.common.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.Question;
import com.kollectivemobile.euki.model.Quiz;
import com.kollectivemobile.euki.utils.Utils;

import java.lang.ref.WeakReference;


public class QuizOptionAdapter extends RecyclerView.Adapter {
    private Quiz mQuiz;
    private Question mQuestion;
    private QuizOptionListener mListener;
    private WeakReference<Context> mContext;

    public QuizOptionAdapter(Context context, Quiz quiz, int position, QuizOptionListener listener) {
        mContext = new WeakReference<>(context);
        mQuiz = quiz;
        mQuestion = quiz.getQuestions().get(position);
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_quiz_option, parent, false);
        return new QuizOptionHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        QuizOptionHolder optionHolder = (QuizOptionHolder) holder;
        String title = Utils.getLocalized(mQuestion.getOptions().get(position).first);
        Boolean isSelected = mQuestion.getAnswerIndex() != null && mQuestion.getAnswerIndex() == position;

        optionHolder.tvTitle.setText(title);
        optionHolder.ivIcon.setImageResource(isSelected ? R.drawable.ic_quiz_option_on : R.drawable.ic_quiz_option_off);
    }

    @Override
    public int getItemCount() {
        return mQuestion.getOptions().size();
    }

    static class QuizOptionHolder extends RecyclerView.ViewHolder {
        private QuizOptionListener mListener;
        public TextView tvTitle;
        public ImageView ivIcon;

        public QuizOptionHolder(View itemView, QuizOptionListener listener) {
            super(itemView);
            mListener = listener;
            tvTitle = itemView.findViewById(R.id.tv_title);
            ivIcon = itemView.findViewById(R.id.iv_icon);

            // Set click listener manually
            View llMain = itemView.findViewById(R.id.ll_main);
            llMain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.optionSelected(getLayoutPosition());
                    }
                }
            });
        }
    }

    public interface QuizOptionListener {
        void optionSelected(int position);
    }
}
