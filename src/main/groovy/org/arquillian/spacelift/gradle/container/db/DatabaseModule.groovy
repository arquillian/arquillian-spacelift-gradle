package org.arquillian.spacelift.gradle.container.db

import org.arquillian.spacelift.Spacelift
import org.jboss.aerogear.test.container.manager.JBossManager
import org.jboss.aerogear.test.container.manager.JBossManagerConfiguration
import org.jboss.aerogear.test.container.spacelift.JBossStarter
import org.jboss.aerogear.test.container.spacelift.JBossStopper

abstract class DatabaseModule<DBM extends DatabaseModule<DBM>> {

    String jbossHome

    String name

    protected String version

    boolean startContainer = false

    private JBossManager manager

    /**
     *
     * @param name name of the module (see module add --name)
     * @param jbossHome location of container you want to add this module to
     */
    DatabaseModule(String name, String jbossHome) {
        this.name = name
        this.jbossHome = jbossHome
    }

    DatabaseModule(String name, File jbossHome) {
        this(name, jbossHome.canonicalPath)
    }

    DBM version(String version) {
        this.version = version
        return this
    }

    DBM shouldStartContainer() {
        startContainer = true
        return this
    }

    DBM startContainer() {
        if (startContainer) {
            manager = Spacelift.task(JBossStarter).configuration(new JBossManagerConfiguration().setJBossHome(jbossHome)).execute().await()
        }
        return this
    }

    DBM stopContainer() {
        if (startContainer && manager) {
            Spacelift.task(manager, JBossStopper).execute().await()
        }
        return this
    }

    /**
     * Installs module into container
     * @return
     */
    abstract DBM install()

    /**
     * Uninstalls module from container
     * @return
     */
    abstract DBM uninstall()
}
