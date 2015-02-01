package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.TaskFactory


interface GradleTask extends ContainerizableObject<GradleTask> {

    public <IN, OUT, TASK extends Task<? super IN,OUT>> TaskFactory<IN, OUT, TASK> factory();
}
