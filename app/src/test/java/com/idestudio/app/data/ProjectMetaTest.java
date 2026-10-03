package com.idestudio.app.data;

import com.idestudio.app.data.models.ProjectMeta;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProjectMetaTest {

    @Test
    public void testProjectMetaCreation() {
        ProjectMeta meta = new ProjectMeta("DemoApp", "com.demo.app", "/path/to/project", 21, 34, "Empty Activity");

        assertNotNull(meta.getId());
        assertEquals("DemoApp", meta.getName());
        assertEquals("com.demo.app", meta.getPackageName());
        assertEquals("Java", meta.getLanguage());
        assertEquals(21, meta.getMinSdk());
        assertEquals(34, meta.getTargetSdk());
    }
}
