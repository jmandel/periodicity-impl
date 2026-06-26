package com.kollectivemobile.euki.manager;

import com.kollectivemobile.euki.model.ContentItem;

public interface AbortionContentManager {
    ContentItem getAbortionMifeMiso12();

    ContentItem getAbortionMiso12();

    ContentItem getAbortionMifeMiso12NoExpandables();

    ContentItem getAbortionMiso12NoExpandables();

    ContentItem getAbortionSuctionVacuum();

    ContentItem getAbortionDilationEvacuation();

    ContentItem getAbortionSuctionVacuumNoExpandables();

    ContentItem getAbortionDilationEvacuationNoExpandables();

    ContentItem getMedialAbortion();
}
