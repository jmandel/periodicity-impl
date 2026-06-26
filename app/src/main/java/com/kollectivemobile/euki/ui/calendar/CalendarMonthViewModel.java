package com.kollectivemobile.euki.ui.calendar;

import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.Date;

public class CalendarMonthViewModel extends ViewModel {
    private static final String SELECTED_DAY_KEY = "selected_day";

    private final SavedStateHandle savedStateHandle;

    public CalendarMonthViewModel(SavedStateHandle savedStateHandle) {
        this.savedStateHandle = savedStateHandle;
    }

    public void saveSelectedDay(Date date) {
        savedStateHandle.set(SELECTED_DAY_KEY, date);
    }

    public Date getSelectedDay() {
        return savedStateHandle.get(SELECTED_DAY_KEY);
    }
}
