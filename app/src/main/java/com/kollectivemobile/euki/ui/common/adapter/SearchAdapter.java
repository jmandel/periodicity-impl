package com.kollectivemobile.euki.ui.common.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.ContentItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter {
    private List<ContentItem> mItems;
    private SearchListener mListener;
    private WeakReference<Context> mContext;

    public SearchAdapter(Context context, SearchListener listener) {
        mItems = new ArrayList<>();
        mContext = new WeakReference<>(context);
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_search, parent, false);
        return new SearchHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        SearchHolder rowHolder = (SearchHolder) holder;
        ContentItem contentItem = mItems.get(position);
        String content = contentItem.getLocalizedContent();

        rowHolder.tvTitle.setText(contentItem.getLocalizedTitle());
        rowHolder.tvText.setText(content);
        rowHolder.tvText.setVisibility(content.isEmpty() ? View.GONE : View.VISIBLE);
        rowHolder.vSeparatorTop.setVisibility(View.VISIBLE);
        rowHolder.vSeparatorBottom.setVisibility(position == mItems.size() - 1 ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class SearchHolder extends RecyclerView.ViewHolder {
        private SearchListener mListener;
        public TextView tvTitle;
        public TextView tvText;
        public View vSeparatorTop;
        public View vSeparatorBottom;
        public View rlMain;

        public SearchHolder(View itemView, SearchListener listener) {
            super(itemView);
            mListener = listener;

            // Manual view binding
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvText = itemView.findViewById(R.id.tv_text);
            vSeparatorTop = itemView.findViewById(R.id.v_separator_top);
            vSeparatorBottom = itemView.findViewById(R.id.v_separator_bottom);
            rlMain = itemView.findViewById(R.id.rl_main);

            // Set click listener manually
            rlMain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.itemSelected(getLayoutPosition());
                    }
                }
            });
        }
    }

    public void update(List<ContentItem> contentItems) {
        if (contentItems != null) {
            mItems.clear();
            mItems.addAll(contentItems);
            notifyDataSetChanged();
        }
    }

    public interface SearchListener {
        void itemSelected(int position);
    }
}
