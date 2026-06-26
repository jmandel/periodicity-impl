package com.kollectivemobile.euki.cycleig;

public class CycleIgShare {
    public final String id;
    public final String fileUrl;
    public final String manageToken;
    public final String viewerLink;
    public final String key;
    public final long exp;
    public final int maxUses;
    public final String bundleJson;
    public final String jwe;

    public CycleIgShare(String id, String fileUrl, String manageToken, String viewerLink, String key, long exp, int maxUses, String bundleJson, String jwe) {
        this.id = id;
        this.fileUrl = fileUrl;
        this.manageToken = manageToken;
        this.viewerLink = viewerLink;
        this.key = key;
        this.exp = exp;
        this.maxUses = maxUses;
        this.bundleJson = bundleJson;
        this.jwe = jwe;
    }
}
