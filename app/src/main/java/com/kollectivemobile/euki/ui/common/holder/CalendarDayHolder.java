package com.kollectivemobile.euki.ui.common.holder;

import android.content.Context;
import android.content.res.ColorStateList;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.kollectivemobile.euki.App;
import com.kollectivemobile.euki.R;
import com.kollectivemobile.euki.model.CalendarFilter;
import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.ui.common.listeners.CalendarDayListener;
import com.kollectivemobile.euki.utils.DateUtils;
import com.kollectivemobile.euki.utils.strings.StringUtils;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarDayHolder extends RecyclerView.ViewHolder {
    private CalendarDayListener mListener;
    private Date mDate;
    public View vClickable;
    public ImageView ivBlur;
    public TextView tvTitle;
    public TextView tvDay;
    public ImageView ivBleedingSize;
    public List<View> mCircleViews;

    public CalendarDayHolder(View itemView, CalendarDayListener listener) {
        super(itemView);
        mListener = listener;

        // Manual view binding
        vClickable = itemView.findViewById(R.id.v_clickable);
        ivBlur = itemView.findViewById(R.id.iv_blur);
        tvTitle = itemView.findViewById(R.id.tv_title);
        tvDay = itemView.findViewById(R.id.tv_day);
        ivBleedingSize = itemView.findViewById(R.id.iv_bleeding_size);

        // Bind circle views
        mCircleViews = Arrays.asList(
                itemView.findViewById(R.id.v_circle_1),
                itemView.findViewById(R.id.v_circle_2),
                itemView.findViewById(R.id.v_circle_3),
                itemView.findViewById(R.id.v_circle_4),
                itemView.findViewById(R.id.v_circle_5),
                itemView.findViewById(R.id.v_circle_6),
                itemView.findViewById(R.id.v_circle_7),
                itemView.findViewById(R.id.v_circle_8)
        );

        // Set click listener manually
        vClickable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.daySelected(mDate);
                }
            }
        });
    }

    public void setup(Date date, Boolean showDayName, CalendarItem calendarItem, CalendarFilter calendarFilter, Boolean isSelectedDate, Boolean isToday, Boolean isPrediction) {
        mDate = date;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        Context context = App.getContext();
        Integer currentIndex = 0;

        if (showDayName) {
            tvDay.setText(StringUtils.capitalize(DateUtils.toString(date, DateUtils.eee)));
            tvDay.setVisibility(View.VISIBLE);
        } else {
            tvDay.setVisibility(View.GONE);
        }

        tvTitle.setText(calendar.get(Calendar.DAY_OF_MONTH) + "");

        int mainBackgroundRes;
        if (isSelectedDate) {
            mainBackgroundRes = R.drawable.bkg_calendar_item_selected;
        } else if (isToday) {
            mainBackgroundRes = R.drawable.bkg_calendar_item_today;
        } else {
            mainBackgroundRes = R.drawable.bkg_calendar_item_normal;
        }

        if (calendarItem != null && calendarItem.hasPeriod()) {
            if (isToday || isSelectedDate) {
                mainBackgroundRes = R.drawable.bkg_calendar_item_bleeding_today;
            } else {
                mainBackgroundRes = R.drawable.bkg_calendar_item_bleeding;
            }
        }

        tvTitle.setBackgroundResource(mainBackgroundRes);

        Integer bleedingSizeRes = null;
        if (calendarItem != null && calendarItem.getBleedingSize() != null && calendarItem.getIncludeCycleSummary()) {
            switch (calendarItem.getBleedingSize()) {
                case SPOTING:
                    bleedingSizeRes = R.drawable.ic_circle_bleeding_spoting;
                    break;
                case LIGHT:
                    bleedingSizeRes = R.drawable.ic_circle_bleeding_light;
                    break;
                case MEDIUM:
                    bleedingSizeRes = R.drawable.ic_circle_bleeding_medium;
                    break;
                case HEAVY:
                    bleedingSizeRes = R.drawable.ic_circle_bleeding_heavy;
                    break;
            }
        } else if (isPrediction) {
            bleedingSizeRes = R.drawable.ic_circle_bleeding_prediction;
        } else {
            bleedingSizeRes = R.drawable.ic_circle_bleeding_none;
        }

        ivBleedingSize.setImageResource(bleedingSizeRes);

        for (View view : mCircleViews) {
            view.setAlpha(0);
        }

        if (calendarItem != null) {
            if (calendarItem.hasBleeding() && (calendarFilter.getBleedingOn() || calendarFilter.showAll())) {
                mCircleViews.get(currentIndex).setAlpha(1);
                mCircleViews.get(currentIndex).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.bleeding)));
                currentIndex++;
            }
            if (calendarItem.hasEmotions() && (calendarFilter.getEmotionsOn() || calendarFilter.showAll())) {
                mCircleViews.get(currentIndex).setAlpha(1);
                mCircleViews.get(currentIndex).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.emotions)));
                currentIndex++;
            }
            if (calendarItem.hasBody() && (calendarFilter.getBodyOn() || calendarFilter.showAll())) {
                mCircleViews.get(currentIndex).setAlpha(1);
                mCircleViews.get(currentIndex).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.body)));
                currentIndex++;
            }
            if (calendarItem.hasSexualActivity() && (calendarFilter.getSexualActivityOn() || calendarFilter.showAll())) {
                mCircleViews.get(currentIndex).setAlpha(1);
                mCircleViews.get(currentIndex).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.sexual_activity)));
                currentIndex++;
            }
            if (calendarItem.hasContraception() && (calendarFilter.getContraceptionOn() || calendarFilter.showAll())) {
                mCircleViews.get(currentIndex).setAlpha(1);
                mCircleViews.get(currentIndex).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.contraception)));
                currentIndex++;
            }
            if (calendarItem.hasTest() && (calendarFilter.getTestOn() || calendarFilter.showAll())) {
                mCircleViews.get(currentIndex).setAlpha(1);
                mCircleViews.get(currentIndex).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.test)));
                currentIndex++;
            }
            if (calendarItem.hasAppointment() && (calendarFilter.getAppointmentOn() || calendarFilter.showAll())) {
                mCircleViews.get(currentIndex).setAlpha(1);
                mCircleViews.get(currentIndex).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.appointment)));
                currentIndex++;
            }
            if (calendarItem.hasNote() && (calendarFilter.getNoteOn() || calendarFilter.showAll())) {
                mCircleViews.get(currentIndex).setAlpha(1);
                mCircleViews.get(currentIndex).setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.note)));
            }
        }
    }
}