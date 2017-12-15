/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.EkstaziHelper;
import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import edu.illinois.yasgl.DirectedGraph;
import org.gradle.api.GradleException;
import org.gradle.api.logging.LogLevel;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

/**
 * Finds types that have changed since the last time they were analyzed.
 */
public class DiffTask extends BaseTask {

    @Override
    protected void performTask() throws Exception {
        Logger.getGlobal().setLoggingLevel(Level.parse(getExtension().getLoggingLevel()));

        Set<String> changed = new HashSet<>();
        Set<String> nonAffected = new HashSet<>();
        Pair<Set<String>, Set<String>> data = computeChangeData();
        String extraText = "";
        if (data != null) {
            nonAffected = data.getKey();
            changed = data.getValue();
        } else {
            extraText = " (no RTS artifacts; likely the first run)";
        }
        printResult(changed, "ChangedClasses" + extraText);
        if (getExtension().getUpdateDiffChecksums()) {
            updateForNextRun(nonAffected);
        }
    }

    protected Pair<Set<String>, Set<String>> computeChangeData() throws GradleException {
        long start = System.currentTimeMillis();
        Pair<Set<String>, Set<String>> data = null;
        if (getExtension().getDepFormat() == DependencyFormat.ZLC) {
            ZLCHelper zlcHelper = new ZLCHelper();
            data = zlcHelper.getChangedData(getArtifactsDir(), getExtension().getCleanBytes());
        } else if (getExtension().getDepFormat()  == DependencyFormat.CLZ) {
            data = EkstaziHelper.getNonAffectedTests(getArtifactsDir());
        }

        Set<String> changed = data == null ? new HashSet<String>() : data.getValue();
        if (Logger.getGlobal().getLoggingLevel().intValue() <= Level.FINEST.intValue()) {
            Writer.writeToFile(changed, "changed-classes", getArtifactsDir());
        }

        long end = System.currentTimeMillis();
        getLogger().log(LogLevel.LIFECYCLE,"[PROFILE] COMPUTING CHANGES: " + Writer.millsToSeconds(end - start));
        return data;
    }

    protected void updateForNextRun(Set<String> nonAffected) throws Exception {
        long start = System.currentTimeMillis();
        Set<File> classPath = getClassPath();

        String sfPathString = fileListToPathString(new ArrayList<>(classPath));//classPath.getAsPath();

        setIncludesExcludes();
        List<String> allTests = getTestClasses("updateForNextRun");
        //getLogger().log(LogLevel.LIFECYCLE, "[allTests]: " + allTests);
        Set<String> affectedTests = new HashSet<>(allTests);
        affectedTests.removeAll(nonAffected);
        DirectedGraph<String> graph = null;

        if (!affectedTests.isEmpty()) {
            ClassLoader loader = createClassLoader(classPath);

            //TODO: set this boolean to true only for static reflectionAnalyses with * (border, string, naive)?
            boolean computeUnreached = true;
            Result result = prepareForNextRun(sfPathString, classPath, allTests, nonAffected, computeUnreached);

            Map<String, Set<String>> testDeps = result.getTestDeps();
            graph = result.getGraph();
            Set<String> unreached = computeUnreached ? result.getUnreachedDeps() : new HashSet<String>();
            if (getExtension().getDepFormat()  == DependencyFormat.ZLC) {
                ZLCHelper zlcHelper = new ZLCHelper();
                zlcHelper.updateZLCFile(testDeps, loader, getArtifactsDir(), unreached);
            } else if (getExtension().getDepFormat()  == DependencyFormat.CLZ) {
                // The next line is not needed with ZLC because '*' is explicitly tracked in ZLC
                affectedTests = result.getAffectedTests();
                if (affectedTests == null) {
                    throw new GradleException("Affected tests should not be null with CLZ format!");
                }
                RTSUtil.computeAndSaveNewCheckSums(getArtifactsDir(), affectedTests, testDeps, loader);
            }
        }

        save(getArtifactsDir(), affectedTests, allTests, sfPathString, graph);
        printToTerminal(allTests, affectedTests);
        long end = System.currentTimeMillis();
        getLogger().log(LogLevel.INFO, "[PROFILE] updateForNextRun(total): " + Writer.millsToSeconds(end - start));
    }

    public void printToTerminal(List<String> testClasses, Set<String> affectedTests) {
        getLogger().log(LogLevel.LIFECYCLE, "STARTS:AffectedTests: " + affectedTests.size());
        getLogger().log(LogLevel.LIFECYCLE, "STARTS:TotalTests: " + testClasses.size());
    }

    public void save(String artifactsDir, Set<String> affectedTests, List<String> testClasses,
                     String sfPathString, DirectedGraph<String> graph) {
        int globalLogLevel = Logger.getGlobal().getLoggingLevel().intValue();
        if (globalLogLevel <= Level.FINER.intValue()) {
            Writer.writeToFile(testClasses, "all-tests", artifactsDir);
            Writer.writeToFile(affectedTests, "selected-tests", artifactsDir);
        }
        if (globalLogLevel <= Level.FINEST.intValue()) {
            RTSUtil.saveForNextRun(artifactsDir, graph, getExtension().getPrintGraph(),
                    getExtension().getGraphFile());
            Writer.writeClassPath(sfPathString, artifactsDir);
        }
    }
}
