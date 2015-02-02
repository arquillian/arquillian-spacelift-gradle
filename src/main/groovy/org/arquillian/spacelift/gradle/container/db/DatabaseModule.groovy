package org.arquillian.spacelift.gradle.container.db

import org.arquillian.spacelift.Spacelift
import org.jboss.aerogear.test.container.manager.JBossManager
import org.jboss.aerogear.test.container.manager.JBossManagerConfiguration
import org.jboss.aerogear.test.container.spacelift.JBossStarter
import org.jboss.aerogear.test.container.spacelift.JBossStopper

abstract class DatabaseModule {

    String jbossHome

    String name

    String destination

    String version

    boolean startContainer = false

    private JBossManager manager

    /**
     *
     * @param name just some identifier
     * @param jbossHome location of container you want to add this module to
     * @param destination where to download / save resolved artifact
     */
    DatabaseModule(String name, String jbossHome, String destination) {
        this.name = name
        this.jbossHome = jbossHome
        this.destination = destination
    }

    def version(String version) {
        this.version = version
        this
    }

    def shouldStartContainer() {
        startContainer = true
        this
    }

    def startContainer() {
        if (startContainer) {
            manager = Spacelift.task(JBossStarter).configuration(new JBossManagerConfiguration().setJBossHome(jbossHome)).execute().await()
        }
    }

    def stopContainer() {
        if (startContainer && manager) {
            Spacelift.task(manager, JBossStopper).execute().await()
        }
    }

    abstract def install()

    abstract def uninstall()
}
