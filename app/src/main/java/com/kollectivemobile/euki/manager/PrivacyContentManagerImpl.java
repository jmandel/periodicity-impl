package com.kollectivemobile.euki.manager;

import com.kollectivemobile.euki.model.ContentItem;
import com.kollectivemobile.euki.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrivacyContentManagerImpl implements PrivacyContentManager {
    private AppSettingsManager mAppSettingsManager;

    public PrivacyContentManagerImpl(AppSettingsManager appSettingsManager) {
        this.mAppSettingsManager = appSettingsManager;
    }

    @Override
    public ContentItem getPrivacyFAQs() {
        String pinCode = mAppSettingsManager.getPinCode();
        String fakePin = (pinCode == null || pinCode.equals("2222")) ? "1111" : "2222";

        ContentItem contentItem = new ContentItem("privacy_faq");
        contentItem.setId("privacy_faq");

        List<ContentItem> childItems = new ArrayList<>();
        for (int i=1; i<=4; i++) {
            String text = "privacy_faqs_title_" + i;
            String content = Utils.getLocalized("privacy_faqs_content_" + i).replace("[FAKE_PIN]", fakePin);

            ContentItem childItem = new ContentItem(text);
            childItem.setContent(content);
            childItem.setItemId(i);
            childItems.add(childItem);
        }
        contentItem.setExpandableItems(childItems);

        return contentItem;
    }

    @Override
    public ContentItem getPrivacyStatement() {
        ContentItem item = new ContentItem("privacy_statement");
        item.setContent("privacy_statement_content");
        return item;
    }

    @Override
    public ContentItem getPrivacyBestPractices() {
        ContentItem item = new ContentItem("Privacy_best_practices_header");
        item.setImageIcon("icon_privacy_best_practices");
        item.setContent("Privacy_best_practices_copy");

        List<ContentItem> childItems = new ArrayList<>();

        String textMessages = "Protect_your_private_messages_dropdown";
        String contentMessages = "Protect_your_private_messages_copy";

        Map<String, String> linksMessages = new HashMap<>();
        linksMessages.put("Signal", "https://signal.org/download/");

        ContentItem childItemMessages = new ContentItem(textMessages);
        childItemMessages.setContent(contentMessages);
        childItemMessages.setLinks(linksMessages);
        childItemMessages.setItemId(0);
        childItems.add(childItemMessages);

        String textHistory = "Protect_your_internet_search_history_dropdown";
        String contentHistory = "Protect_your_internet_search_history_copy";

        Map<String, String> linksHistory = new HashMap<>();
        linksHistory.put("Tunnelbear", "https://www.vpnranks.com/reviews/tunnelbear/free-trial/");
        linksHistory.put("ProtonVPN", "https://protonvpn.com/");
        linksHistory.put("Nivel gratuito de ProtonVPN", "https://protonvpn.com/"); // Adjusted
        linksHistory.put("Nivel gratuito limitado de Tunnelbear", "https://www.tunnelbear.com/");
        linksHistory.put("DuckDuckGo", "https://duckduckgo.com/");
        linksHistory.put("Firefox Focus", "https://play.google.com/store/apps/details?id=org.mozilla.focus&hl=es&gl=ES"); // Adjusted

        ContentItem childItemHistory = new ContentItem(textHistory);
        childItemHistory.setContent(contentHistory);
        childItemHistory.setLinks(linksHistory);
        childItemHistory.setItemId(1);
        childItems.add(childItemHistory);

        String textPaymentHistory = "Protect_your_payment_history_dropdown";
        String contentPaymentHistory = "Protect_your_payment_history_copy";

        ContentItem childItemPaymentHistory = new ContentItem(textPaymentHistory);
        childItemPaymentHistory.setContent(contentPaymentHistory);
        childItemPaymentHistory.setItemId(2);
        childItems.add(childItemPaymentHistory);

        String textLocation = "Protect_your_location_data_and_ad_tracking_dropdown";
        String contentLocation = "Protect_your_location_data_and_ad_tracking_copy";

        Map<String, String> linksLocation = new HashMap<>();
        linksLocation.put("DuckDuckGo", "https://duckduckgo.com/");
        linksLocation.put("Firefox Focus", "https://play.google.com/store/apps/details?id=org.mozilla.focus&hl=es&gl=ES"); // Adjusted

        ContentItem childItemLocation = new ContentItem(textLocation);
        childItemLocation.setContent(contentLocation);
        childItemLocation.setLinks(linksLocation);
        childItemLocation.setItemId(3);
        childItems.add(childItemLocation);

        String textResources = "Resources_dropdown";
        String contentResources = "Resources_copy";

        Map<String, String> linksResources = new HashMap<>();
        linksResources.put("Digital Defense Fund", "https://digitaldefensefund.org/ddf-guides/abortion-privacy");
        linksResources.put("Electronic Frontier Foundation privacy guidance", "https://www.eff.org/issues/privacy");
        linksResources.put("If/When/How", "https://ifwhenhow.org/");
        linksResources.put("Repro Legal Helpline", "https://ifwhenhow.org/");
        ContentItem childItemResources = new ContentItem(textResources);
        childItemResources.setContent(contentResources);
        childItemResources.setLinks(linksResources);
        childItemResources.setItemId(4);
        childItems.add(childItemResources);

        item.setExpandableItems(childItems);

        return item;
    }
}
