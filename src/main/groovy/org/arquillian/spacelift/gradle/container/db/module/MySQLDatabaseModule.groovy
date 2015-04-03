package org.arquillian.spacelift.gradle.container.db.module

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.container.db.DatabaseModule
import org.jboss.aerogear.test.container.spacelift.JBossCLI
import org.jboss.shrinkwrap.resolver.api.maven.Maven

class MySQLDatabaseModule extends DatabaseModule<MySQLDatabaseModule> {


    MySQLDatabaseModule(String name, String jbossHome) {
        super(name, jbossHome)
    }

    MySQLDatabaseModule(String name, File jbossHome) {
        super(name, jbossHome)
        // default version
        this.version = "5.1.28"
    }

    @Override
    MySQLDatabaseModule install() {

        // path to the module
        String path = name.replaceAll("\\.","/")
        File driver = Maven.resolver().resolve("mysql:mysql-connector-java:${version}").withoutTransitivity().asSingleFile()
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
    MySQLDatabaseModule uninstall() {
        startContainer()
        Spacelift.task(JBossCLI).environment("JBOSS_HOME", jbossHome).connect().cliCommand("module remove --name=${name}").execute().await()
        stopContainer()

        return this
    }
}
