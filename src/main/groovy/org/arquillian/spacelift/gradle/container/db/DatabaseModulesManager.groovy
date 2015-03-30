package org.arquillian.spacelift.gradle.container.db


class DatabaseModulesManager {

    def databaseModules = []

    DatabaseModulesManager add(DatabaseModule module) {
        databaseModules.add(module)
        return this
    }

    DatabaseModulesManager install(String name) {
        databaseModules.each { module ->
            if (module.name == name) {
                module.install()
            }
        }
        return this
    }

    DatabaseModulesManager install(names) {
        databaseModules.each { module ->
            if (names.contains(module.name)) {
                module.install()
            }
        }
        return this
    }
    
    DatabaseModulesManager installAll() {
        databaseModules.each { module ->
            module.install()
        }
        return this
    }

    DatabaseModulesManager uninstall(String name) {
        databaseModules.each { module ->
            if (module.name == name) {
                module.uninstall()
            }
        }
        return this
    }

    def uninstallAll() {
        databaseModules.each { module ->
            module.uninstall()
        }
        return this
    }

    int size() {
        return databaseModules.size()
    }
}
