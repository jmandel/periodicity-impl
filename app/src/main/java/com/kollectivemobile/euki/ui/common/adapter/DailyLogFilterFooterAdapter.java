package com.kollectivemobile.euki.ui.common.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.databinding.LayoutFilterItemFooterBinding;
import com.kollectivemobile.euki.utils.advrecyclerview.headerfooter.AbstractHeaderFooterWrapperAdapter;

public class DailyLogFilterFooterAdapter extends AbstractHeaderFooterWrapperAdapter<DailyLogFilterFooterAdapter.HeaderViewHolder, DailyLogFilterFooterAdapter.FooterViewHolder> {
    private View.OnClickListener mOnClickListener;

    public DailyLogFilterFooterAdapter(RecyclerView.Adapter adapter, View.OnClickListener onClickListener) {
        setAdapter(adapter);
        mOnClickListener = onClickListener;
    }

    @NonNull
    @Override
    public HeaderViewHolder onCreateHeaderItemViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_filter_item_footer, parent, false);
        return new HeaderViewHolder(view);
    }

    @NonNull
    @Override
    public FooterViewHolder onCreateFooterItemViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutFilterItemFooterBinding binding = LayoutFilterItemFooterBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FooterViewHolder(binding, mOnClickListener);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        setupFullSpanForGridLayoutManager(recyclerView);
    }

    @Override
    public void onBindHeaderItemViewHolder(@NonNull HeaderViewHolder holder, int localPosition) {
    }

    @Override
    public void onBindFooterItemViewHolder(@NonNull FooterViewHolder holder, int localPosition) {
        holder.binding.btnAction.setText(App.getContext().getString(R.string.done));
    }

    @Override
    public int getHeaderItemCount() {
        return 0;
    }

    @Override
    public int getFooterItemCount() {
        return 1;
    }

    public void setupFullSpanForGridLayoutManager(RecyclerView recyclerView) {
        RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
        if (!(lm instanceof GridLayoutManager)) {
            return;
        }

        final GridLayoutManager glm = (GridLayoutManager) lm;
        final GridLayoutManager.SpanSizeLookup origSizeLookup = glm.getSpanSizeLookup();
        final int spanCount = glm.getSpanCount();

        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                final long segmentedPosition = getSegmentedPosition(position);
                final int segment = extractSegmentPart(segmentedPosition);
                final int offset = extractSegmentOffsetPart(segmentedPosition);

                if (segment == SEGMENT_TYPE_NORMAL) {
                    return origSizeLookup.getSpanSize(offset);
                } else {
                    return spanCount; // header or footer
                }
            }
        });
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(View itemView){
            super(itemView);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        final LayoutFilterItemFooterBinding binding;

        private View.OnClickListener mOnClickListener;

        public FooterViewHolder(LayoutFilterItemFooterBinding binding, View.OnClickListener onClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.mOnClickListener = onClickListener;

            this.binding.btnAction.setOnClickListener(v -> {
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(v);
                }
            });
        }
    }
}
