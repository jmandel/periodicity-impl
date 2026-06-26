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

public class DailyLogEmotionsHolder extends BaseDailyLogHolder implements View.OnClickListener {
    private List<SelectableButton> sbEmotions;

    public DailyLogEmotionsHolder(@NonNull View itemView, DailyLogViewListener listener) {
        super(itemView, listener);

        // Bind views manually
        sbEmotions = Arrays.asList(
                itemView.findViewById(R.id.sb_emotions_1),
                itemView.findViewById(R.id.sb_emotions_2),
                itemView.findViewById(R.id.sb_emotions_3),
                itemView.findViewById(R.id.sb_emotions_4),
                itemView.findViewById(R.id.sb_emotions_5),
                itemView.findViewById(R.id.sb_emotions_6),
                itemView.findViewById(R.id.sb_emotions_7),
                itemView.findViewById(R.id.sb_emotions_8),
                itemView.findViewById(R.id.sb_emotions_9)
        );

        // Set click listeners
        for (SelectableButton selectableButton : sbEmotions) {
            selectableButton.setOnClickListener(this);
        }
    }

    public static DailyLogEmotionsHolder create(ViewGroup parent, DailyLogViewListener listener) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.layout_daily_log_emotions, parent, false);
        return new DailyLogEmotionsHolder(view, listener);
    }

    @Override
    public DailyLogAdapter.ViewType getViewType() {
        return DailyLogAdapter.ViewType.EMOTIONS;
    }

    @Override
    public Boolean hasData() {
        return mCalendarItem.hasEmotions();
    }

    @Override
    public void bind(CalendarItem calendarItem, Boolean selected, DailyLogAdapter.ViewType selectedType) {
        super.bind(calendarItem, selected, selectedType);

        for (Constants.Emotions emotions : mCalendarItem.getEmotions()) {
            sbEmotions.get(emotions.ordinal()).changeSelected(true);
        }
    }

    @Override
    public void onClick(View view) {
        if (view instanceof SelectableButton && view.getTag() instanceof String) {
            SelectableButton selectableButton = (SelectableButton) view;
            Integer index = Integer.parseInt((String) view.getTag());
            Constants.Emotions emotion = Constants.Emotions.values()[index - 1];

            if (mCalendarItem.getEmotions().contains(emotion)) {
                mCalendarItem.getEmotions().remove(emotion);
            } else {
                mCalendarItem.getEmotions().add(emotion);
            }

            updateTitle();
            mListener.dataChanged();
        }
    }
}

