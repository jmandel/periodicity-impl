package com.kollectivemobile.euki.ui.common.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.FilterItem;
import com.kollectivemobile.euki.utils.Utils;

import java.util.List;

public class CalendarFilterAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final Integer VIEW_TYPE_ITEM = 0;
    static final Integer VIEW_TYPE_FOOTER = 1;

    private List<FilterItem> mFilterItems;
    private CalendarFilterListener mListener;
    private Context mContext;

    public CalendarFilterAdapter(Context context, List<FilterItem> filterItems, CalendarFilterListener listener) {
        mFilterItems = filterItems;
        mContext = context;
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_ITEM) {
            View view = inflater.inflate(R.layout.layout_filter_item, parent, false);
            return new CalendarItemHolder(view, mListener);
        }
        View view = inflater.inflate(R.layout.layout_filter_footer, parent, false);
        return new CalendarFooterHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CalendarItemHolder) {
            CalendarItemHolder itemHolder = (CalendarItemHolder) holder;
            FilterItem filterItem = mFilterItems.get(position);
            itemHolder.vCircle.setBackgroundResource(filterItem.getOn() ? R.drawable.ic_calendar_filter_on : R.drawable.ic_calendar_filter_off);
            itemHolder.vCircle.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, filterItem.getColor())));
            itemHolder.tvTitle.setText(Utils.getLocalized(filterItem.getTitle()));
        }
    }

    @Override
    public int getItemCount() {
        return mFilterItems.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < mFilterItems.size()) {
            return VIEW_TYPE_ITEM;
        }

        return VIEW_TYPE_FOOTER;
    }

    static class CalendarItemHolder extends RecyclerView.ViewHolder {
        private CalendarFilterListener mListener;
        private View vCircle;
        private TextView tvTitle;

        public CalendarItemHolder(View itemView, CalendarFilterListener listener) {
            super(itemView);
            mListener = listener;
            vCircle = itemView.findViewById(R.id.v_circle);
            tvTitle = itemView.findViewById(R.id.tv_title);

            itemView.findViewById(R.id.ll_main).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.filterSelected(getLayoutPosition());
                    }
                }
            });
        }
    }

    static class CalendarFooterHolder extends RecyclerView.ViewHolder {
        private CalendarFilterListener mListener;

        public CalendarFooterHolder(View itemView, CalendarFilterListener listener) {
            super(itemView);
            mListener = listener;

            itemView.findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.clear();
                    }
                }
            });

            itemView.findViewById(R.id.btn_show_results).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.showResults();
                    }
                }
            });
        }
    }

    public interface CalendarFilterListener {
        void filterSelected(int position);
        void clear();
        void showResults();
    }
}
