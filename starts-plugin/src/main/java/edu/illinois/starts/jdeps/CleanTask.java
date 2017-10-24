/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.FileUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.logging.Level;

/**
 * Removes STARTS plugin artifacts.
 */
public class CleanTask extends BaseTask {

    @TaskAction
    public void executeTask() throws Exception {
        getLogger().log(LogLevel.LIFECYCLE, "[CLEAN] RUNNING CLEAN COMMAND");
        File directory = new File(getArtifactsDir());
        getLogger().log(LogLevel.LIFECYCLE, "[CLEAN] CHECKING DIRECTORY " + directory.getPath() );
        if (directory.exists()) {
            getLogger().log(LogLevel.LIFECYCLE, "[CLEAN] DELETING DIRECTORY " );
            FileUtil.delete(directory);
        }
    }
}
