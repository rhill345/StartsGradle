/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import org.gradle.api.logging.LogLevel;

import java.util.logging.Level;

/**
 * Invoked after after running selected tests (see lifecycle.xml for details).
 */
public class StartsTask extends RunTask {
    //private Logger logger;

    public void executeTest() throws Exception {
        long endOfRunMojo = Long.parseLong(System.getProperty("[PROFILE] END-OF-RUN-MOJO: "));
        Logger.getGlobal().setLoggingLevel(Level.parse(mExtention.getLoggingLevel()));
       // logger = Logger.getGlobal();
        long end = System.currentTimeMillis();
        getLogger().log(LogLevel.LIFECYCLE,  "[PROFILE] TEST-RUNNING-TIME: " + Writer.millsToSeconds(end - endOfRunMojo));
        getLogger().log(LogLevel.LIFECYCLE, "[PROFILE] STARTS-MOJO-TOTAL: " + Writer.millsToSeconds(end - endOfRunMojo));
    }
}
