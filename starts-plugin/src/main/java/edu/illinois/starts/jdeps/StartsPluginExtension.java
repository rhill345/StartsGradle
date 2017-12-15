package edu.illinois.starts.jdeps;

import edu.illinois.starts.enums.DependencyFormat;

import java.io.File;

/**
 * Created by randy on 10/8/17.
 */
public class StartsPluginExtension {

    /**
     * Set this to "false" to not filter out "sun.*" and "java.*" classes from jdeps parsing.
     */
    private boolean filterLib = false;

    public DependencyFormat getDepFormat() {
        return depFormat;
    }

    /**
     * Allows to switch the format in which we want to store the test dependencies.
     * A full list of what we currently support can be found in
     * @see edu.illinois.starts.enums.DependencyFormat */
    protected DependencyFormat depFormat = DependencyFormat.ZLC;

    /**
     * Path to directory that contains the result of running jdeps on third-party
     * and standard library jars that an application may need, e.g., those in M2_REPO.
     */
    protected String graphCache = "jdeps-cache";

    /**
     * Set this to "false" to not print the graph obtained from jdeps parsing.
     * When "true" the graph is written to file after the run.
     */
   protected boolean printGraph = true;

    /**
     * Output filename for the graph, if printGraph == true.
     */
    protected String graphFile = "graph";

    /**
     * Log levels as defined in java.util.logging.Level.
     */
    protected String loggingLevel = "CONFIG";

    /**
     * Set this to "false" to disable smart hashing, i.e., to *not* strip
     * Bytecode files of debug info prior to computing checksums. See the "Smart
     * Checksums" Sections in the Ekstazi paper:
     * http://dl.acm.org/citation.cfm?id=2771784
     */
    protected boolean cleanBytes = true;

    /**
     * Set this to "true" to update test dependencies on disk. The default value of "false"
     * is useful for "dry runs" where one may want to see the diff without updating
     * the test dependencies.
     */
    private boolean updateDiffChecksums;

    /**
     * Set this to "false" to prevent checksums from being persisted to disk. This
     * is useful for "dry runs" where one may want to see the non-affected tests that
     * STARTS writes to the Surefire excludesFile, without updating test dependencies.
     */
    protected boolean updateRunChecksums = true;

    /**
     * Set this option to "true" to run all tests, not just the affected ones. This option is useful
     * in cases where one is interested to measure the time to run all tests, while at the
     * same time measuring the times for analyzing what tests to select and reporting the number of
     * tests it would select.
     * Note: Run with "-DstartsLogging=FINER" or "-DstartsLogging=FINEST" so that the "selected-tests"
     * file, which contains the list of tests that would be run if this option is set to false, will
     * be written to disk.
     */
    protected boolean retestAll;

    /**
     * Set this to "true" to update test dependencies on disk. The default value of "false"
     * is useful for "dry runs" where one may want to see the diff without updating
     * the test dependencies.
     */
    private boolean updateImpactedChecksums;

    /**
     * Set to "true" to print newly-added classes: classes in the program that were not in the previous version.
     */
    private boolean trackNewClasses = false;

    public StartsPluginExtension() {}

    public boolean getFilterLib() {
        return filterLib;
    }

    public String getGraphCache(String baseDir) {
        return baseDir + File.separator + graphCache;
    }

    public boolean getPrintGraph() {
        return printGraph;
    }

    public String getGraphFile() {
        return graphFile;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public boolean getUpdateRunChecksums() {
        return updateRunChecksums;
    }

    public boolean getRetestAll() {
        return retestAll;
    }

    public boolean getCleanBytes() {
        return cleanBytes;
    }

    public boolean getUpdateDiffChecksums() {
        return updateDiffChecksums;
    }

    public boolean getUpdateImpactedChecksums() {
        return updateImpactedChecksums;
    }

    public boolean getTrackNewClasses() {
        return trackNewClasses;
    }

    public void setFilterLib(boolean filterLib) {
        this.filterLib = filterLib;
    }

    public void setDepFormat(DependencyFormat depFormat) {
        this.depFormat = depFormat;
    }

    public void setGraphCache(String graphCache) {
        this.graphCache = graphCache;
    }

    public void setPrintGraph(boolean printGraph) {
        this.printGraph = printGraph;
    }

    public void setGraphFile(String graphFile) {
        this.graphFile = graphFile;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public void setCleanBytes(boolean cleanBytes) {
        this.cleanBytes = cleanBytes;
    }

    public void setUpdateDiffChecksums(boolean updateDiffChecksums) {
        this.updateDiffChecksums = updateDiffChecksums;
    }

    public void setUpdateRunChecksums(boolean updateRunChecksums) {
        this.updateRunChecksums = updateRunChecksums;
    }

    public void setRetestAll(boolean retestAll) {
        this.retestAll = retestAll;
    }

    public void setUpdateImpactedChecksums(boolean updateImpactedChecksums) {
        this.updateImpactedChecksums = updateImpactedChecksums;
    }

    public void setTrackNewClasses(boolean trackNewClasses) {
        this.trackNewClasses = trackNewClasses;
    }
}
