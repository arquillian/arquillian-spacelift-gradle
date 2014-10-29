package org.arquillian.spacelift.gradle.openshift

/**
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
class OpenshiftMetadata {

    private String url
    private String ssh
    private String git
    private String cloned
    private String dbuser
    private String dbpassword
    private String dbname

    def setUrl(String url) {
        this.url = url
    }

    def setSsh(String ssh) {
        this.ssh = ssh
    }

    def setGit(String git) {
        this.git = git
    }

    def setCloned(String cloned) {
        this.cloned = cloned
    }

    def setDBUser(String dbuser){
        this.dbuser = dbuser
    }

    def setDBPassword(String dbpassword){
        this.dbpassword = dbpassword
    }

    def setDBName(String dbname) {
        this.dbname = dbname
    }

    public String getDbuser() {
        return dbuser
    }

    public void setDbuser(String dbuser) {
        this.dbuser = dbuser
    }

    public String getDbpassword() {
        return dbpassword
    }

    public void setDbpassword(String dbpassword) {
        this.dbpassword = dbpassword
    }

    public String getDbname() {
        return dbname
    }

    public void setDbname(String dbname) {
        this.dbname = dbname
    }

    public String getUrl() {
        return url
    }

    public String getSsh() {
        return ssh
    }

    public String getGit() {
        return git
    }

    public String getCloned() {
        return cloned
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()

        sb.append("URL: ").append(url).append("\n")
                .append("SSH to: ").append(ssh).append("\n")
                .append("Git remote: ").append(git).append("\n")
                .append("Cloned to: ").append(cloned).append("\n")
                .append("Root User: ").append(dbuser).append("\n")
                .append("Root Password: ").append(dbpassword).append("\n")
                .append("Database Name: ").append(dbname).append("\n")


        return sb.toString()
    }
}
