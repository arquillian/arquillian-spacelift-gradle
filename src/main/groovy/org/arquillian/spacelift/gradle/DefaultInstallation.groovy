package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.tool.basic.DownloadTool
import org.arquillian.spacelift.tool.basic.UntarTool
import org.arquillian.spacelift.tool.basic.UnzipTool
import org.gradle.api.Project
import org.slf4j.Logger

class DefaultInstallation extends BaseContainerizableObject<DefaultInstallation> implements Installation {

    // version of the product installation belongs to
    Closure version = {}

    // product name
    Closure product = {}

    // these two variables allow to directly specify fs path or remote url of the installation bits
    Closure fsPath = {}

    // url to download from
    Closure remoteUrl = {}

    // zip file name
    Closure fileName = {}

    // represents directory where installation is extracted to
    Closure home = {}

    // automatically extract archive
    Closure autoExtract =  { true }

    // application of Ant mapper during extraction
    Closure extractMapper = {}

    Closure forceReinstall = { false }

    // actions to be invoked after installation is done
    Closure postActions = {}

    // precondition closure returning boolean
    // if true, installation will be installed, if false, skipped
    Closure preconditions = { true }

    // tools provided by this installation
    InheritanceAwareContainer<GradleSpaceliftTool, GradleSpaceliftTool> tools

    DefaultInstallation(String productName, Project project) {
        super(productName, project)
        this.product = { productName }
        this.tools = new InheritanceAwareContainer(project, this, GradleSpaceliftTool, GradleSpaceliftTool)
    }

    /**
     * Cloning constructor. Preserves lazy nature of closures to be evaluated later on.
     * @param other Installation to be cloned
     */
    DefaultInstallation(String installationName, DefaultInstallation other) {
        super(installationName, other)
        // use direct access to skip call of getter
        this.version = other.@version.clone()
        this.product = other.@product.clone()
        this.fsPath = other.@fsPath.clone()
        this.remoteUrl = other.@remoteUrl.clone()
        this.fileName = other.@fileName.clone()
        this.home = other.@home.clone()
        this.autoExtract = other.@autoExtract.clone()
        this.extractMapper = other.@extractMapper.clone()
        this.forceReinstall = other.@forceReinstall.clone()
        this.preconditions = other.@preconditions.clone()
        this.postActions = other.@postActions.clone()
        this.tools = other.@tools.clone()
    }

    @Override
    public DefaultInstallation clone(String name) {
        new DefaultInstallation(name, this)
    }

    @Override
    File getHome() {
        def homeDir = home.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(homeDir==null) {
            URL url = getRemoteUrl()
            if(url!=null) {
                homeDir = guessDirNameFromUrl(url)
            }
            else {
                return project.spacelift.workspace
            }
        }

        new File(project.spacelift.workspace, homeDir)
    }

    @Override
    String getVersion() {
        def versionString = version.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(versionString==null) {
            return ""
        }

        return versionString.toString();
    }

    @Override
    String getProduct() {
        def productName = product.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(productName==null) {
            return ""
        }

        return productName.toString()
    }

    def tools(Closure closure) {
        tools.configure(closure)
        this
    }

    def getFileName() {
        def fileNameString = fileName.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(fileNameString==null) {
            URL url = getRemoteUrl()
            if(url!=null) {
                return guessFileNameFromUrl(url)
            }
            return ""
        }

        return fileNameString.toString()
    }

    def getAutoExtract() {
        def shouldExtract = autoExtract.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(shouldExtract==null) {
            return true
        }
        return Boolean.parseBoolean(shouldExtract.toString())
    }

    def getForceReinstall() {
        def shouldReinstall = forceReinstall.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(shouldReinstall==null) {
            return false
        }
        return Boolean.parseBoolean(shouldReinstall.toString())
    }

    def getFsPath() {
        def filePath = fsPath.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(filePath==null) {
            filePath = "${project.spacelift.installationsDir}/${getProduct()}/${getVersion()}/${getFileName()}"
        }
        return new File(filePath.toString());
    }

    def getRemoteUrl() {
        def remoteUrlString = remoteUrl.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(remoteUrlString==null) {
            return null;
        }
        return new URL(remoteUrlString.toString())
    }

    // get installation and perform steps defined in closure after it is extracted
    void install(Logger logger) {

        if (!preconditions.rehydrate(new GradleSpaceliftDelegate(), this, this).call()) {
            // if closure returns false, we did not meet preconditions
            // so we return from installation process
            logger.info(":install:${name} Skipping, did not meet preconditions.")
            return
        }

        File targetFile = getFsPath()
        if(getForceReinstall() == false && targetFile.exists()) {
            logger.info(":install:${name} Grabbing ${getFileName()} from file system")
        }
        else if(getRemoteUrl()!=null){
            // ensure parent directory exists
            targetFile.getParentFile().mkdirs()

            // dowload bits if they do not exists
            logger.info(":install:${name} Grabbing from ${getRemoteUrl()}, storing at ${targetFile}")
            Tasks.prepare(DownloadTool).from(getRemoteUrl()).timeout(60000).to(targetFile).execute().await()
        }

        // extract file if set to and at the same time file is defined
        if(getAutoExtract() && getFileName() != "") {
            if(getForceReinstall() == false && getHome().exists()) {
                logger.info(":install:${name} Reusing existing installation at ${getHome()}")
            }
            else {

                if(getForceReinstall() == true && getHome().exists()) {
                    logger.info(":install:${name} Deleting previous installation at ${getHome()}")
                    project.ant.delete(dir: getHome())
                }

                def remap = extractMapper.rehydrate(new GradleSpaceliftDelegate(), this, this)

                logger.info(":install:${name} Extracting installation from ${getFileName()}")

                // based on installation type, we might want to unzip/untar/something else
                switch(getFileName()) {
                    case ~/.*jar/:
                        project.configure(Tasks.chain(getFsPath(),UnzipTool).toDir(new File(project.spacelift.workspace, getFileName())), remap)
                        .execute().await()
                        break
                    case ~/.*zip/:
                        project.configure(Tasks.chain(getFsPath(),UnzipTool).toDir(project.spacelift.workspace), remap).execute().await()
                        break
                    case ~/.*tgz/:
                    case ~/.*tar\.gz/:
                        project.configure(Tasks.chain(getFsPath(),UntarTool).toDir(project.spacelift.workspace), remap).execute().await()
                        break
                    case ~/.*tbz/:
                    case ~/.*tar\.bz2/:
                        project.configure(Tasks.chain(getFsPath(),UntarTool).bzip2(true).toDir(project.spacelift.workspace), remap).execute().await()
                        break
                    default:
                        logger.warn(":install:${name} Unable to extract ${getFileName()}, unknown archive type")
                }
            }
        }
        else {
            if(new File(getHome(), getFileName()).exists()) {
                logger.info(":install:${name} Reusing existing installation ${new File(getHome(),getFileName())}")
            }
            else {
                logger.info(":install:${name} Copying installation to ${project.spacelift.workspace}")
                project.ant.copy(file: getFsPath(), tofile: new File(getHome(), getFileName()))
            }
        }

        // register installed tools
        tools.each { tool ->
            tool.registerInSpacelift(GradleSpacelift.toolRegistry())
        }
        // execute post actions
        postActions.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
    }

    private String guessFileNameFromUrl(URL url) {
        String path = url.getPath()

        if (path==null || "".equals(path) || path.endsWith("/")) {
            return ""
        }

        File file = new File(url.getPath())
        return file.getName()
    }

    private String guessDirNameFromUrl(URL url) {
        String dirName = guessFileNameFromUrl(url)

        int dot = dirName.lastIndexOf(".")
        if(dot!=-1) {
            int tar = dirName.lastIndexOf(".tar")
            if(tar != -1) {
                return dirName.substring(0, tar)
            }
            else {
                return dirName.substring(0, dot)
            }
        }

        return dirName
    }
}
