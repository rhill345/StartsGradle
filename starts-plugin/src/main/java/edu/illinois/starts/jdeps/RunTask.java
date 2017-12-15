/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.testing.Test;

import java.util.*;
import java.util.logging.Level;

/**
 * Prepares for test runs by writing non-affected tests in the excludesFile.
 */
public class RunTask extends DiffTask {

    protected Set<String> nonAffectedTests;
    protected Set<String> changedClasses;
    protected long runEndTime;
    private Logger logger;

    @Override
    public void performTask() throws Exception {
        Logger.getGlobal().setLoggingLevel(Level.parse(getExtension().getLoggingLevel()));
        logger = Logger.getGlobal();
        long start = System.currentTimeMillis();
        setChangedAndNonaffected();
        List<String> excludePaths = Writer.fqnsToExcludePath(nonAffectedTests);
        setIncludesExcludes();
        if (logger.getLoggingLevel().intValue() <= Level.FINEST.intValue()) {
            Writer.writeToFile(nonAffectedTests, "non-affected-tests", getArtifactsDir());
        }
        Set<String> allTests = new HashSet<>(getTestClasses("checkIfAllAffected"));
        run(excludePaths);
        if (allTests.equals(nonAffectedTests)) {
            getLogger().log(LogLevel.LIFECYCLE,  "********** Run **********");
            getLogger().log(LogLevel.LIFECYCLE,  "No tests are selected to run.");
        }
        long runEndTime = System.currentTimeMillis();
        System.setProperty("[PROFILE] END-OF-RUN-TASK: ", Long.toString(runEndTime));
        getLogger().log(LogLevel.LIFECYCLE, "[PROFILE] RUN-TASK-TOTAL: " + Writer.millsToSeconds(runEndTime - start));
    }

    protected void run(List<String> excludePaths) throws Exception {
        if (getExtension().getRetestAll()) {
            dynamicallyUpdateExcludes(new ArrayList<String>());
        } else {
            dynamicallyUpdateExcludes(excludePaths);
        }
        long startUpdateTime = System.currentTimeMillis();
        if (getExtension().getUpdateRunChecksums()) {
            updateForNextRun(nonAffectedTests);
        }
        long endUpdateTime = System.currentTimeMillis();
        getLogger().log(LogLevel.LIFECYCLE, "[PROFILE] STARTS-TASK-UPDATE-TIME: "
                + Writer.millsToSeconds(endUpdateTime - startUpdateTime));
    }

    private void dynamicallyUpdateExcludes(List<String> excludePaths) {
        for (Task task : getProject().getTasks().withType(Test.class)) {
            List<String> toExclude = new ArrayList<>(excludePaths);
            toExclude.addAll(((Test)task).getExcludes());
            ((Test)task).setExcludes(toExclude);
        }
    }

    protected void setChangedAndNonaffected() throws Exception {
        nonAffectedTests = new HashSet<>();
        changedClasses = new HashSet<>();
        Pair<Set<String>, Set<String>> data = computeChangeData();
        nonAffectedTests = data == null ? new HashSet<String>() : data.getKey();
        changedClasses  = data == null ? new HashSet<String>() : data.getValue();
    }
}
