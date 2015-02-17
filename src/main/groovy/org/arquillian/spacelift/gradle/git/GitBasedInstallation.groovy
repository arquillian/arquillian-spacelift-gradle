package org.arquillian.spacelift.gradle.git

import groovy.transform.CompileStatic

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.BaseContainerizableObject
import org.arquillian.spacelift.gradle.DefaultGradleTask
import org.arquillian.spacelift.gradle.DeferredValue
import org.arquillian.spacelift.gradle.GradleTask
import org.arquillian.spacelift.gradle.InheritanceAwareContainer
import org.arquillian.spacelift.gradle.Installation
import org.arquillian.spacelift.task.TaskRegistry
import org.slf4j.Logger

/**
 * Installs software from a git repository
 * @author kpiwko
 *
 */
@CompileStatic
class GitBasedInstallation extends BaseContainerizableObject<GitBasedInstallation> implements Installation {

    DeferredValue<String> product = DeferredValue.of(String.class).from("unused")

    DeferredValue<String> version = DeferredValue.of(String.class).from("unused")

    DeferredValue<String> repository = DeferredValue.of(String.class)

    DeferredValue<String> commit = DeferredValue.of(String.class).from("master")

    // represents directory where installation is extracted to
    DeferredValue<File> home = DeferredValue.of(File.class)

    DeferredValue<Boolean> isInstalled = DeferredValue.of(Boolean.class).from({
        return getHome().exists()
    })

    // actions to be invoked after installation is done
    DeferredValue<Void> postActions = DeferredValue.of(Void.class)

    // tools provided by this installation
    InheritanceAwareContainer<GradleTask, DefaultGradleTask> tools

    GitBasedInstallation(String name, Object parent) {
        super(name, parent)

        this.tools = new InheritanceAwareContainer(this, GradleTask, DefaultGradleTask)
    }

    GitBasedInstallation(String name, GitBasedInstallation other) {
        super(name, other)

        this.product = other.@product.copy()
        this.version = other.@product.copy()
        this.repository = other.@repository.copy()
        this.commit = other.@commit.copy()
        this.isInstalled = other.@isInstalled.copy()
        this.postActions = other.@postActions.copy()
        this.tools = (InheritanceAwareContainer<GradleTask, DefaultGradleTask>) other.@tools.clone()

    }

    @Override
    GitBasedInstallation clone(String name) {
        return new GitBasedInstallation(name, this)
    }

    @Override
    String getVersion() {
        return version.resolve()
    }

    @Override
    String getProduct() {
        return product.resolve()
    }

    @Override
    File getHome() {
        return home.resolve()
    }

    @Override
    public boolean isInstalled() {
        return isInstalled.resolve()
    }

    @Override
    void install(Logger logger) {
        File location = Spacelift.task(repository.resolve(), GitCloneTool.class).destination(getHome()).execute().await()
        Spacelift.task(location, GitCheckoutTool.class).checkout(commit.resolve()).execute().await()
    }

    @Override
    void registerTools(TaskRegistry registry) {
        ((Iterable<GradleTask>) tools).each { GradleTask task ->
            Spacelift.registry().register(task.factory())
        }
    }
}
