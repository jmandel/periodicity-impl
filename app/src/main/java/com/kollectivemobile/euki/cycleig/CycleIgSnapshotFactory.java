package com.kollectivemobile.euki.cycleig;

import com.kollectivemobile.euki.model.database.entity.CalendarItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class CycleIgSnapshotFactory {
    public CycleIgSnapshot create(List<CalendarItem> sourceItems, String startInput, String endInput, CycleIgScope scope) {
        List<CalendarItem> sortedItems = new ArrayList<>(sourceItems);
        Collections.sort(sortedItems, Comparator.comparing(CalendarItem::getDate));

        Date firstDate = sortedItems.isEmpty() ? null : CycleIgDates.startOfDay(sortedItems.get(0).getDate());
        Date lastDate = sortedItems.isEmpty() ? null : CycleIgDates.startOfDay(sortedItems.get(sortedItems.size() - 1).getDate());
        Date startDate = startInput == null || startInput.trim().isEmpty() ? firstDate : CycleIgDates.parseDate(startInput);
        Date endDate = endInput == null || endInput.trim().isEmpty() ? lastDate : CycleIgDates.parseDate(endInput);

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("There is no Euki cycle data to share.");
        }
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("The SMART Link start date must be before the end date.");
        }

        List<CalendarItem> scopedItems = new ArrayList<>();
        for (CalendarItem item : sortedItems) {
            Date itemDate = CycleIgDates.startOfDay(item.getDate());
            if (itemDate.before(startDate) || itemDate.after(endDate)) {
                continue;
            }
            if (hasExportableData(item, scope)) {
                scopedItems.add(item);
            }
        }

        if (count(scopedItems, CycleIgSnapshotFactory::hasBleedingValue) == 0) {
            throw new IllegalArgumentException("SMART Link requires at least one bleeding value in the selected range.");
        }

        return new CycleIgSnapshot(scopedItems, startDate, endDate, scope, preview(scopedItems, scope));
    }

    private static CycleIgSnapshot.Preview preview(List<CalendarItem> items, CycleIgScope scope) {
        return new CycleIgSnapshot.Preview(
                items.size(),
                count(items, item -> hasBleedingValue(item) && item.getIncludeCycleSummary()),
                count(items, item -> hasBleedingValue(item) && !item.getIncludeCycleSummary()),
                count(items, item -> item.getBleedingSize() != null && item.getIncludeCycleSummary()),
                countCounters(items, item -> item.getBleedingProductsCounter()),
                countCounters(items, item -> item.getBleedingClotsCounter()),
                scope.includeEmotions() ? sum(items, item -> item.getEmotions() == null ? 0 : item.getEmotions().size()) : 0,
                scope.includeBody() ? sum(items, item -> item.getBody() == null ? 0 : item.getBody().size()) : 0,
                scope.includeNotes() ? count(items, CalendarItem::hasNote) : 0
        );
    }

    private static boolean hasExportableData(CalendarItem item, CycleIgScope scope) {
        return hasBleedingValue(item)
                || scope.includeEmotions() && item.getEmotions() != null && !item.getEmotions().isEmpty()
                || scope.includeBody() && item.getBody() != null && !item.getBody().isEmpty()
                || scope.includeNotes() && item.hasNote();
    }

    static boolean hasBleedingValue(CalendarItem item) {
        return item != null && item.hasBleeding();
    }

    private static int count(List<CalendarItem> items, Predicate predicate) {
        int total = 0;
        for (CalendarItem item : items) {
            if (predicate.test(item)) {
                total++;
            }
        }
        return total;
    }

    private static int sum(List<CalendarItem> items, Counter counter) {
        int total = 0;
        for (CalendarItem item : items) {
            total += counter.count(item);
        }
        return total;
    }

    private static int countCounters(List<CalendarItem> items, CounterList counterList) {
        int total = 0;
        for (CalendarItem item : items) {
            List<Integer> counters = counterList.get(item);
            if (counters == null) {
                continue;
            }
            for (Integer counter : counters) {
                if (counter != null && counter > 0) {
                    total++;
                }
            }
        }
        return total;
    }

    private interface Predicate {
        boolean test(CalendarItem item);
    }

    private interface Counter {
        int count(CalendarItem item);
    }

    private interface CounterList {
        List<Integer> get(CalendarItem item);
    }
}
