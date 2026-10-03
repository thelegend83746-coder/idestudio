package com.idestudio.app.data.models;

import com.idestudio.app.core.constants.AppConstants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TemplateModel implements Serializable {
    private final String id;
    private final String name;
    private final String description;
    private boolean isSelected;

    public TemplateModel(String id, String name, String description, boolean isSelected) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isSelected = isSelected;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public static List<TemplateModel> getTemplates() {
        List<TemplateModel> list = new ArrayList<>();
        list.add(new TemplateModel("empty", AppConstants.TEMPLATE_EMPTY_ACTIVITY, "Creates a simple, clean activity with a single layout XML file.", true));
        list.add(new TemplateModel("basic", AppConstants.TEMPLATE_BASIC_ACTIVITY, "Creates an activity with a standard Material toolbar and a floating action button.", false));
        list.add(new TemplateModel("jetpack", AppConstants.TEMPLATE_JETPACK, "Sets up Android Jetpack components with ViewModel and lifecycle architecture.", false));
        list.add(new TemplateModel("compose", AppConstants.TEMPLATE_COMPOSE, "Sets up an activity configured for Android Compose UI architecture.", false));
        return list;
    }
}
