package com.kollectivemobile.euki.cycleig;

import com.kollectivemobile.euki.model.database.entity.CalendarItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CycleIgSnapshot {
    private final List<CalendarItem> items;
    private final Date startDate;
    private final Date endDate;
    private final CycleIgScope scope;
    private final Preview preview;

    public CycleIgSnapshot(List<CalendarItem> items, Date startDate, Date endDate, CycleIgScope scope, Preview preview) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
        this.startDate = startDate;
        this.endDate = endDate;
        this.scope = scope;
        this.preview = preview;
    }

    public List<CalendarItem> getItems() {
        return items;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public String getStartDateString() {
        return CycleIgDates.formatDate(startDate);
    }

    public String getEndDateString() {
        return CycleIgDates.formatDate(endDate);
    }

    public CycleIgScope getScope() {
        return scope;
    }

    public Preview getPreview() {
        return preview;
    }

    public static class Preview {
        public final int dayCount;
        public final int menstrualBleedingFacts;
        public final int nonMenstrualBleedingFacts;
        public final int flowFacts;
        public final int productFacts;
        public final int clotFacts;
        public final int emotionFacts;
        public final int bodyFacts;
        public final int noteFacts;

        public Preview(
                int dayCount,
                int menstrualBleedingFacts,
                int nonMenstrualBleedingFacts,
                int flowFacts,
                int productFacts,
                int clotFacts,
                int emotionFacts,
                int bodyFacts,
                int noteFacts
        ) {
            this.dayCount = dayCount;
            this.menstrualBleedingFacts = menstrualBleedingFacts;
            this.nonMenstrualBleedingFacts = nonMenstrualBleedingFacts;
            this.flowFacts = flowFacts;
            this.productFacts = productFacts;
            this.clotFacts = clotFacts;
            this.emotionFacts = emotionFacts;
            this.bodyFacts = bodyFacts;
            this.noteFacts = noteFacts;
        }
    }
}
