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
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskAction;

import java.util.*;
import java.util.logging.Level;

/**
 * Finds types that have changed since the last time they were analyzed.
 */

public class DiffTask extends BaseTask{

    @TaskAction
    public void executeTask() throws Exception {
        Logger.getGlobal().setLoggingLevel(Level.parse(mExtention.getLoggingLevel()));

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
        if (mExtention.getUpdateDiffChecksums()) {
            updateForNextRun(nonAffected);
        }
    }

    protected Pair<Set<String>, Set<String>> computeChangeData() throws GradleException {
        long start = System.currentTimeMillis();
        Pair<Set<String>, Set<String>> data = null;
        getLogger().log(LogLevel.LIFECYCLE,"[computeChangeData] COMPUTING CHANGES: ");

        if (mExtention.getDepFormat() == DependencyFormat.ZLC) {
            getLogger().log(LogLevel.LIFECYCLE,"[DependencyFormat.ZLC]");
            ZLCHelper zlcHelper = new ZLCHelper();
            data = zlcHelper.getChangedData(getArtifactsDir(), mExtention.getCleanBytes());
        } else if (mExtention.getDepFormat()  == DependencyFormat.CLZ) {
            getLogger().log(LogLevel.LIFECYCLE,"[computeChangeData] COMPUTING CHANGES: ");
            data = EkstaziHelper.getNonAffectedTests(getArtifactsDir());
        }
        getLogger().log(LogLevel.LIFECYCLE,"[DONE] getDepFormat: ");
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
        FileCollection classPath = getClassPath();

        String sfPathString = classPath.getAsPath();;//Writer.pathToString(new ArrayList(classPath.getFiles()));
        getLogger().log(LogLevel.LIFECYCLE, "[sfPathString] " + sfPathString);
        setIncludesExcludes();
        List<String> allTests = getTestClasses("updateForNextRun");
        Set<String> affectedTests = new HashSet<>(allTests);
        affectedTests.removeAll(nonAffected);
        DirectedGraph<String> graph = null;
        if (!affectedTests.isEmpty()) {
            ClassLoader loader = createClassLoader(classPath);
            getLogger().log(LogLevel.LIFECYCLE, "Done getting ClassLoader");
            //TODO: set this boolean to true only for static reflectionAnalyses with * (border, string, naive)?
            boolean computeUnreached = true;
            Result result = prepareForNextRun(sfPathString, classPath, allTests, nonAffected, computeUnreached);
            getLogger().log(LogLevel.LIFECYCLE, "Done getting prepareForNextRun");
            Map<String, Set<String>> testDeps = result.getTestDeps();
            graph = result.getGraph();
            Set<String> unreached = computeUnreached ? result.getUnreachedDeps() : new HashSet<String>();
            if (mExtention.getDepFormat()  == DependencyFormat.ZLC) {
                ZLCHelper zlcHelper = new ZLCHelper();
                getLogger().log(LogLevel.LIFECYCLE, "testDeps: " + testDeps);
                getLogger().log(LogLevel.LIFECYCLE, "unreached: " + unreached.size());
                zlcHelper.updateZLCFile(testDeps, loader, getArtifactsDir(), unreached);
            } else if (mExtention.getDepFormat()  == DependencyFormat.CLZ) {
                // The next line is not needed with ZLC because '*' is explicitly tracked in ZLC
                affectedTests = result.getAffectedTests();
                if (affectedTests == null) {
                    //throw new MojoExecutionException("Affected tests should not be null with CLZ format!");
                }
                RTSUtil.computeAndSaveNewCheckSums(getArtifactsDir(), affectedTests, testDeps, loader);
            }
        }
        getLogger().log(LogLevel.LIFECYCLE, "start save");
        save(getArtifactsDir(), affectedTests, allTests, sfPathString, graph);
        getLogger().log(LogLevel.LIFECYCLE, "Done save");
        printToTerminal(allTests, affectedTests);
        long end = System.currentTimeMillis();
        getLogger().log(LogLevel.LIFECYCLE, "[PROFILE] updateForNextRun(total): " + Writer.millsToSeconds(end - start));
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
            RTSUtil.saveForNextRun(artifactsDir, graph, mExtention.getPrintGraph(), mExtention.getGraphFile());
            Writer.writeClassPath(sfPathString, artifactsDir);
        }
    }
}
