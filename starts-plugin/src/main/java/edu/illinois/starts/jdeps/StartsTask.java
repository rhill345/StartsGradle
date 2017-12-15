/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import org.gradle.api.logging.LogLevel;

import java.util.logging.Level;

/**
 * Invoked after after running selected tests.
 */
public class StartsTask extends RunTask {

    @Override
    public void performTask() throws Exception {
        super.performTask();
        Logger.getGlobal().setLoggingLevel(Level.parse(getExtension().getLoggingLevel()));
        long end = System.currentTimeMillis();
        getLogger().log(LogLevel.LIFECYCLE,  "[PROFILE] TEST-RUNNING-TIME: " + Writer.millsToSeconds(end - runEndTime));
        getLogger().log(LogLevel.LIFECYCLE, "[PROFILE] STARTS-TASK-TOTAL: " + Writer.millsToSeconds(end - runEndTime));
    }
}
