package org.arquillian.spacelift.gradle.openshift

import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.task.Task

class CreateOpenShiftCartridge extends Task<Object, ProcessResult> {

    private String name
    private String namespace
    private String server
    private String gear
    private boolean isScaling = false
    private boolean noGit = true
    private boolean force = false
    private boolean ignoreIfExists = false
    private List<String> cartridges = new ArrayList<String>();
    private String repo
    private File gitSsh

    // credentials
    def password
    def login
    def token

    @Override
    protected ProcessResult process(Object unused) throws Exception {

        // delete app if it exists
        if(force) {
            def command = Spacelift.task('rhc')
                    .parameters("app", "delete", "--confirm", name, "-n", namespace)
                    // if app is not present, it will fail with 101
                    .shouldExitWith(0, 101)
                    .interaction(GradleSpaceliftDelegate.ECHO_OUTPUT)

            if(login) {
                command.parameters("-l", login)
            }
            if(password) {
                command.parameters("-p", password)
            }
            if(token) {
                command.parameters("--token", token)
            }

            command.execute().await()
        }

        def command = Spacelift.task('rhc')

        if (gitSsh) {
            command.addEnvironment(["GIT_SSH": gitSsh.getAbsolutePath()])
        }

        command.parameters("app", "create")

        command.parameters("-a", name)
        command.parameters("-n", namespace)

        if(gear) {
            command.parameters("-g", gear)
        }
        if(isScaling) {
            command.parameter("-s")
        }
        if(noGit) {
            command.parameter("--no-git")
        }
        if(server) {
            command.parameters("--server", server)
        }
        if(login) {
            command.parameters("-l", login)
        }
        if(password) {
            command.parameters("-p", password)
        }
        if(token) {
            command.parameters("--token", token)
        }
        if (repo) {
            command.parameters("--repo", repo)
        }
        if(ignoreIfExists) {
            command.shouldExitWith(0,1)
        }

        command.parameters(cartridges)

        ProcessResult result = command.interaction(GradleSpaceliftDelegate.ECHO_OUTPUT).execute().await()

        result
    }

    CreateOpenShiftCartridge ignoreIfExists(boolean ignoreIfExists) {
        this.ignoreIfExists = ignoreIfExists
        this
    }

    CreateOpenShiftCartridge force(boolean force) {
        this.force = force
        this
    }

    CreateOpenShiftCartridge named(String name) {
        this.name = name
        this
    }

    CreateOpenShiftCartridge cartridges(CharSequence... cartridges) {
        this.cartridges.addAll(cartridges)
        this
    }

    CreateOpenShiftCartridge sized(String gear) {
        this.gear = gear
        this
    }

    CreateOpenShiftCartridge at(String namespace) {
        this.namespace = namespace
        this
    }

    CreateOpenShiftCartridge server(String server) {
        this.server = server
        this
    }

    CreateOpenShiftCartridge scale(boolean scale) {
        this.isScaling = scale
        this
    }

    CreateOpenShiftCartridge checkout(boolean checkout) {
        this.noGit = !checkout
        this
    }

    CreateOpenShiftCartridge username(String username) {
        this.login = username
        this
    }

    CreateOpenShiftCartridge password(String password) {
        this.password = password
        this
    }

    CreateOpenShiftCartridge token(String token) {
        this.token = token
        this
    }

    CreateOpenShiftCartridge repo(String repo) {
        this.repo = repo
        this
    }

    CreateOpenShiftCartridge gitSsh(File gitSsh) {
        if (gitSsh && gitSsh.exists() && gitSsh.isFile()) {
            this.gitSsh = gitSsh
        }
        this
    }
}
