package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

import org.arquillian.spacelift.execution.Tasks
import org.arquillian.spacelift.tool.ToolRegistry
import org.arquillian.spacelift.tool.basic.DownloadTool
import org.arquillian.spacelift.tool.basic.UntarTool
import org.arquillian.spacelift.tool.basic.UnzipTool
import org.gradle.api.Project
import org.slf4j.Logger

@CompileStatic
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

    Closure isInstalled = {
        return getHome().exists()
    }

    // actions to be invoked after installation is done
    Closure postActions = {}

    // precondition closure returning boolean
    // if true, installation will be installed, if false, skipped
    Closure preconditions = { true }

    // tools provided by this installation
    InheritanceAwareContainer<GradleSpaceliftTaskFactory, DefaultGradleSpaceliftTaskFactory> tools

    DefaultInstallation(String productName, Project project) {
        super(productName, project)
        this.product = { productName }
        this.tools = new InheritanceAwareContainer(project, this, GradleSpaceliftTaskFactory, DefaultGradleSpaceliftTaskFactory)
    }

    /**
     * Cloning constructor. Preserves lazy nature of closures to be evaluated later on.
     * @param other Installation to be cloned
     */
    DefaultInstallation(String installationName, DefaultInstallation other) {
        super(installationName, other)
        // use direct access to skip call of getter
        this.version = (Closure) other.@version.clone()
        this.product = (Closure) other.@product.clone()
        this.fsPath = (Closure) other.@fsPath.clone()
        this.remoteUrl = (Closure) other.@remoteUrl.clone()
        this.fileName = (Closure) other.@fileName.clone()
        this.home = (Closure) other.@home.clone()
        this.isInstalled = (Closure) other.@isInstalled.clone()
        this.autoExtract = (Closure) other.@autoExtract.clone()
        this.extractMapper = (Closure) other.@extractMapper.clone()
        this.preconditions = (Closure) other.@preconditions.clone()
        this.postActions = (Closure) other.@postActions.clone()
        this.tools =  (InheritanceAwareContainer<GradleSpaceliftTaskFactory, DefaultGradleSpaceliftTaskFactory>) other.@tools.clone()
    }

    @Override
    public DefaultInstallation clone(String name) {
        new DefaultInstallation(name, this)
    }

    @Override
    File getHome() {
        String homeDir = DSLUtil.resolve(String.class, home, this)
        if(homeDir==null) {
            URL url = getRemoteUrl()
            if(url!=null) {
                homeDir = guessDirNameFromUrl(url)
            }
            else {
                return (File) project['spacelift']['workspace'] //project.spacelift.workspace
            }
        }

        new File((File) project['spacelift']['workspace'], homeDir)
    }

    @Override
    String getVersion() {
        String versionString = DSLUtil.resolve(String.class, version, this)
        if(versionString==null) {
            return ""
        }

        return versionString
    }

    @Override
    String getProduct() {
        String productName = DSLUtil.resolve(String.class, product, this)
        if(productName==null) {
            return ""
        }

        return productName
    }

    @Override
    public boolean isInstalled() {
        return DSLUtil.resolve(Boolean.class, isInstalled, this)
    }

    @Override
    public void registerTools(ToolRegistry registry) {
        // register installed tools
        ((Iterable<GradleSpaceliftTaskFactory>)tools).each { GradleSpaceliftTaskFactory factory ->
            factory.register(registry)
        }
    }

    // get installation and perform steps defined in closure after it is extracted
    @Override
    void install(Logger logger) {

        if (!DSLUtil.resolve(Boolean.class, preconditions, this)) {
            // if closure returns false, we did not meet preconditions
            // so we return from installation process
            logger.info(":install:${name} Skipping, did not meet preconditions.")
            return
        }

        File targetFile = getFsPath()
        if(targetFile.exists()) {
            logger.info(":install:${name} Grabbing ${getFileName()} from file system cache")
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
            if(getHome().exists()) {
                logger.info(":install:${name} Deleting previous installation at ${getHome()}")
                project.getAnt().invokeMethod("delete", [dir: getHome()])
            }

            def remap = extractMapper.rehydrate(new GradleSpaceliftDelegate(), this, this)

            logger.info(":install:${name} Extracting installation from ${getFileName()}")

            // based on installation type, we might want to unzip/untar/something else
            switch(getFileName()) {
                case ~/.*jar/:
                    ((UnzipTool)project.configure(Tasks.chain(getFsPath(),UnzipTool).toDir(new File((File) project['spacelift']['workspace'], getFileName())), remap))
                    .execute().await()
                    break
                case ~/.*zip/:
                    ((UnzipTool)project.configure(Tasks.chain(getFsPath(),UnzipTool).toDir((File) project['spacelift']['workspace']), remap)).execute().await()
                    break
                case ~/.*tgz/:
                case ~/.*tar\.gz/:
                    ((UntarTool)project.configure(Tasks.chain(getFsPath(),UntarTool).toDir((File) project['spacelift']['workspace']), remap)).execute().await()
                    break
                case ~/.*tbz/:
                case ~/.*tar\.bz2/:
                    ((UntarTool)project.configure(Tasks.chain(getFsPath(),UntarTool).bzip2(true).toDir((File) project['spacelift']['workspace']), remap)).execute().await()
                    break
                default:
                    logger.warn(":install:${name} Unable to extract ${getFileName()}, unknown archive type")
            }
        }
        else {
            if(new File(getHome(), getFileName()).exists()) {
                logger.info(":install:${name} Deleting previous installation at ${new File(getHome(), getFileName())}")
                project.getAnt().invokeMethod("delete", [file: new File(getHome(), getFileName())])

                logger.info(":install:${name} Reusing existing installation ${new File(getHome(), getFileName())}")
            }
            else {
                logger.info(":install:${name} Copying installation to ${(File) project['spacelift']['workspace']}")
                project.getAnt().invokeMethod("copy", [file: getFsPath(), tofile: new File(getHome(), getFileName())])
            }
        }

        // register tools
        registerTools(GradleSpacelift.toolRegistry())

        // execute post actions
        DSLUtil.resolve(postActions, this)
    }

    String getFileName() {
        String fileNameString = DSLUtil.resolve(String.class, fileName, this)
        if(fileNameString==null) {
            URL url = getRemoteUrl()
            if(url!=null) {
                return guessFileNameFromUrl(url)
            }
            return ""
        }

        return fileNameString.toString()
    }

    boolean getAutoExtract() {
        return DSLUtil.resolve(Boolean.class, autoExtract, this)
    }

    File getFsPath() {
        String filePath = DSLUtil.resolve(String.class, fsPath, this)
        if(filePath==null) {
            filePath = "${(File) project['spacelift']['installationsDir']}/${getProduct()}/${getVersion()}/${getFileName()}"
        }
        return new File(filePath.toString());
    }

    URL getRemoteUrl() {
        String remoteUrlString = DSLUtil.resolve(String.class, remoteUrl, this)
        if(remoteUrlString==null) {
            return null;
        }
        return new URL(remoteUrlString)
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
