package org.arquillian.spacelift.gradle.maven

import java.text.MessageFormat

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.gradle.xml.XmlFileLoader
import org.arquillian.spacelift.gradle.xml.XmlTextLoader
import org.arquillian.spacelift.gradle.xml.XmlUpdater
import org.arquillian.spacelift.task.Task
import org.slf4j.LoggerFactory

class SettingsXmlUpdater extends Task<Object, Void> {

    def static final log = LoggerFactory.getLogger('SettingsXmlUpdater')

    def static final PROFILE_TEMPLATE = '''
        <profile>
            <id>{0}</id>

            <repositories>
                <repository>
                    <id>{0}</id>
                    <name>{0}</name>
                    <url>{1}</url>
                    <layout>default</layout>
                    <releases>
                        <enabled>true</enabled>
                        <updatePolicy>never</updatePolicy>
                    </releases>
                    <snapshots>
                        <enabled>{2}</enabled>
                        <updatePolicy>never</updatePolicy>
                    </snapshots>
                </repository>
            </repositories>
            <!-- plugin repositories are required to fetch dependencies for plugins (e.g. gwt-maven-plugin) -->
            <pluginRepositories>
                <pluginRepository>
                    <id>{0}</id>
                    <name>{0}</name>
                    <url>{1}</url>
                    <layout>default</layout>
                    <releases>
                        <enabled>true</enabled>
                        <updatePolicy>never</updatePolicy>
                    </releases>
                    <snapshots>
                        <enabled>{2}</enabled>
                        <updatePolicy>never</updatePolicy>
                    </snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    '''

    def static final PROFILE_ACTIVATION_TEMPLATE = '''
        <activeProfile>{0}</activeProfile>
    '''

    def settingsXmlFile

    def localRepositoryDir

    def repositories = []

    SettingsXmlUpdater() {

        def project = new GradleSpaceliftDelegate().project()
        def ant = project.ant

        this.settingsXmlFile = new File(project.spacelift.workspace, "settings.xml")

        // ensure there is a settings.xml file
        if(!settingsXmlFile.exists()) {
            log.warn("No settings.xml file found in ${project.spacelift.workspace}, trying to copy the one from default location")
            def defaultFile = new File(new File(System.getProperty("user.home")), '.m2/settings.xml')

            if(!defaultFile.exists()) {
                log.warn("No settings.xml file found in ${defaultFile.getAbsolutePath()}, using fallback template")
                defaultFile = File.createTempFile("settings.xml", "-spacelift");
                this.getClass().getResource("/settings.xml-template").withInputStream { ris ->
                    defaultFile.withOutputStream { fos -> fos << ris }
                }
            }

            ant.copy(file: "${defaultFile}", tofile: "${settingsXmlFile}")
        }

        this.localRepositoryDir = project.spacelift.localRepository
    }

    SettingsXmlUpdater repository(repositoryId, repositoryUri, snapshotsEnabled) {
        repositories.add(["repositoryId":repositoryId, "repositoryUri": repositoryUri, "snapshotsEnabled":snapshotsEnabled])
        this
    }

    @Override
    protected Void process(Object input) throws Exception {
        def settings = Spacelift.task(settingsXmlFile, XmlFileLoader).execute().await()

        // in case there is not any 'profiles' or 'activeProfiles' element, add them to settings.xml
        if (settings.profiles.isEmpty()) {
            settings.children().add(0, new Node(null, 'profiles'))
        }

        if (settings.activeProfiles.isEmpty()) {
            settings.children().add(0, new Node(null, 'activeProfiles'))
        }

        // update with defined repositories
        repositories.each { r ->
            def profileElement = Spacelift.task(MessageFormat.format(PROFILE_TEMPLATE, r.repositoryId, r.repositoryUri, r.snapshotsEnabled), XmlTextLoader).execute().await()
            def profileActivationElement = Spacelift.task(MessageFormat.format(PROFILE_ACTIVATION_TEMPLATE, r.repositoryId), XmlTextLoader).execute().await()

            // profiles

            // remove previous profiles with the same id
            settings.profiles.profile.findAll { p -> p.id.text() == "${r.repositoryId}" }.each { it.replaceNode {} }
            // append profiles
            settings.profiles.each { it.append(profileElement) }

            // activeProfiles

            // remove previous profile activations
            settings.activeProfiles.activeProfile.findAll { ap -> ap.text() == "${r.repositoryId}" }.each { it.replaceNode {} }
            // append profile activations
            settings.activeProfiles.each { it.append(profileActivationElement) }
        }

        // update with local repository
        // delete <localRepository> if present
        settings.localRepository.each { it.replaceNode {} }
        settings.children().add(0, new Node(null, 'localRepository', "${localRepositoryDir.getAbsolutePath()}"))

        Spacelift.task(settings, XmlUpdater).file(settingsXmlFile).execute().await()
        return null;
    }

}
