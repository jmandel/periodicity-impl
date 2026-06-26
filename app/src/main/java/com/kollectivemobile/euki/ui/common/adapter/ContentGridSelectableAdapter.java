package com.kollectivemobile.euki.ui.common.adapter;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.ContentItem;
import com.kollectivemobile.euki.utils.Utils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ContentGridSelectableAdapter extends RecyclerView.Adapter {
    private WeakReference<Context> mContext;
    private List<ContentItem> mContentItems;
    private ContentGridSelectableListener mListener;

    public ContentGridSelectableAdapter(Context context, List<ContentItem> items, ContentGridSelectableListener listener) {
        mContentItems = new ArrayList<>();
        mContentItems.addAll(items);

        mContext = new WeakReference<>(context);
        mListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_content_grid_selectable, parent, false);
        return new ContentGridHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ContentGridHolder gridHolder = (ContentGridHolder) holder;
        ContentItem contentItem = mContentItems.get(position);

        gridHolder.ivIcon.setImageResource(Utils.getImageId(contentItem.getImageIcon()));

        String title = "";
        if (contentItem.getTitle() != null && !contentItem.getTitle().isEmpty()) {
            title = contentItem.getTitle();
        } else if (contentItem.getId() != null && !contentItem.getId().isEmpty()) {
            title = contentItem.getId();
        }
        gridHolder.tvTitle.setText(Utils.getLocalized(title));
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getItemCount() {
        return mContentItems.size();
    }

    class ContentGridHolder extends RecyclerView.ViewHolder {
        public CardView cvMain;
        public ImageView ivIcon;
        public TextView tvTitle;

        ContentGridSelectableListener mListener;

        public ContentGridHolder(@NonNull View itemView, ContentGridSelectableListener listener) {
            super(itemView);
            mListener = listener;
            cvMain = itemView.findViewById(R.id.cv_main);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);

            // Set click listener manually
            cvMain.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mainClicked();
                }
            });
        }

        void mainClicked() {
            if (mListener != null) {
                mListener.gridRowSelected(getLayoutPosition() - 1);
            }
        }
    }


    public interface ContentGridSelectableListener {
        void gridRowSelected(int position);
    }
}
