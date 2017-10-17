/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.maven.AgentLoader;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import org.gradle.api.tasks.TaskAction;

import java.util.*;
import java.util.logging.Level;

import static edu.illinois.starts.constants.StartsConstants.STARTS_EXCLUDE_PROPERTY;

/**
 * Prepares for test runs by writing non-affected tests in the excludesFile.
 */
public class RunTask extends DiffTask {

    protected Set<String> nonAffectedTests;
    protected Set<String> changedClasses;
    private Logger logger;

    @TaskAction
    public void executeTask() throws Exception {
        Logger.getGlobal().setLoggingLevel(Level.parse(mExtention.getLoggingLevel()));
        logger = Logger.getGlobal();
        long start = System.currentTimeMillis();
        setChangedAndNonaffected();
        List<String> excludePaths = Writer.fqnsToExcludePath(nonAffectedTests);
        setIncludesExcludes();
        if (logger.getLoggingLevel().intValue() <= Level.FINEST.intValue()) {
            Writer.writeToFile(nonAffectedTests, "non-affected-tests", getArtifactsDir());
        }
        run(excludePaths);
        Set<String> allTests = new HashSet<>(getTestClasses("checkIfAllAffected"));
        if (allTests.equals(nonAffectedTests)) {
            logger.log(Level.INFO, "********** Run **********");
            logger.log(Level.INFO, "No tests are selected to run.");
        }
        long end = System.currentTimeMillis();
        System.setProperty("[PROFILE] END-OF-RUN-MOJO: ", Long.toString(end));
        logger.log(Level.FINE, "[PROFILE] RUN-MOJO-TOTAL: " + Writer.millsToSeconds(end - start));
    }

    protected void run(List<String> excludePaths) throws Exception {
        if (mExtention.isRetestAll()) {
            dynamicallyUpdateExcludes(new ArrayList<String>());
        } else {
            dynamicallyUpdateExcludes(excludePaths);
        }
        long startUpdateTime = System.currentTimeMillis();
        if (mExtention.isUpdateRunChecksums()) {
            updateForNextRun(nonAffectedTests);
        }
        long endUpdateTime = System.currentTimeMillis();
        logger.log(Level.FINE, "[PROFILE] STARTS-MOJO-UPDATE-TIME: "
                + Writer.millsToSeconds(endUpdateTime - startUpdateTime));
    }

    private void dynamicallyUpdateExcludes(List<String> excludePaths) {
        if (AgentLoader.loadDynamicAgent()) {
            logger.log(Level.FINEST, "AGENT LOADED!!!");
            System.setProperty(STARTS_EXCLUDE_PROPERTY, Arrays.toString(excludePaths.toArray(new String[0])));
        } else {
            //throw new MojoExecutionException("I COULD NOT ATTACH THE AGENT");
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
