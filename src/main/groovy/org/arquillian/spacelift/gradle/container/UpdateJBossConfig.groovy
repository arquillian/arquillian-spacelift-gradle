package org.arquillian.spacelift.gradle.container

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.task.Task
import org.jboss.aerogear.test.container.manager.JBossManager
import org.jboss.aerogear.test.container.manager.JBossManagerConfiguration
import org.jboss.aerogear.test.container.spacelift.JBossCLI
import org.jboss.aerogear.test.container.spacelift.JBossStarter
import org.jboss.aerogear.test.container.spacelift.JBossStopper

class UpdateJBossConfig extends Task<File, Void> {

    JBossManagerConfiguration configuration = new JBossManagerConfiguration()

    boolean shouldStartContainer = false

    UpdateJBossConfig jbossHome(File jbossHome) {
        jbossHome(jbossHome.getCanonicalPath())
    }

    UpdateJBossConfig jbossHome(String jbossHomePath) {
        this.configuration.setJBossHome(jbossHomePath)
        return this
    }

    /**
     * Sets jboss manager configuration. Overrides {@link UpdateJBossConfig#jbossHome(jbossHome)} value
     * @param configuration
     * @return
     */
    UpdateJBossConfig jbossConfiguration(JBossManagerConfiguration configuration) {
        this.configuration = configuration
        return this
    }

    UpdateJBossConfig shouldStartContainer() {
        shouldStartContainer = true
        return this
    }

    @Override
    protected Void process(File input) throws Exception {
        if(input==null || !input.exists() || !input.canRead()) {
            throw new IllegalArgumentException("Input file (${input ? input.canonicalPath : 'null'}) must exists and be readable")
        }

        JBossManager manager
        if (shouldStartContainer) {
            manager = Spacelift.task(JBossStarter)
                    .configuration(configuration)
                    .execute()
                    .await()
        }

        Spacelift.task(JBossCLI)
                .environment("JBOSS_HOME", configuration.getJBossHome())
                .file(input.canonicalPath)
                .execute()
                .await()

        if (shouldStartContainer) {
            Spacelift.task(manager, JBossStopper).execute().await()
        }

        return null
    }
}
