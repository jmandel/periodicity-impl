package com.kollectivemobile.euki.ui.common.holder;

import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.ui.common.adapter.DailyLogAdapter;
import com.kollectivemobile.euki.ui.common.listeners.DailyLogViewListener;
import com.kollectivemobile.euki.ui.common.views.SelectableButton;
import com.kollectivemobile.euki.utils.Constants;

import java.util.Arrays;
import java.util.List;

public class DailyLogTestHolder extends BaseDailyLogHolder implements View.OnClickListener {
    private List<SelectableButton> sbTestSTI;
    private List<SelectableButton> sbTestPregnancy;

    public DailyLogTestHolder(@NonNull View itemView, DailyLogViewListener listener) {
        super(itemView, listener);

        // Manual view binding
        sbTestSTI = Arrays.asList(
                itemView.findViewById(R.id.sb_test_sti_1),
                itemView.findViewById(R.id.sb_test_sti_2)
        );
        sbTestPregnancy = Arrays.asList(
                itemView.findViewById(R.id.sb_test_pregnancy_1),
                itemView.findViewById(R.id.sb_test_pregnancy_2)
        );

        // Set OnClickListeners
        for (SelectableButton selectableButton : sbTestSTI) {
            selectableButton.setOnClickListener(this);
        }
        for (SelectableButton selectableButton : sbTestPregnancy) {
            selectableButton.setOnClickListener(this);
        }
    }

    public static DailyLogTestHolder create(ViewGroup parent, DailyLogViewListener listener) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_daily_log_test, parent, false);
        return new DailyLogTestHolder(view, listener);
    }

    @Override
    public DailyLogAdapter.ViewType getViewType() {
        return DailyLogAdapter.ViewType.TEST;
    }

    @Override
    public Boolean hasData() {
        return mCalendarItem.hasTest();
    }

    @Override
    public void bind(CalendarItem calendarItem, Boolean selected, DailyLogAdapter.ViewType selectedType) {
        super.bind(calendarItem, selected, selectedType);

        if (calendarItem.getTestSTI() != null) {
            sbTestSTI.get(calendarItem.getTestSTI().ordinal()).changeSelected(true);
        }
        if (calendarItem.getTestPregnancy() != null) {
            sbTestPregnancy.get(calendarItem.getTestPregnancy().ordinal()).changeSelected(true);
        }
    }

    @Override
    public void onClick(View view) {
        if (view instanceof SelectableButton && view.getTag() instanceof String) {
            SelectableButton selectableButton = (SelectableButton) view;
            Integer index = Integer.parseInt((String) view.getTag());

            if (sbTestSTI.contains(selectableButton)) {
                mCalendarItem.setTestSTI(selectableButton.getSelected() ? Constants.TestSTI.values()[index - 1] : null);
            } else if (sbTestPregnancy.contains(selectableButton)) {
                mCalendarItem.setTestPregnancy(selectableButton.getSelected() ? Constants.TestPregnancy.values()[index - 1] : null);
            }

            updateTitle();
            mListener.dataChanged();
        }
    }
}

