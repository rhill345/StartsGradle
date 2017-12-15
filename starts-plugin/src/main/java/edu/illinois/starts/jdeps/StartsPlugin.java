package edu.illinois.starts.jdeps;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.testing.Test;


/**
 * Created by randy on 10/8/17.
 */
public class StartsPlugin implements Plugin<Project> {

    // Task name definitions.
    private static final String TASK_STARTS = "starts";
    private static final String TASK_STARTS_DIFF = "startsDiff";
    private static final String TASK_STARTS_CLEAN = "startsClean";
    private static final String TASK_STARTS_SELECT = "startsSelect";
    private static final String TASK_STARTS_IMPACTED = "startsImpacted";
    private static final String TASK_STARTS_RUN = "startsRun";

    // Spec to force a task to always run.
    private final Spec upToDateSpec = new Spec<Task>() {
        @Override
        public boolean isSatisfiedBy(Task task) {
            return false;
        }
    };

    @Override
    public void apply(Project project) {

        project.getExtensions().create("startsSettings", StartsPluginExtension.class);
        addTasksToProject(project);
        for (Project currentProject : project.getAllprojects()) {
            if (!currentProject.getName().equals(project.getName())) {
                addTasksToProject(currentProject);
            }
        }
    }

    private void addTasksToProject(Project project) {
        TaskContainer tasks = project.getTasks();

        Task diffTask = tasks.create(TASK_STARTS_DIFF, DiffTask.class);
        diffTask.setDescription("To see the types that changed since the last time STARTS was run");
        diffTask.getOutputs().upToDateWhen(upToDateSpec);

        Task cleanTask = tasks.create(TASK_STARTS_CLEAN, CleanTask.class);
        cleanTask.setDescription("To remove all artifacts that STARTS stores between versions (i.e. in the .starts directories)");
        cleanTask.getOutputs().upToDateWhen(upToDateSpec);

        Task selectTask = tasks.create(TASK_STARTS_SELECT, SelectTask.class);
        selectTask.setDescription("To see the tests that are affected by the most recent changes");
        selectTask.getOutputs().upToDateWhen(upToDateSpec);

        Task runTask = tasks.create(TASK_STARTS_RUN, RunTask.class);
        runTask.setDescription("Prepares for test runs by writing non-affected tests in the excludesFile");
        runTask.getOutputs().upToDateWhen(upToDateSpec);

        Task impactedTask = tasks.create(TASK_STARTS_IMPACTED, ImpactedTask.class);
        impactedTask.setDescription("To see the types that may be impacted by changes since the last time STARTS was run");
        impactedTask.getOutputs().upToDateWhen(upToDateSpec);

        Task startsTask = tasks.create(TASK_STARTS, StartsTask.class);
        startsTask.setDescription("To perform RTS using STARTS (i.e., select tests and run the selected tests)");
        startsTask.getOutputs().upToDateWhen(upToDateSpec);

        // make the starts task depend on all compile tasks.
        setTaskDependency(project, startsTask, "compile");

        // Add starts dependency to the Test Tasks.
        for(Task task : project.getTasks().withType(Test.class)) {
            task.dependsOn(startsTask);
        }
    }

    private void setTaskDependency(Project project, Task task, String taskName) {
        TaskContainer tasks  = project.getTasks();
        for (Task dependTask : tasks) {
            if (dependTask.getName().contains(taskName)) {
                task.dependsOn(dependTask);
            }
        }
    }
}
