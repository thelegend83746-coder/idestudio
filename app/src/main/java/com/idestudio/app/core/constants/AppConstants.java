package com.idestudio.app.core.constants;

public final class AppConstants {
    private AppConstants() {}

    // Storage Paths strictly within /storage/emulated/0/test-folder
    public static final String ROOT_IDE_DIR = "/storage/emulated/0/test-folder/ide-studio";
    public static final String PROJECTS_DIR = ROOT_IDE_DIR + "/projects";
    public static final String METADATA_DIR = ROOT_IDE_DIR + "/.metadata";
    public static final String PROJECTS_META_FILE = METADATA_DIR + "/projects.json";

    // Intent Extras
    public static final String EXTRA_PROJECT_ID = "extra_project_id";
    public static final String EXTRA_PROJECT_PATH = "extra_project_path";
    public static final String EXTRA_TEMPLATE_NAME = "extra_template_name";

    // Templates
    public static final String TEMPLATE_EMPTY_ACTIVITY = "Empty Activity";
    public static final String TEMPLATE_BASIC_ACTIVITY = "Basic Activity";
    public static final String TEMPLATE_JETPACK = "Jetpack";
    public static final String TEMPLATE_COMPOSE = "Compose";

    // Defaults
    public static final int DEFAULT_MIN_SDK = 21;
    public static final int DEFAULT_TARGET_SDK = 34;
    public static final String DEFAULT_LANGUAGE = "Java";
}
