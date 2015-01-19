package org.arquillian.spacelift.gradle.db

import org.arquillian.spacelift.execution.Tasks
import org.jboss.aerogear.test.container.manager.JBossManager
import org.jboss.aerogear.test.container.manager.JBossManagerConfiguration
import org.jboss.aerogear.test.container.spacelift.JBossCLI
import org.jboss.aerogear.test.container.spacelift.JBossStarter
import org.jboss.aerogear.test.container.spacelift.JBossStopper

class UpdateJBossDatasource {

    String jbossHome = System.getenv("JBOSS_HOME")

    String script

    boolean shouldStartContainer = false

    UpdateJBossDatasource withScript(String script) {
        this.script = script
        this
    }

    UpdateJBossDatasource withScript(File script) {
        withScript(script.getCanonicalPath())
    }

    UpdateJBossDatasource withJBossHome(File jbossHome) {
        withJBossHome(jbossHome.getCanonicalPath())
    }

    UpdateJBossDatasource withJBossHome(String jbossHome) {
        this.jbossHome = jbossHome
        this
    }

    UpdateJBossDatasource shouldStartContainer() {
        shouldStartContainer = true
        this
    }

    UpdateJBossDatasource update() {

        JBossManager manager

        if (shouldStartContainer) {
            manager = Tasks.prepare(JBossStarter)
                    .configuration(new JBossManagerConfiguration().setJBossHome(jbossHome))
                    .execute()
                    .await()
        }

        Tasks.prepare(JBossCLI)
                .environment("JBOSS_HOME", jbossHome)
                .file(script)
                .execute()
                .await()

        if (shouldStartContainer) {
            Tasks.chain(manager, JBossStopper).execute().await()
        }

        return this
    }
}
