package org.arquillian.spacelift.gradle.container.db.module

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.container.db.DatabaseModule
import org.jboss.aerogear.test.container.spacelift.JBossCLI
import org.jboss.shrinkwrap.resolver.api.maven.Maven


class PostgreSQLDatabaseModule extends DatabaseModule<PostgreSQLDatabaseModule> {

    protected String version = "9.3-1100-jbdc41"

    PostgreSQLDatabaseModule(String name, String jbossHome) {
        super(name, jbossHome)
    }

    PostgreSQLDatabaseModule(String name, File jbossHome) {
        super(name, jbossHome)
    }

    @Override
    PostgreSQLDatabaseModule install() {

        // path to the module
        String path = name.replaceAll("\\.","/")
        File driver = Maven.resolver().resolve("org.postgresql:postgresql:${version}").withoutTransitivity().asSingleFile()
        if (! new File(jbossHome, "/modules/${path}").exists()) {

            startContainer()
            Spacelift.task(JBossCLI)
                    .environment("JBOSS_HOME", jbossHome)
                    .connect()
                    .cliCommand("module add --name=${name} --resources=${driver.canonicalPath} --dependencies=javax.api,javax.transaction.api")
                    .execute()
                    .await()
            stopContainer()
        }
        return this
    }

    @Override
    PostgreSQLDatabaseModule uninstall() {
        startContainer()
        Spacelift.task(JBossCLI).environment("JBOSS_HOME", jbossHome).connect().cliCommand("module remove --name=org.postgresql").execute().await()
        stopContainer()

        return this
    }
}
