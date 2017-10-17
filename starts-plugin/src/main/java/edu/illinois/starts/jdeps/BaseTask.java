/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.*;
import edu.illinois.starts.util.Logger;
import edu.illinois.yasgl.DirectedGraph;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.testing.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import java.util.logging.Level;

import static edu.illinois.starts.constants.StartsConstants.STARTS_DIRECTORY_PATH;

/**
 * Base MOJO for the JDeps-Based STARTS.
 */
abstract class BaseTask extends Test {


    /**
     * The directory in which to store STARTS artifacts that are needed between runs.
     */
    protected String artifactsDir;


    private FileCollection mClassPath;

    protected final StartsPluginExtension mExtention;

    protected BaseTask() {
        mExtention = new StartsPluginExtension(getProject());
    }

    protected void printResult(Set<String> set, String title) {
        Writer.writeToLog(set, title, Logger.getGlobal());
    }

    public String getArtifactsDir() throws Exception {
        if (artifactsDir == null) {
            // TODO: make sure diretory is the projectbeing tested
            artifactsDir = getWorkingDir().getAbsolutePath() + File.separator + STARTS_DIRECTORY_PATH;
            File file = new File(artifactsDir);
            if (!file.mkdirs() && !file.exists()) {
                throw new Exception("I could not create artifacts dir: " + artifactsDir);
            }
        }
        return artifactsDir;
    }

    public void setIncludesExcludes() {
        long start = System.currentTimeMillis();

        // TODO: Ask what we are excludeing / including here

        Set<String> includes = getProject().fileTree("test").getIncludes();
        Set<String> excludes = getProject().fileTree("test").getExcludes();
        setIncludes(includes);
        setExcludes(excludes);
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(setIncludesExcludes): "
                + Writer.millsToSeconds(end - start));
    }

    public List getTestClasses(String methodName) {
        long start = System.currentTimeMillis();

       List<String> classes = new ArrayList<>();
       Set<File> testFiles = getTestClassesDirs().getFiles();
       for (File f : testFiles) {
           classes.add(f.getName());
       }

        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] " + methodName + "(getTestClasses): "
                + Writer.millsToSeconds(end - start));
        return classes;
    }

    private List getProjectClasses(String methodName) {
        long start = System.currentTimeMillis();

        List<String> classes = new ArrayList<>();
        Set<File> projectFiles = getClasspath().getFiles();
        for (File f : projectFiles) {
            classes.add(f.getName());
        }

        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] " + methodName + "(getProjectClasses): "
                + Writer.millsToSeconds(end - start));
        return classes;
    }

    public ClassLoader createClassLoader(FileCollection classPath) {
        long start = System.currentTimeMillis();
        GradleClassLoader classLoader = null;
        try {
            classLoader = new GradleClassLoader(null, "MyRole");
            for (File f : classPath){
                classLoader.addURL(f);
            }
            classLoader.setDefaultAssertionStatus(false);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(createClassLoader): "
                + Writer.millsToSeconds(end - start));
        return classLoader;
    }

    protected class Result {
        private Map<String, Set<String>> testDeps;
        private DirectedGraph<String> graph;
        private Set<String> affectedTests;
        private Set<String> unreachedDeps;

        public Result(Map<String, Set<String>> testDeps, DirectedGraph<String> graph,
                      Set<String> affectedTests, Set<String> unreached) {
            this.testDeps = testDeps;
            this.graph = graph;
            this.affectedTests = affectedTests;
            this.unreachedDeps = unreached;
        }

        public Map<String, Set<String>> getTestDeps() {
            return testDeps;
        }

        public DirectedGraph<String> getGraph() {
            return graph;
        }

        public Set<String> getAffectedTests() {
            return affectedTests;
        }

        public Set<String> getUnreachedDeps() {
            return unreachedDeps;
        }
    }

    public FileCollection getClassPath() {
        long start = System.currentTimeMillis();
        if (mClassPath == null) {
            try {
                mClassPath = getClasspath();
            } catch (Exception drre) {
                drre.printStackTrace();
            }
        }
        Logger.getGlobal().log(Level.FINEST, "SF-CLASSPATH: " + mClassPath);
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(getSureFireClassPath): "
                + Writer.millsToSeconds(end - start));
        return mClassPath;
    }

    public Result prepareForNextRun(String sfPathString, FileCollection classPath, List<String> classesToAnalyze,
                                    Set<String> nonAffected, boolean computeUnreached) {
        long start = System.currentTimeMillis();

        String m2Repo = getProject().getRepositories().mavenLocal().getUrl().getPath();
        File jdepsCache = new File(mExtention.getGraphCache());
        // We store the jdk-graphs at the root of "jdepsCache" directory, with
        // jdk.graph being the file that merges all the graphs for all standard
        // library jars.
        File libraryFile = new File(jdepsCache, "jdk.graph");
        // Create the Loadables object early so we can use its helpers
        Loadables loadables = new Loadables(classesToAnalyze, artifactsDir, sfPathString, mExtention.getFilterLib(), jdepsCache);
        // Surefire Classpath object is easier to iterate over without de-constructing
        // sfPathString (which we use in a number of other places)
        loadables.setClasspath(new ArrayList(classPath.getFiles()));

        List<String> moreEdges = new ArrayList<String>();
        long loadMoreEdges = System.currentTimeMillis();
        Cache cache = new Cache(jdepsCache, m2Repo);
        // 1. Load non-reflection edges from third-party libraries in the classpath
        cache.loadM2EdgesFromCache(moreEdges, sfPathString);
        long loadM2EdgesFromCache = System.currentTimeMillis();
        // 2. Get non-reflection edges from CUT and SDK; use (1) to build graph
        loadables.create(new ArrayList<>(moreEdges), new ArrayList(classPath.getFiles()), computeUnreached);

        Map<String, Set<String>> transitiveClosure = loadables.getTransitiveClosure();
        long createLoadables = System.currentTimeMillis();

        // We don't need to compute affected tests this way with ZLC format.
        // In RTSUtil.computeAffectedTests(), we find affected tests by (a) removing nonAffected tests from the set of
        // all tests and then (b) adding all tests that reach to * as affected if there has been a change. This is only
        // for CLZ which does not encode information about *. ZLC already encodes and reasons about * when it finds
        // nonAffected tests.
        Set<String> affected = mExtention.getDepFormat() == DependencyFormat.ZLC ? null
                : RTSUtil.computeAffectedTests(new HashSet<>(classesToAnalyze),
                nonAffected, transitiveClosure);
        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(loadMoreEdges): "
                + Writer.millsToSeconds(loadMoreEdges - start));
        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(loadM2EdgesFromCache): "
                + Writer.millsToSeconds(loadM2EdgesFromCache - loadMoreEdges));
        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(createLoadable): "
                + Writer.millsToSeconds(createLoadables - loadM2EdgesFromCache));
        Logger.getGlobal().log(Level.FINE, "[PROFILE] prepareForNextRun(computeAffectedTests): "
                + Writer.millsToSeconds(end - createLoadables));
        Logger.getGlobal().log(Level.FINE, "[PROFILE] updateForNextRun(prepareForNextRun(TOTAL)): "
                + Writer.millsToSeconds(end - start));
        return new Result(transitiveClosure, loadables.getGraph(), affected, loadables.getUnreached());
    }

    protected List<String> getAllClasses() {
        List<String> allClasses = new ArrayList<>();
        allClasses.addAll(getTestClasses("getAllClasses()"));
        allClasses.addAll(getProjectClasses("getAllClasses()"));
       return allClasses;
    }
}
