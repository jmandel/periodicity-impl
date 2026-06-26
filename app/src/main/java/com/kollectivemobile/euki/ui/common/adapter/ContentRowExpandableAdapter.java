package com.kollectivemobile.euki.ui.common.adapter;

import android.content.Context;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.listeners.LinkListener;
import com.kollectivemobile.euki.manager.BookmarkManager;
import com.kollectivemobile.euki.model.ContentItem;
import com.kollectivemobile.euki.model.SwipeableItem;
import com.kollectivemobile.euki.utils.TextUtils;
import com.kollectivemobile.euki.utils.advrecyclerview.expandable.ExpandableItemState;
import com.kollectivemobile.euki.utils.advrecyclerview.utils.AbstractExpandableItemAdapter;
import com.kollectivemobile.euki.utils.advrecyclerview.utils.AbstractExpandableItemViewHolder;
import com.kollectivemobile.euki.utils.siwperview.CustomContainerView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ContentRowExpandableAdapter extends AbstractExpandableItemAdapter<ContentRowExpandableAdapter.TitleHolder, ContentRowExpandableAdapter.TextHolder> {
    private List<ContentItem> mContentItems;

    private ContentRowExpandableListener mListener;
    private LinkListener mLinkListener;
    private WeakReference<Context> mContext;
    private BookmarkManager mBookmarkManager;

    // Inside ContentItem class
    private List<SwipeableItem> mSwiperItems;


    public ContentRowExpandableAdapter(Context context, List<ContentItem> items, ContentRowExpandableListener listener, LinkListener linkListener, BookmarkManager bookmarkManager) {
        mContentItems = new ArrayList<>();
        mContentItems.addAll(items);
        mContext = new WeakReference<>(context);
        mListener = listener;
        mLinkListener = linkListener;
        mBookmarkManager = bookmarkManager;
        setHasStableIds(true);
    }

    @Override
    public int getGroupCount() {
        return mContentItems.size();
    }

    @Override
    public int getChildCount(int groupPosition) {
        return 1;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return mContentItems.get(groupPosition).getItemId();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return mContentItems.get(groupPosition).getItemId();
    }

    @Override
    public int getGroupItemViewType(int groupPosition) {
        return 0;
    }

    @Override
    public int getChildItemViewType(int groupPosition, int childPosition) {
        return 0;
    }

    @NonNull
    @Override
    public TitleHolder onCreateGroupViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_content_row_expandable_title, parent, false);
        return new TitleHolder(view, mListener);
    }

    @NonNull
    @Override
    public TextHolder onCreateChildViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_content_row_expandable_text, parent, false);
        return new TextHolder(view);
    }

    @Override
    public void onBindGroupViewHolder(@NonNull final TitleHolder holder, int groupPosition, int viewType) {
        final ContentItem contentItem = mContentItems.get(groupPosition);

        holder.tvTitle.setText(contentItem.getLocalizedTitle());
        holder.vSeparatorTop.setVisibility(View.VISIBLE);
        holder.vSeparatorBottom.setVisibility(groupPosition == mContentItems.size() - 1 ? View.VISIBLE : View.GONE);

        final ExpandableItemState expandState = holder.getExpandState();

        if (expandState.isUpdated()) {
            holder.ivArrow.setImageResource(expandState.isExpanded() ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
            holder.vSeparatorBottom.setVisibility(expandState.isExpanded() ? View.GONE : groupPosition == mContentItems.size() - 1 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onBindChildViewHolder(@NonNull final TextHolder holder, int groupPosition, int childPosition, int viewType) {
        final ContentItem contentItem = mContentItems.get(groupPosition);

        String originalText = contentItem.getLocalizedContent();
        Spanned spannedFormattedText = Html.fromHtml(originalText, Html.FROM_HTML_MODE_LEGACY);
        Spannable spannableFormattedText = new SpannableString(spannedFormattedText);
        Spannable linkSpannable = TextUtils.getSpannable(originalText, contentItem.getLinks(), mLinkListener, contentItem.getBoldStrings());

        for (Object span : linkSpannable.getSpans(0, linkSpannable.length(), Object.class)) {
            int start = linkSpannable.getSpanStart(span);
            int end = linkSpannable.getSpanEnd(span);

            String spanText = linkSpannable.subSequence(start, end).toString();

            int newStart = spannedFormattedText.toString().indexOf(spanText);
            int newEnd = newStart + spanText.length();

            if (newStart >= 0 && newEnd <= spannedFormattedText.length()) {
                spannableFormattedText.setSpan(span, newStart, newEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        holder.tvTitle.setText(spannableFormattedText);
        holder.tvTitle.setMovementMethod(LinkMovementMethod.getInstance());
        holder.vSeparatorBottom.setVisibility(groupPosition == mContentItems.size() - 1 ? View.VISIBLE : View.GONE);

        int resId = mBookmarkManager.isBookmark(contentItem.getId()) ? R.drawable.ic_bookmark_on : R.drawable.ic_bookmark_off;
        holder.ivBookmark.setImageResource(resId);
        holder.ivBookmark.setOnClickListener(view -> {
            if (mBookmarkManager.isBookmark(contentItem.getId())) {
                mBookmarkManager.removeBookmark(contentItem.getId());
                holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_off);
            } else {
                mBookmarkManager.addBookmark(contentItem.getId());
                holder.ivBookmark.setImageResource(R.drawable.ic_bookmark_on);
            }
        });

        // Check if the contentItem has swipe_pager items
        if (contentItem.getSwiperItems() != null && !contentItem.getSwiperItems().isEmpty()) {
            holder.swiperView.setItems(contentItem.getSwiperItems());
            holder.swiperView.setVisibility(View.VISIBLE);
        } else {
            holder.swiperView.setVisibility(View.GONE);
        }
    }


    @Override
    public boolean onCheckCanExpandOrCollapseGroup(@NonNull TitleHolder holder, int groupPosition, int x, int y, boolean expand) {
        return true;
    }

    static class TitleHolder extends AbstractExpandableItemViewHolder {
        private ContentRowExpandableListener mListener;
        private TextView tvTitle;
        private ImageView ivArrow;
        private View vSeparatorTop;
        private View vSeparatorBottom;

        public TitleHolder(View itemView, ContentRowExpandableListener listener) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            ivArrow = itemView.findViewById(R.id.iv_arrow);
            vSeparatorTop = itemView.findViewById(R.id.v_separator_top);
            vSeparatorBottom = itemView.findViewById(R.id.v_separator_bottom);
            mListener = listener;

            itemView.findViewById(R.id.rl_main).setOnClickListener(v -> onClick());
        }

        private void onClick() {
            if (mListener != null) {
                mListener.rowExpandableSelected(getLayoutPosition() - 1);
            }
        }
    }

    static class TextHolder extends AbstractExpandableItemViewHolder {
        private TextView tvTitle;
        private View vSeparatorBottom;
        private ImageView ivBookmark;
        private CustomContainerView swiperView;

        public TextHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            vSeparatorBottom = itemView.findViewById(R.id.v_separator_bottom);
            ivBookmark = itemView.findViewById(R.id.iv_bookmark);
            swiperView = itemView.findViewById(R.id.swiper_view);
        }
    }

    public interface ContentRowExpandableListener {
        void rowExpandableSelected(int position);
    }
}
