package org.arquillian.spacelift.gradle.git

import groovy.transform.CompileStatic

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.BaseContainerizableObject
import org.arquillian.spacelift.gradle.DSLUtil
import org.arquillian.spacelift.gradle.DefaultGradleTask
import org.arquillian.spacelift.gradle.GradleTask
import org.arquillian.spacelift.gradle.InheritanceAwareContainer
import org.arquillian.spacelift.gradle.Installation
import org.arquillian.spacelift.task.TaskRegistry
import org.gradle.api.Project
import org.slf4j.Logger

/**
 * Installs software from a git repository
 * @author kpiwko
 *
 */
@CompileStatic
class GitBasedInstallation extends BaseContainerizableObject<GitBasedInstallation> implements Installation {

    Closure product = { "unused" }

    Closure version = { "unused" }

    Closure repository = { }

    Closure commit = { "master" }

    Closure home = {}

    Closure isInstalled = {
        return getHome().exists()
    }

    Closure postActions = {}

    // tools provided by this installation
    InheritanceAwareContainer<GradleTask, DefaultGradleTask> tools

    GitBasedInstallation(String name, Object parent) {
        super(name, parent)

        this.tools = new InheritanceAwareContainer(this, GradleTask, DefaultGradleTask)
    }

    GitBasedInstallation(String name, GitBasedInstallation other) {
        super(name, other)

        this.product = (Closure) other.@product.clone()
        this.version = (Closure) other.@product.clone()
        this.repository = (Closure) other.@repository.clone()
        this.commit = (Closure) other.@commit.clone()
        this.isInstalled = (Closure) other.@isInstalled.clone()
        this.postActions = (Closure) other.@postActions.clone()
        this.tools = (InheritanceAwareContainer<GradleTask, DefaultGradleTask>) other.@tools.clone()

    }

    @Override
    GitBasedInstallation clone(String name) {
        return new GitBasedInstallation(name, this)
    }

    @Override
    String getVersion() {
        def versionString = DSLUtil.resolve(String.class, version, this)
        if(versionString==null) {
            return ""
        }

        return versionString.toString();
    }

    @Override
    String getProduct() {
        def productName = DSLUtil.resolve(String.class, product, this)
        if(productName==null) {
            return ""
        }

        return productName.toString()
    }

    @Override
    File getHome() {
        String homeDir = DSLUtil.resolve(String.class, DSLUtil.deferredValue(home), this)
        return new File((File) parent['workspace'], homeDir)
    }

    @Override
    public boolean isInstalled() {
        return DSLUtil.resolve(Boolean.class, isInstalled, this)
    }

    @Override
    void install(Logger logger) {

        String repository = DSLUtil.resolve(String.class, repository, this)

        File location = Spacelift.task(repository, GitCloneTool.class).destination(getHome()).execute().await()

        String commit = DSLUtil.resolve(String.class, commit, this)

        Spacelift.task(location, GitCheckoutTool.class).checkout(commit).execute().await()
    }

    @Override
    void registerTools(TaskRegistry registry) {
        ((Iterable<GradleTask>) tools).each { GradleTask task ->
            Spacelift.registry().register(task.factory())
        }
    }
}
