/*
 * Copyright (c) 2015 - Present. The STARTS Team. All Rights Reserved.
 */

package edu.illinois.starts.jdeps;

import edu.illinois.starts.enums.DependencyFormat;
import edu.illinois.starts.helpers.*;
import edu.illinois.starts.util.Logger;
import edu.illinois.yasgl.DirectedGraph;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.testing.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import java.util.logging.Level;

import static edu.illinois.starts.constants.StartsConstants.STARTS_DIRECTORY_PATH;

/**
 * Base TASK for the JDeps-Based STARTS.
 */
abstract class BaseTask extends DefaultTask {

    private final static String javaPath = "java" + File.separator;
    /**
     * The directory in which to store STARTS artifacts that are needed between runs.
     */
    protected String artifactsDir;

    private Set<File> mClassPath;

    private StartsPluginExtension mExtension;

    private Set<String> mAllTestClasses;

    protected  StartsPluginExtension getExtension() {
        if (mExtension == null) {
            // Check to see if a configuration was set in gradle file.
            mExtension = getProject().getExtensions().findByType(StartsPluginExtension.class);
            if (mExtension == null) {
                // If still null, create a new extension object with defaults.
                mExtension = new StartsPluginExtension(getProject());
            }
        }
        return mExtension;
    }

    @TaskAction
    public void executeTask() throws Exception {
        // Load test classes prior to excludes.
        getTestClasses("executeTask");

        // perform task action
        performTask();
    }

    /*
    *  Entry method for perform a gradle task
    * */
    protected abstract void performTask() throws Exception;

    protected void printResult(Set<String> set, String title) {
        Writer.writeToLog(set, title, Logger.getGlobal());
    }

    public String getArtifactsDir() throws GradleException {
        if (artifactsDir == null) {
            artifactsDir = getWorkingDir().getAbsolutePath() + File.separator + STARTS_DIRECTORY_PATH;
            File file = new File(artifactsDir);
            if (!file.mkdirs() && !file.exists()) {
                throw new GradleException("I could not create artifacts dir: " + artifactsDir);
            }
        }
       return artifactsDir;
    }

    public void setIncludesExcludes() {
        // Includes and excludes are already pulled from the gradle settings file.  Do nothing here.
    }

    public List getTestClasses(String methodName) {
        if (mAllTestClasses == null) {
            mAllTestClasses = new HashSet<>();
            for (Task task : getProject().getTasks().withType(Test.class)) {
                mAllTestClasses.addAll(getClasses(new ArrayList<>(((Test)task).getCandidateClassFiles().getFiles()), methodName));
            }
        }
        return new ArrayList(mAllTestClasses);
    }

    public List getClasses(List<File> files, String methodName) {
        long start = System.currentTimeMillis();
        List<String> classes = new ArrayList<>();
        for (File classFile : files) {
            // TODO: Find a better way to find the class and package names without the extension.
            // Ideally, this should be determine using a class loader rather than infiering the package
            // by the file name and folder structure
            if (classFile.getName().contains(".class")) {
                if (isAndroidTest(classFile.getPath())) {
                    String packageClass = convertFileToClassName(classFile, getAndroidBuildType());
                    classes.add(packageClass);
                } else if (classFile.getPath().contains(javaPath)) {
                    String packageClass = convertFileToClassName(classFile, javaPath);
                    classes.add(packageClass);
                }
            }
        }

        long end = System.currentTimeMillis();
        Logger.getGlobal().log(Level.FINE, "[PROFILE] " + methodName + "(" + methodName + "): "
                + Writer.millsToSeconds(end - start));
        return classes;
    }


    private String convertFileToClassName(File file, String pathSeparatePoint) {
        String packageClass = file.getPath().split(pathSeparatePoint)[1];
        packageClass = packageClass.substring(packageClass.indexOf("/") + 1);
        packageClass = packageClass.replace(".class", "").replace(File.separator, ".");
        return packageClass;
    }

    private boolean isAndroidTest(String path) {
        if (path.contains("intermediates/classes/test") && path.contains(getAndroidBuildType())) {
            return true;
        }
        return false;
    }

    private String getAndroidBuildType() {
        // TODO: determine the android project build "flavor" by inspecting the task dependency chain.  As of now, the only flavor suported
        // is debug.
        return "debug";
    }

    public List getProjectClasses(String methodName) {
        List<File> files = new ArrayList<>(getClassPath());
        return getClasses(files, methodName) ;
    }

    public ClassLoader createClassLoader(Set<File> classPath) {
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
        getLogger().log(LogLevel.DEBUG, "[PROFILE] updateForNextRun(createClassLoader): "
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

    public Set<File> getClassPath() {
        long start = System.currentTimeMillis();
        if (mClassPath == null) {
            try {
                mClassPath = new HashSet<>();
                // Find all test tasks and add the class paths.
                for (Task task : getProject().getTasks().withType(Test.class)) {
                    mClassPath.addAll(((Test)task).getClasspath().getFiles());
                }
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

    public Result prepareForNextRun(String sfPathString, Set<File> classPath, List<String> classesToAnalyze,
                                    Set<String> nonAffected, boolean computeUnreached) {
        long start = System.currentTimeMillis();
        String m2Repo = getProject().getRepositories().mavenLocal().getUrl().getPath();
        File jdepsCache = new File(getExtension().getGraphCache(getBaseDir()));
        // We store the jdk-graphs at the root of "jdepsCache" directory, with
        // jdk.graph being the file that merges all the graphs for all standard
        // library jars.
        File libraryFile = new File(jdepsCache, "jdk.graph");
        // Create the Loadables object early so we can use its helpers
        Loadables loadables = new Loadables(classesToAnalyze, artifactsDir, sfPathString, getExtension().getFilterLib(), jdepsCache);
        // Surefire Classpath object is easier to iterate over without de-constructing
        // sfPathString (which we use in a number of other places)

        loadables.setClasspath(new ArrayList(getFileCollectionStrings(classPath)));

        List<String> moreEdges = new ArrayList<String>();
        long loadMoreEdges = System.currentTimeMillis();
        Cache cache = new Cache(jdepsCache, m2Repo);
        // 1. Load non-reflection edges from third-party libraries in the classpath
        cache.loadM2EdgesFromCache(moreEdges, sfPathString);
        long loadM2EdgesFromCache = System.currentTimeMillis();
        // 2. Get non-reflection edges from CUT and SDK; use (1) to build graph
        loadables.create(new ArrayList<>(moreEdges), new ArrayList(getFileCollectionStrings(classPath)), computeUnreached);
        Map<String, Set<String>> transitiveClosure = loadables.getTransitiveClosure();
        long createLoadables = System.currentTimeMillis();

        // We don't need to compute affected tests this way with ZLC format.
        // In RTSUtil.computeAffectedTests(), we find affected tests by (a) removing nonAffected tests from the set of
        // all tests and then (b) adding all tests that reach to * as affected if there has been a change. This is only
        // for CLZ which does not encode information about *. ZLC already encodes and reasons about * when it finds
        // nonAffected tests.
        Set<String> affected = getExtension().getDepFormat() == DependencyFormat.ZLC ? null
                : RTSUtil.computeAffectedTests(new HashSet<>(classesToAnalyze),
                nonAffected, transitiveClosure);
        long end = System.currentTimeMillis();
        getLogger().log(LogLevel.INFO, "[PROFILE] prepareForNextRun(loadMoreEdges): "
                + Writer.millsToSeconds(loadMoreEdges - start));
        getLogger().log(LogLevel.INFO, "[PROFILE] prepareForNextRun(loadM2EdgesFromCache): "
                + Writer.millsToSeconds(loadM2EdgesFromCache - loadMoreEdges));
        getLogger().log(LogLevel.INFO, "[PROFILE] prepareForNextRun(createLoadable): "
                + Writer.millsToSeconds(createLoadables - loadM2EdgesFromCache));
        getLogger().log(LogLevel.INFO, "[PROFILE] prepareForNextRun(computeAffectedTests): "
                + Writer.millsToSeconds(end - createLoadables));
        getLogger().log(LogLevel.INFO, "[PROFILE] updateForNextRun(prepareForNextRun(TOTAL)): "
                + Writer.millsToSeconds(end - start));

        return new Result(transitiveClosure, loadables.getGraph(), affected, loadables.getUnreached());
    }

    List<String> getFileCollectionStrings(Set<File> collection) {
        List<String> paths = new ArrayList<>();
        for (File f : collection) {
            paths.add(f.getAbsolutePath());
        }
        return paths;
    }

    protected List<String> getAllClasses() {
        List<String> allClasses = new ArrayList<>();
        allClasses.addAll(getTestClasses("getTestClasses()"));
        allClasses.addAll(getProjectClasses("getAllClasses()"));
       return allClasses;
    }

    protected String getBaseDir() {
        return getWorkingDir().getAbsolutePath();
    }

    public File getWorkingDir() {
        // Grab the test task and update the excludes.
        return getProject().getProjectDir();
    }

    protected String fileListToPathString(List<File> files) {
        if (files.size() == 0) {
            return "";
        }
        StringBuilder pathString = new StringBuilder(files.get(0).getAbsolutePath());
        for (int i = 1; i < files.size(); i++) {
            pathString.append(File.pathSeparator).append(files.get(i).getAbsolutePath());
        }
        return pathString.toString();
    }
}
