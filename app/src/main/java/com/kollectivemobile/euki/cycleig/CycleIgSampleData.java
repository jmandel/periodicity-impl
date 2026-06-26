package com.kollectivemobile.euki.cycleig;

import com.kollectivemobile.euki.model.database.entity.CalendarItem;
import com.kollectivemobile.euki.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class CycleIgSampleData {
    private static final String[] CYCLE_STARTS = {
            "2026-01-02",
            "2026-01-30",
            "2026-02-27",
            "2026-03-27",
            "2026-04-24",
            "2026-05-22",
            "2026-06-19"
    };
    private static final Date LAST_SAMPLE_DATE = CycleIgDates.parseDate("2026-06-25");

    private CycleIgSampleData() {
    }

    public static List<CalendarItem> create() {
        List<CalendarItem> items = new ArrayList<>();
        for (int index = 0; index < CYCLE_STARTS.length; index++) {
            items.addAll(createCycle(CycleIgDates.parseDate(CYCLE_STARTS[index]), index));
        }
        items.removeIf(item -> item.getDate().after(LAST_SAMPLE_DATE));
        Collections.sort(items, (first, second) -> first.getDate().compareTo(second.getDate()));
        return items;
    }

    private static List<CalendarItem> createCycle(Date startDate, int cycleIndex) {
        int cycleNumber = cycleIndex + 1;
        List<CalendarItem> items = new ArrayList<>();

        items.add(day(startDate, 0, Constants.BleedingSize.HEAVY, true,
                Arrays.asList(Constants.Emotions.CALM),
                Arrays.asList(Constants.Body.CRAMPS, Constants.Body.FATIGUE),
                "Synthetic Euki sample cycle " + cycleNumber + " day 1."));
        items.add(day(startDate, 1, Constants.BleedingSize.MEDIUM, true,
                Arrays.asList(Constants.Emotions.IRRITABLE),
                Arrays.asList(Constants.Body.CRAMPS),
                null));
        items.add(day(startDate, 2, Constants.BleedingSize.LIGHT, true,
                Arrays.asList(Constants.Emotions.SAD),
                Arrays.asList(Constants.Body.HEADACHE),
                null));
        items.add(day(startDate, 3, Constants.BleedingSize.SPOTING, true,
                Arrays.asList(Constants.Emotions.ENERGETIC),
                new ArrayList<>(),
                "Synthetic tapering bleeding in cycle " + cycleNumber + "."));
        items.add(day(startDate, 10, null, false,
                Arrays.asList(Constants.Emotions.HAPPY),
                Arrays.asList(Constants.Body.DISCHARGE),
                null));
        items.add(day(startDate, 13, null, false,
                Arrays.asList(Constants.Emotions.HORNY),
                Arrays.asList(Constants.Body.OVULATION),
                null));
        items.add(day(startDate, 21, null, false,
                Arrays.asList(cycleIndex % 2 == 0 ? Constants.Emotions.CALM : Constants.Emotions.STRESSED),
                Arrays.asList(Constants.Body.BLOATING),
                null));

        if (cycleIndex % 2 == 1) {
            items.add(day(startDate, 22, Constants.BleedingSize.SPOTING, false,
                    Arrays.asList(Constants.Emotions.STRESSED),
                    Arrays.asList(Constants.Body.STOMACHACHE),
                    "Synthetic non-menstrual spotting in cycle " + cycleNumber + "."));
        }

        if (cycleIndex == 0) {
            CalendarItem first = items.get(0);
            first.getSexualProtectionPregnancyCounter().set(0, 1);
            first.setContraceptionPills(Constants.ContraceptionPills.TOOK);
        }

        return items;
    }

    private static CalendarItem day(Date startDate, int offset, Constants.BleedingSize bleedingSize, boolean includeCycleSummary, List<Constants.Emotions> emotions, List<Constants.Body> body, String note) {
        CalendarItem item = new CalendarItem(CycleIgDates.addDays(startDate, offset));
        item.setBleedingSize(bleedingSize);
        item.setIncludeCycleSummary(bleedingSize != null && includeCycleSummary);
        item.setEmotions(new ArrayList<>(emotions));
        item.setBody(new ArrayList<>(body));
        item.setBleedingProductsCounter(new ArrayList<>(Collections.nCopies(7, 0)));
        item.setBleedingClotsCounter(new ArrayList<>(Collections.nCopies(2, 0)));

        if (bleedingSize != null) {
            item.getBleedingProductsCounter().set(bleedingSize == Constants.BleedingSize.SPOTING ? 6 : 2, 1);
            if (bleedingSize == Constants.BleedingSize.HEAVY) {
                item.getBleedingClotsCounter().set(0, 1);
            }
        }
        if (note != null) {
            item.setNote(note);
        }
        return item;
    }
}
