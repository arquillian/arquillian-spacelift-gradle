package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.task.TaskRegistry
import org.arquillian.spacelift.task.net.DownloadTool
import org.gradle.api.Project
import org.slf4j.Logger


@CompileStatic
class MyOwnInstallationDefinition extends BaseContainerizableObject<MyOwnInstallationDefinition> implements Installation {

    MyOwnInstallationDefinition(String name, Object parent) {
        super(name, parent)
    }

    MyOwnInstallationDefinition(String name, MyOwnInstallationDefinition other) {
        super(name, other)
    }

    @Override
    public MyOwnInstallationDefinition clone(String name) {
        return new MyOwnInstallationDefinition(name, this)
    }

    @Override
    public String getProduct() {
        "test"
    }

    @Override
    public String getVersion() {
        "1"
    }

    @Override
    public File getHome() {
        return Spacelift.configuration().workspace()
    }

    @Override
    public boolean isInstalled() {
        return false;
    }

    @Override
    public void registerTools(TaskRegistry registry) {
        // do nothing
    }

    @Override
    public void install(Logger logger) {
        File location = new File(getHome(), "${product}-${version}")
        Spacelift.task(DownloadTool).from("https://github.com/arquillian/arquillian-selenium-bom/archive/master.zip").timeout(60000).to(location).execute().await()
        assert location.exists()
    }
}
