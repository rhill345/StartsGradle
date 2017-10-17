package edu.illinois.starts.jdeps;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Created by randy on 10/8/17.
 */
public class StartsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getTasks().create("sdiff", DiffTask.class);//.
                //dependsOn(project.getTasks().findByName(""));
        project.getTasks().create("sclean", CleanTask.class);
    }
}
