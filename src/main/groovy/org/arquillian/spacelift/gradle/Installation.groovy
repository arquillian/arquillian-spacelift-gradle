package org.arquillian.spacelift.gradle

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.tool.basic.DownloadTool
import org.arquillian.spacelift.tool.basic.UntarTool
import org.arquillian.spacelift.tool.basic.UnzipTool
import org.gradle.api.Project
import org.slf4j.Logger


// this class represents anything installable
// values for installation can either be a single value or if anything requires multiple values per product
// you can use OS families:
// * windows
// * mac
// * linux
// * solaris
class Installation implements ValueExtractor {

    // this is required in order to use project container abstraction
    final String name

    // version of the product installation belongs to
    Closure version = {}

    // product name
    Closure product = {}

    // these two variables allow to directly specify fs path or remote url of the installation bits
    Closure fsPath = {}
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
    InheritanceAwareContainer<GradleSpaceliftTool> tools

    // access to project defined variables
    Project project

    Installation(String productName, Project project) {
        this.name = productName
        this.product = { productName }
        this.project = project
        this.tools = new InheritanceAwareContainer(project, this, GradleSpaceliftTool)
    }

    def autoExtract(arg) {
        this.autoExtract = extractValueAsLazyClosure(arg).dehydrate()
        this.autoExtract.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getAutoExtract() {
        def shouldExtract = autoExtract.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(shouldExtract==null) {
            return true
        }
        return Boolean.parseBoolean(shouldExtract.toString())
    }

    def forceReinstall(arg) {
        this.forceReinstall = extractValueAsLazyClosure(arg).dehydrate()
        this.forceReinstall.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getForceReinstall() {
        def shouldReinstall = forceReinstall.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(shouldReinstall==null) {
            return false
        }
        return Boolean.parseBoolean(shouldReinstall.toString())
    }

    def fsPath(arg) {
        this.fsPath = extractValueAsLazyClosure(arg).dehydrate()
        this.fsPath.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getFsPath() {
        def filePath = fsPath.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(filePath==null) {
            filePath = "${project.spacelift.installationsDir}/${getProduct()}/${getVersion()}/${getFileName()}"
        }
        return new File(filePath.toString());
    }

    def remoteUrl(arg) {
        this.remoteUrl = extractValueAsLazyClosure(arg).dehydrate()
        this.remoteUrl.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getRemoteUrl() {
        def remoteUrlString = remoteUrl.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(remoteUrlString==null) {
            return null;
        }
        return new URL(remoteUrlString.toString())
    }

    def home(arg) {
        this.home = extractValueAsLazyClosure(arg).dehydrate()
        this.home.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getHome() {
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

    def fileName(arg) {
        this.fileName = extractValueAsLazyClosure(arg).dehydrate()
        this.fileName.resolveStrategy = Closure.DELEGATE_FIRST
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

    def product(arg) {
        this.product = extractValueAsLazyClosure(arg).dehydrate()
        this.product.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getProduct() {
        def productName = product.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(productName==null) {
            return ""
        }

        return productName.toString()
    }

    def version(arg) {
        this.version = extractValueAsLazyClosure(arg).dehydrate()
        this.version.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def getVersion() {
        def versionString = version.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
        if(versionString==null) {
            return ""
        }

        return versionString.toString();
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
                        project.configure(Tasks.chain(getFsPath(),UntarTool).bzip2(true).toDir(project.spacelift, remap).workspace).execute().await()
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

    // we keep extraction mapper to be a part of ant extract command
    def extractMapper(Closure closure) {
        this.extractMapper = extractValueAsLazyClosure(closure).dehydrate()
        this.extractMapper.resolveStrategy = Closure.DELEGATE_FIRST
    }

    // we keep post actions to be executed after installation is done
    def postActions(Closure closure) {
        this.postActions = extractValueAsLazyClosure(closure).dehydrate()
        this.postActions.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def preconditions(Closure closure) {
        this.preconditions = extractValueAsLazyClosure(closure).dehydrate()
        this.preconditions.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def tools(Closure closure) {
        tools.configure(closure)
        this
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
            return dirName.substring(0, dot)
        }

        return dirName
    }

}
