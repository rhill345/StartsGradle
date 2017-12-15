/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.FileUtil;

import java.io.File;

/**
 * Removes STARTS plugin artifacts.
 */
public class CleanTask extends BaseTask {

    @Override
    protected void performTask() {
        File directory = new File(getArtifactsDir());
        if (directory.exists()) {
            FileUtil.delete(directory);
        }
    }
}
