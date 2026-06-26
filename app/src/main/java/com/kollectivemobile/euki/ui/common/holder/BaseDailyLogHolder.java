package com.kollectivemobile.euki.ui.common.holder;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.ui.common.adapter.DailyLogAdapter;
import com.kollectivemobile.euki.ui.common.listeners.DailyLogViewListener;

public abstract class BaseDailyLogHolder extends RecyclerView.ViewHolder {
    protected LinearLayout llTitle;
    protected View vCircle;
    protected LinearLayout llContent;

    protected CalendarItem mCalendarItem;
    protected DailyLogViewListener mListener;
    protected Boolean isSelected = false;

    public BaseDailyLogHolder(@NonNull View itemView, DailyLogViewListener listener) {
        super(itemView);
        mListener = listener;

        llTitle = itemView.findViewById(R.id.ll_title);
        vCircle = itemView.findViewById(R.id.v_circle);
        llContent = itemView.findViewById(R.id.ll_content);

        llTitle.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.selectedViewType(getViewType(), getLayoutPosition());
            }
        });
    }

    public void bind(CalendarItem calendarItem, Boolean selected, DailyLogAdapter.ViewType selectedType) {
        itemView.setAlpha(selectedType == null || selectedType == getViewType() ? 1.0f : 0.4f);
        mCalendarItem = calendarItem;
        isSelected = selected;
        llContent.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        updateTitle();
    }

    public void updateTitle() {
        vCircle.setBackgroundResource(hasData() ? R.drawable.bkg_circular_daily_log_on : R.drawable.bkg_circular_daily_log_off);
    }

    public abstract DailyLogAdapter.ViewType getViewType();

    public abstract Boolean hasData();
}
