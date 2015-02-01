package org.arquillian.spacelift.gradle.maven

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.task.Task
import org.slf4j.LoggerFactory

class MavenExecutor extends Task<Object, ProcessResult>{

    def static final log = LoggerFactory.getLogger('MavenExecutor')

    def projectPom

    def workingDir

    def batchMode = true

    def nonRecursive = false

    def debug = false

    def settingsXml

    def goals = []

    def profiles = []

    def properties = []

    Map<String, String> env = [:]

    private def command = []

    MavenExecutor() {
        def project = new GradleSpaceliftDelegate().project()
        if (new File("${project.spacelift.workspace}/settings.xml").exists()) {
            this.settingsXml = "${project.spacelift.workspace}/settings.xml"
            properties << "org.apache.maven.user-settings=${project.spacelift.workspace}/settings.xml"
        }
    }

    @Override
    protected ProcessResult process(Object input) throws Exception {

        def command = Spacelift.task('mvn')

        if (batchMode) {
            command.parameter('-B')
        }

        if(nonRecursive) {
            command.parameter('-N')
        }

        if(debug) {
            command.parameter('-X')
        }

        command.parameters(getProfiles())

        if (projectPom) {
            command.parameter('-f')
            command.parameter(projectPom)
        }

        if (settingsXml) {
            command.parameter('-s')
            command.parameter(settingsXml)
        }

        command.addEnvironment(env)

        command.parameters(goals)
        command.parameters(getProperties())

        if(workingDir) {
            command.workingDir(workingDir)
        }

        return command.interaction(GradleSpaceliftDelegate.ECHO_OUTPUT).execute().await()
    }

    def pom(projectPom) {
        this.projectPom = projectPom
        this
    }

    def withoutSubprojects() {
        this.nonRecursive = true
        this
    }

    def withoutBatchMode() {
        this.batchMode = false
        this
    }

    def debug() {
        this.debug = true
        this
    }

    def workingDir(dir) {
        this.workingDir = (dir instanceof File) ? dir.getAbsolutePath() : dir;
        this
    }

    def settings(settingsXml) {
        this.settingsXml = settingsXml
        this
    }

    def goal(goal) {
        this.goals.add(goal)
        this
    }

    def goals(CharSequence...goals) {
        this.goals.addAll(goals)
        this
    }

    def property(property) {
        this.properties.add(property)
        this
    }

    def properties(CharSequence...properties) {
        this.properties.addAll(properties)
        this
    }

    def profile(profile) {
        this.profiles.add(profile)
        this
    }

    def profiles(CharSequence...profiles) {
        this.profiles.addAll(profiles)
        this
    }

    def env(key, value) {
        this.env.put(key.toString(), value.toString())
        this
    }

    def env(envProperty) {
        for(def entry:envProperty) {
            this.env.put(entry.key.toString(), entry.value.toString())
        }
        this
    }

    def androidTarget(target) {
        // TODO identify all possible combinations for Android Target settings
        this.properties << "arq.group.containers.container.android.configuration.target=${target}"
        this
    }

    def jbossHome(jbossHome) {
        this.properties << "arq.container.main-server-group.configuration.jbossHome=${jbossHome}"
        this.properties << "arq.group.jboss.container.domain-controller.configuration.jbossHome=${jbossHome}"
        this
    }

    def surefireSuffix(suffix) {
        this.properties << "surefire.reportNameSuffix=${suffix}"
        this
    }

    def ignoreTestFailures() {
        this.properties << "maven.test.failure.ignore=true"
        this
    }

    private def getProfiles() {
        def profs = []
        this.profiles.each { p ->
            profs << "-P" + p
        }
        profs
    }

    private def getProperties() {
        def props = []
        this.properties.each { p -> props << "\"-D${p}\"" }
        props
    }
}
