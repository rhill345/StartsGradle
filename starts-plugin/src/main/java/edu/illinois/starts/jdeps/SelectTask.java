/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Pair;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskAction;

import java.util.HashSet;
import java.util.Set;

/**
 * Finds tests affected by a change but does not run them.
 */
//@Mojo(name = "select", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
//@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class SelectTask extends DiffTask {
    /**
     * Set this to "true" to update test dependencies on disk. The default value of
     * "false" is useful for "dry runs" where one may want to see the affected
     * tests, without updating test dependencies.
     */
   /* @Parameter(property = "updateSelectChecksums", defaultValue = "false")
    private boolean updateSelectChecksums;

    private Logger logger;*/

    @TaskAction
    public void executeTask() throws Exception {
        //Logger.getGlobal().setLoggingLevel(Level.parse(loggingLevel));
        //logger = Logger.getGlobal();
        long start = System.currentTimeMillis();
        Set<String> affectedTests = computeAffectedTests();
        printResult(affectedTests, "AffectedTests");
        long end = System.currentTimeMillis();
        getLogger().log(LogLevel.LIFECYCLE, "[PROFILE] RUN-MOJO-TOTAL: " + Writer.millsToSeconds(end - start));
        getLogger().log(LogLevel.LIFECYCLE, "[PROFILE] TEST-RUNNING-TIME: " + 0.0);
    }

    private Set<String> computeAffectedTests() throws Exception {
        setIncludesExcludes();
        Set<String> allTests = new HashSet<>(getTestClasses("checkIfAllAffected"));
        Set<String> affectedTests = new HashSet<>(allTests);
        Pair<Set<String>, Set<String>> data = computeChangeData();
        Set<String> nonAffectedTests = data == null ? new HashSet<String>() : data.getKey();
        affectedTests.removeAll(nonAffectedTests);
        if (allTests.equals(nonAffectedTests)) {
            getLogger().log(LogLevel.LIFECYCLE, "********** Run **********");
            getLogger().log(LogLevel.LIFECYCLE, "No tests are selected to run.");
        }
        long startUpdate = System.currentTimeMillis();
        if (mExtention.isUpdateRunChecksums()) {
            updateForNextRun(nonAffectedTests);
        }
        long endUpdate = System.currentTimeMillis();
        getLogger().log(LogLevel.LIFECYCLE, "[PROFILE] STARTS-MOJO-UPDATE-TIME: " + Writer.millsToSeconds(endUpdate - startUpdate));
        return affectedTests;
    }
}
