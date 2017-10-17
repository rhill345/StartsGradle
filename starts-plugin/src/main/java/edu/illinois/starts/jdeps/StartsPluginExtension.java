package edu.illinois.starts.jdeps;

import edu.illinois.starts.enums.DependencyFormat;
import org.gradle.api.Project;

import java.util.Map;

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
    protected DependencyFormat depFormat;


    /**
     * Path to directory that contains the result of running jdeps on third-party
     * and standard library jars that an application may need, e.g., those in M2_REPO.
     */
    protected String graphCache = "${basedir}${file.separator}jdeps-cache";

    /**
     * Set this to "false" to not print the graph obtained from jdeps parsing.
     * When "true" the graph is written to file after the run.
     */
   protected boolean printGraph = false;

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
    protected boolean cleanBytes;

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
    protected boolean updateRunChecksums;

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


    public StartsPluginExtension(Project project) {

        Map<String, ?> properties = project.getProperties();
        if(properties.containsKey("filterLib")){
            filterLib = (Boolean) properties.get("filterLib");
        }

        if(properties.containsKey("depFormat")){
            depFormat = (DependencyFormat) properties.get("depFormat");
        }

        if(properties.containsKey("graphCache")){
            graphCache = (String) properties.get("graphCache");
        }

        if(properties.containsKey("graph")){
            graphFile = (String) properties.get("graph");
        }

        if(properties.containsKey("startsLogging")){
            loggingLevel = (String) properties.get("startsLogging");
        }

        if(properties.containsKey("cleanBytes")){
            cleanBytes = (Boolean) properties.get("cleanBytes");
        }

        if(properties.containsKey("updateDiffChecksums")){
            updateDiffChecksums = (Boolean) properties.get("updateDiffChecksums");
        }

        if(properties.containsKey("updateRunChecksums")){
            updateRunChecksums = (Boolean) properties.get("updateRunChecksums");
        }

        if(properties.containsKey("retestAll")){
            retestAll = (Boolean) properties.get("retestAll");
        }

    }

    public boolean getFilterLib() {
        return filterLib;
    }

    public void setFilterLib(boolean filterLib) {
        this.filterLib = filterLib;
    }

    public String getGraphCache() {
        return graphCache;
    }

    public void setGraphCache(String graphCache) {
        this.graphCache = graphCache;
    }

    public boolean getPrintGraph() {
        return printGraph;
    }

    public void setPrintGraph(boolean printGraph) {
        this.printGraph = printGraph;
    }

    public String getGraphFile() {
        return graphFile;
    }

    public void setGraphFile(String graphFile) {
        this.graphFile = graphFile;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public boolean isUpdateRunChecksums() {
        return updateRunChecksums;
    }

    public boolean isRetestAll() {
        return retestAll;
    }

    public boolean getCleanBytes() {
        return cleanBytes;
    }

    public boolean getUpdateDiffChecksums() {
        return updateDiffChecksums;
    }
}
