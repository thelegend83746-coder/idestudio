package com.idestudio.app.data.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SdkInfo implements Serializable {
    private final int apiLevel;
    private final String codeName;
    private final String versionName;

    public SdkInfo(int apiLevel, String codeName, String versionName) {
        this.apiLevel = apiLevel;
        this.codeName = codeName;
        this.versionName = versionName;
    }

    public int getApiLevel() {
        return apiLevel;
    }

    public String getCodeName() {
        return codeName;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getDisplayText() {
        return "API " + apiLevel + " — " + codeName + " (" + versionName + ")";
    }

    public static List<SdkInfo> getSupportedSdkList() {
        List<SdkInfo> list = new ArrayList<>();
        list.add(new SdkInfo(16, "Jelly Bean", "Android 4.1"));
        list.add(new SdkInfo(19, "KitKat", "Android 4.4"));
        list.add(new SdkInfo(21, "Lollipop", "Android 5.0"));
        list.add(new SdkInfo(23, "Marshmallow", "Android 6.0"));
        list.add(new SdkInfo(24, "Nougat", "Android 7.0"));
        list.add(new SdkInfo(26, "Oreo", "Android 8.0"));
        list.add(new SdkInfo(28, "Pie", "Android 9.0"));
        list.add(new SdkInfo(29, "Android 10", "Android 10.0"));
        list.add(new SdkInfo(30, "Android 11", "Android 11.0"));
        list.add(new SdkInfo(31, "Android 12", "Android 12.0"));
        list.add(new SdkInfo(33, "Android 13", "Android 13.0"));
        list.add(new SdkInfo(34, "Android 14", "Android 14.0"));
        return list;
    }
}
