package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.tool.basic.DownloadTool
import org.gradle.api.Project
import org.slf4j.Logger

class MyOwnInstallationDefinition extends BaseContainerizableObject<MyOwnInstallationDefinition> implements Installation {

    MyOwnInstallationDefinition(String name, Project project) {
        super(name, project)
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
        project.spacelift.workspace
    }

    @Override
    public void install(Logger logger) {
        File location = new File(getHome(), "${product}-${version}")
        Tasks.prepare(DownloadTool).from("https://github.com/arquillian/arquillian-selenium-bom/archive/master.zip").timeout(60000).to(location).execute().await()
        assert location.exists()
    }
}
