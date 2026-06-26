package com.kollectivemobile.euki.ui.common.holder;

import androidx.annotation.NonNull;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.ui.common.adapter.DailyLogAdapter;
import com.kollectivemobile.euki.ui.common.listeners.DailyLogViewListener;
import com.kollectivemobile.euki.ui.common.views.SelectableButton;
import com.kollectivemobile.euki.utils.Constants;

import java.util.Arrays;
import java.util.List;

public class DailyLogBleedingHolder extends BaseDailyLogHolder implements View.OnClickListener {
    public List<SelectableButton> sbBleedingSizes;
    public List<SelectableButton> sbBleedingProducts;
    public List<SelectableButton> sbBleedingClots;

    public ImageView ivInfo;
    public LinearLayout llIncludeCycle;
    public ImageView ivIncludeCycle;

    private boolean mBleedingTrackingEnabled;

    public DailyLogBleedingHolder(@NonNull View itemView, DailyLogViewListener listener, Boolean bleedingTrackingEnabled) {
        super(itemView, listener);

        // Manual view binding
        sbBleedingSizes = Arrays.asList(
                itemView.findViewById(R.id.sb_bleeding_size_1),
                itemView.findViewById(R.id.sb_bleeding_size_2),
                itemView.findViewById(R.id.sb_bleeding_size_3),
                itemView.findViewById(R.id.sb_bleeding_size_4)
        );

        sbBleedingProducts = Arrays.asList(
                itemView.findViewById(R.id.sb_bleeding_product_1),
                itemView.findViewById(R.id.sb_bleeding_product_2),
                itemView.findViewById(R.id.sb_bleeding_product_3),
                itemView.findViewById(R.id.sb_bleeding_product_4),
                itemView.findViewById(R.id.sb_bleeding_product_5),
                itemView.findViewById(R.id.sb_bleeding_product_6),
                itemView.findViewById(R.id.sb_bleeding_product_7)
        );

        sbBleedingClots = Arrays.asList(
                itemView.findViewById(R.id.sb_clot_1),
                itemView.findViewById(R.id.sb_clot_2)
        );

        ivInfo = itemView.findViewById(R.id.tv_info);
        llIncludeCycle = itemView.findViewById(R.id.ll_include_cycle);
        ivIncludeCycle = itemView.findViewById(R.id.iv_include_cycle);

        mBleedingTrackingEnabled = bleedingTrackingEnabled;

        // Set click listeners manually
        for (SelectableButton selectableButton : sbBleedingSizes) {
            selectableButton.setOnClickListener(this);
        }
        for (SelectableButton selectableButton : sbBleedingProducts) {
            selectableButton.setOnClickListener(this);
        }
        for (SelectableButton selectableButton : sbBleedingClots) {
            selectableButton.setOnClickListener(this);
        }

        ivInfo.setOnClickListener(view -> mListener.infoAction());
        ivIncludeCycle.setOnClickListener(view -> {
            if (mBleedingTrackingEnabled) {
                mCalendarItem.setIncludeCycleSummary(!mCalendarItem.getIncludeCycleSummary());
                updateIncludeCycle();
            }
        });

        llIncludeCycle.setAlpha(mBleedingTrackingEnabled ? 1.0f : 0.5f);
    }

    public static DailyLogBleedingHolder create(ViewGroup parent, DailyLogViewListener listener, Boolean bleedingTrackingEnabled) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_daily_log_bleeding, parent, false);
        return new DailyLogBleedingHolder(view, listener, bleedingTrackingEnabled);
    }

    @Override
    public DailyLogAdapter.ViewType getViewType() {
        return DailyLogAdapter.ViewType.BLEEDING;
    }

    @Override
    public Boolean hasData() {
        return mCalendarItem.hasBleeding();
    }

    @Override
    public void bind(CalendarItem calendarItem, Boolean selected, DailyLogAdapter.ViewType selectedType) {
        super.bind(calendarItem, selected, selectedType);

        // Set IncludeCycleSummary to true by default if it hasn't been initialized
       /* if (calendarItem.getIncludeCycleSummary() != null) {
            calendarItem.setIncludeCycleSummary(true);
            mCalendarItem.setIncludeCycleSummary(true);
        }*/

        if (calendarItem.getBleedingSize() != null) {
            sbBleedingSizes.get(calendarItem.getBleedingSize().ordinal()).changeSelected(true);
        }
        for (int i = 0; i < sbBleedingProducts.size(); i++) {
            SelectableButton selectableButton = sbBleedingProducts.get(i);
            selectableButton.setCounter(mCalendarItem.getBleedingProductsCounter().get(i));
        }
        for (int i = 0; i < sbBleedingClots.size(); i++) {
            SelectableButton selectableButton = sbBleedingClots.get(i);
            selectableButton.setCounter(mCalendarItem.getBleedingClotsCounter().get(i));
        }

        ivInfo.setVisibility(selected ? View.VISIBLE : View.GONE);
        updateIncludeCycle();
    }

    private void updateIncludeCycle() {
        if (!mCalendarItem.hasBleeding()) {
            mCalendarItem.setIncludeCycleSummary(true);
        }

        llIncludeCycle.setVisibility(mCalendarItem.hasBleeding() ? View.VISIBLE : View.GONE);
        Integer resId = mCalendarItem.getIncludeCycleSummary() ? R.drawable.ic_track_cycle_on : R.drawable.ic_track_cycle_off;
        ivIncludeCycle.setImageResource(resId);
    }

    @Override
    public void onClick(View view) {
        if (view instanceof SelectableButton && view.getTag() instanceof String) {
            SelectableButton selectableButton = (SelectableButton) view;
            Integer index = Integer.parseInt((String) view.getTag());

            if (sbBleedingSizes.contains(selectableButton)) {
                mCalendarItem.setBleedingSize(selectableButton.getSelected() ? Constants.BleedingSize.values()[index - 1] : null);
            } else if (sbBleedingProducts.contains(selectableButton)) {
                mCalendarItem.getBleedingProductsCounter().set(index - 1, selectableButton.getCounter());
            } else if (sbBleedingClots.contains(selectableButton)) {
                mCalendarItem.getBleedingClotsCounter().set(index - 1, selectableButton.getCounter());
            }

            updateTitle();
            updateIncludeCycle();
            mListener.dataChanged();

            if (mCalendarItem.hasBleeding()) {
                mListener.checkIncludeBleedingCycle();
            }
        }
    }
}
