/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.helpers.RTSUtil;
import edu.illinois.starts.helpers.Writer;
import edu.illinois.starts.helpers.ZLCHelper;
import edu.illinois.starts.util.Logger;
import edu.illinois.starts.util.Pair;
import edu.illinois.yasgl.DirectedGraph;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Find all types that are impacted by a change.
 */
//@Mojo(name = "impacted", requiresDirectInvocation = true, requiresDependencyResolution = ResolutionScope.TEST)
//@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class ImpactedTask extends DiffTask {

    private Logger logger;

    public void executeTask() throws Exception {
        Logger.getGlobal().setLoggingLevel(Level.parse(mExtention.getLoggingLevel()));
        logger = Logger.getGlobal();
        Pair<Set<String>, Set<String>> data = computeChangeData();
        // 0. Find all classes in program
        List<String> allClasses = getAllClasses();
        if (allClasses.isEmpty()) {
            logger.log(Level.INFO, "There are no .class files in this module.");
            return;
        }
        Set<String>  impacted = new HashSet<>(allClasses);
        // 1a. Find what changed and what is non-affected
        Set<String> nonAffected = data == null ? new HashSet<String>() : data.getKey();
        Set<String> changed = data == null ? new HashSet<String>() : data.getValue();

        // 1b. Remove nonAffected from all classes to get classes impacted by the change
        impacted.removeAll(nonAffected);

        logger.log(Level.FINEST, "CHANGED: " + changed.toString());
        logger.log(Level.FINEST, "IMPACTED: " + impacted.toString());
        // 2. Optionally find newly-added classes
        if (mExtention.isTrackNewClasses()) {
            Set<String> newClasses = new HashSet<>(allClasses);
            Set<String> oldClasses = ZLCHelper.getExistingClasses(getArtifactsDir());
            newClasses.removeAll(oldClasses);
            logger.log(Level.FINEST, "NEWLY-ADDED: " + newClasses.toString());
            Writer.writeToFile(newClasses, "new-classes", getArtifactsDir());
        }
        // 3. Optionally update ZLC file for next run, using all classes in the SUT
        if (mExtention.isUpdateImpactedChecksums()) {
            updateForNextRun(allClasses);
        }
        // 4. Print impacted and/or write to file
        Writer.writeToFile(changed, "changed-classes", getArtifactsDir());
        Writer.writeToFile(impacted, "impacted-classes", getArtifactsDir());
    }

    private void updateForNextRun(List<String> allClasses) throws GradleException {
        long start = System.currentTimeMillis();
        //Classpath sfClassPath = getSureFireClassPath();
        FileCollection classpathFS = getClasspath();
        String sfPathString = classpathFS.getAsPath();// Writer.pathToString(classpathFS.getAsPath());
        ClassLoader loader = createClassLoader(classpathFS);
        Result result = prepareForNextRun(sfPathString, classpathFS, allClasses, new HashSet<String>(), false);
        ZLCHelper zlcHelper = new ZLCHelper();
        zlcHelper.updateZLCFile(result.getTestDeps(), loader, getArtifactsDir(), new HashSet<String>());
        long end = System.currentTimeMillis();
        if (logger.getLoggingLevel().intValue() == Level.FINER.intValue()) {
            Writer.writeClassPath(sfPathString, getArtifactsDir());
        }
        if (logger.getLoggingLevel().intValue() <= Level.FINEST.intValue()) {
            save(getArtifactsDir(), result.getGraph());
        }
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(total): " + Writer.millsToSeconds(end - start));
    }

    private void save(String artifactsDir, DirectedGraph<String> graph) {
        RTSUtil.saveForNextRun(artifactsDir, graph,
                mExtention.getPrintGraph(), mExtention.getGraphFile());
    }
}
