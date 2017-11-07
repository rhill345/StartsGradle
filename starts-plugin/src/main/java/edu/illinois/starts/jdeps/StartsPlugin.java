package edu.illinois.starts.jdeps;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskContainer;


/**
 * Created by randy on 10/8/17.
 */
public class StartsPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        TaskContainer tasks = project.getTasks();

        Task testResouces = tasks.findByName("processTestResources");

        // Configure diff task.
        Task diffTask = tasks.create("sdiff", DiffTask.class);
        diffTask.setDescription("Finds types that have changed since the last time they were analyzed..");
        Task cleanTask = tasks.create("sclean", CleanTask.class);
        tasks.create("sselect", SelectTask.class);
        tasks.create("srun", RunTask.class);
        tasks.create("simpacted", ImpactedTask.class);
        tasks.create("sstarts", StartsTask.class);

        testResouces.doFirst(cleanTask.getActions().get(0));
    }
}
