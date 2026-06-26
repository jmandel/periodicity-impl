package com.kollectivemobile.euki.cycleig;

public class CycleIgScope {
    private final boolean includeEmotions;
    private final boolean includeBody;
    private final boolean includeNotes;

    public CycleIgScope(boolean includeEmotions, boolean includeBody, boolean includeNotes) {
        this.includeEmotions = includeEmotions;
        this.includeBody = includeBody;
        this.includeNotes = includeNotes;
    }

    public static CycleIgScope all() {
        return new CycleIgScope(true, true, true);
    }

    public boolean includeEmotions() {
        return includeEmotions;
    }

    public boolean includeBody() {
        return includeBody;
    }

    public boolean includeNotes() {
        return includeNotes;
    }
}
