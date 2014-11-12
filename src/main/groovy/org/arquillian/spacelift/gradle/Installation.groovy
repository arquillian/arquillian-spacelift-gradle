package org.arquillian.spacelift.gradle

import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory


// this class represents anything installable
// values for installation can either be a single value or if anything requires multiple values per product
// you can use OS families:
// * windows
// * mac
// * linux
// * solaris
@Mixin(ValueExtractor)
class Installation {
    static final Logger log = LoggerFactory.getLogger('Installation')

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
    def tools = []

    // access to project defined variables
    Project project

    Installation(String productName, Project project) {
        this.name = productName
        this.product = { productName }
        this.project = project
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
            return project.spacelift.workspace
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
    void install() {

        if (!preconditions.rehydrate(new GradleSpaceliftDelegate(), this, this).call()) {
            // if closure returns false, we did not meet preconditions
            // so we return from installation process
            log.info("Installation '" + name + "' did not meet preconditions - it will be excluded from execution.")
            return
        }

        def ant = project.ant

        File targetFile = getFsPath()
        if(getForceReinstall() == false && targetFile.exists()) {
            log.info("Grabbing ${getFileName()} from file system")
        }
        else {
            // ensure parent directory exists
            targetFile.getParentFile().mkdirs()

            // dowload bits if they do not exists
            log.info("Downloading ${getFileName()} from URL ${getRemoteUrl()} to ${getFsPath()}")
            ant.get(src: getRemoteUrl(), dest: getFsPath(), usetimestamp: true)
        }

        if(getAutoExtract()) {
            if(forceReinstall == false && getHome().exists()) {
                log.info("Reusing existing installation ${getHome()}")
            }
            else {

                if(forceReinstall && getHome().exists()) {
                    log.info("Deleting previous installation ${getHome()}")
                    ant.delete(dir: getHome())
                }

                extractMapper = extractMapper.rehydrate(new GradleSpaceliftDelegate(), this.project, this)

                log.info("Extracting installation to ${project.spacelift.workspace}")

                // based on installation type, we might want to unzip/untar/something else
                switch(getFileName()) {
                    case ~/.*jar/:
                        ant.unzip (src: getFsPath(), dest: new File(project.spacelift.workspace, getFileName()), extractMapper)
                        break
                    case ~/.*zip/:
                        ant.unzip (src: getFsPath(), dest: project.spacelift.workspace, extractMapper)
                        break
                    case ~/.*tgz/:
                    case ~/.*tar\.gz/:
                        ant.untar(src: getFsPath(), dest: project.spacelift.workspace, compression: 'gzip', extractMapper)
                        break
                    case ~/.*tbz/:
                    case ~/.*tar\.bz2/:
                        ant.untar(src: getFsPath(), dest: project.spacelift.workspace, compression: 'bzip2', extractMapper)
                        break
                    default:
                        throw new RuntimeException("Invalid file type for installation ${getFileName()}")
                }
            }
        }
        else {
            if(new File(getHome(), getFileName()).exists()) {
                log.info("Reusing existing installation ${new File(getHome(),getFileName())}")
            }
            else {
                log.info("Copying installation to ${project.spacelift.workspace}")
                ant.copy(file: getFsPath(), tofile: new File(getHome(), getFileName()))
            }
        }

        // register installed tools
        tools.each { tool ->
            // use project.configure method to user closure to configure the tool
            def gradleTool = project.configure(new GradleSpaceliftTool(project), tool.rehydrate(new GradleSpaceliftDelegate(), this, this))
            gradleTool.registerInSpacelift(GradleSpacelift.toolRegistry())
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

    def tool(Closure closure) {
        def tool = extractValueAsLazyClosure(closure).dehydrate()
        tool.resolveStrategy = Closure.DELEGATE_FIRST
        tools << tool
    }
}
