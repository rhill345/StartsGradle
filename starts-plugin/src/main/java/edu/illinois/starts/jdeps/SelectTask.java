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

public class SelectTask extends DiffTask {

    @TaskAction
    public void executeTask() throws Exception {
        long start = System.currentTimeMillis();
        Set<String> affectedTests = computeAffectedTests();
        printResult(affectedTests, "AffectedTests");
        long end = System.currentTimeMillis();
        getLogger().log(LogLevel.LIFECYCLE, "[PROFILE] RUN-TASK-TOTAL: " + Writer.millsToSeconds(end - start));
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

    private void printSet(String name, Set<String> set) {
        for (String s : set) {
            getLogger().log(LogLevel.LIFECYCLE, "[" + name + "] " + s);
        }
    }
}
