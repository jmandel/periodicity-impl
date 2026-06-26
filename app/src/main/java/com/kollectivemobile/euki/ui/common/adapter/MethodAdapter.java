package com.kollectivemobile.euki.ui.common.adapter;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.LayoutQuizMethodBinding;
import com.kollectivemobile.euki.model.QuizMethod;
import com.kollectivemobile.euki.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MethodAdapter extends RecyclerView.Adapter<MethodAdapter.BookmarkHolder> {
    private List<QuizMethod> mMethods;
    private MethodListener mListener;
    private WeakReference<Context> mContext;

    public MethodAdapter(Context context, List<QuizMethod> methods, MethodListener listener) {
        mMethods = new ArrayList<>(methods);
        mContext = new WeakReference<>(context);
        mListener = listener;
    }

    @Override
    public BookmarkHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_quiz_method, parent, false);
        return new BookmarkHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(BookmarkHolder holder, int position) {
        QuizMethod method = mMethods.get(position);

        holder.binding.tvTitle.setText(Utils.getLocalized(method.getTitle()));
        holder.binding.tvTitle.setAlpha((float) (method.getSelected() ? 1.0 : 0.3));
        holder.binding.ivIcon.setImageResource(Utils.getImageId(method.getImageName()));
        holder.binding.ivIcon.setAlpha((float) (method.getSelected() ? 1.0 : 0.3));
    }

    @Override
    public int getItemCount() {
        return mMethods.size();
    }

    static class BookmarkHolder extends RecyclerView.ViewHolder {
        private MethodListener mListener;
        private LayoutQuizMethodBinding binding;

        public BookmarkHolder(View itemView, MethodListener listener) {
            super(itemView);
            mListener = listener;
            binding = LayoutQuizMethodBinding.bind(itemView);

            // Set click listener using lambda expression
            binding.llMain.setOnClickListener(v -> onClick());
        }

        private void onClick() {
            if (mListener != null) {
                mListener.methodSelected(getBindingAdapterPosition());
            }
        }
    }

    public interface MethodListener {
        void methodSelected(int position);
    }
}
