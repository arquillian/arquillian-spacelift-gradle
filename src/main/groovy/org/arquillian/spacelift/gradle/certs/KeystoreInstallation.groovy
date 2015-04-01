package org.arquillian.spacelift.gradle.certs

/**
 * Created by kpiwko on 26/03/15.
 */
import groovy.transform.CompileStatic
import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.gradle.utils.EnvironmentUtils
import org.slf4j.LoggerFactory
import org.arquillian.spacelift.gradle.BaseContainerizableObject
import org.arquillian.spacelift.gradle.DeferredValue
import org.arquillian.spacelift.gradle.Installation
import org.arquillian.spacelift.task.TaskRegistry
import org.slf4j.Logger

/**
 * Creates a keystore/truststore combo that by default will contain entry for current host.
 * Hostname of current host is by default its IP address but you can override it.
 *
 * Requires tool 'keytool' to be registered
 */
@CompileStatic
class KeystoreInstallation extends BaseContainerizableObject<KeystoreInstallation> implements Installation {

    private static final Logger logger = LoggerFactory.getLogger(KeystoreInstallation.class)

    DeferredValue<String> product = DeferredValue.of(String.class).from("unused")
    DeferredValue<String> version = DeferredValue.of(String.class).from("unused")

    // a file whereof keystore is copied. Spacelift embeds its own keystore based on JDK7 content
    DeferredValue<File> keystoreSource = DeferredValue.of(File.class).from({
        logger.info(":install:${name} copying Spacelift reference keystore")
        File keystore =  File.createTempFile("keystore", "-spacelift")
        KeystoreInstallation.class.getResource("/certs/aerogear.keystore").withInputStream { InputStream ris ->
            keystore.withOutputStream { OutputStream fos -> fos << ris }
        }
        return keystore
    })

    // password of the keystore
    DeferredValue<String> keystorePass = DeferredValue.of(String.class).from("aerogear")

    // location where keystore is copied to
    DeferredValue<File> keystore = DeferredValue.of(File.class).from({
        return new File(getHome(), 'aerogear.keystore')
    })

    // a file whereof truststore is copied. Spacelift embeds its own keystore based on JDK7 content
    DeferredValue<File> truststoreSource = DeferredValue.of(File.class).from({
        logger.info(":install:${name} copying Spacelift reference truststore")
        File truststore =  File.createTempFile("truststore", "-spacelift")
        KeystoreInstallation.class.getResource("/certs/aerogear.truststore").withInputStream { InputStream ris ->
            truststore.withOutputStream { OutputStream fos -> fos << ris }
        }
        return truststore
    })

    // password of the truststore
    DeferredValue<String> truststorePass = DeferredValue.of(String.class).from("aerogear")

    // location where truststore is copied to
    DeferredValue<File> truststore = DeferredValue.of(File.class).from({
        return new File(getHome(), "aerogear.truststore")
    })

    // alias of generated local certificate
    DeferredValue<String> alias = DeferredValue.of(String.class).from("aerogear")

    // path to generated local certificate
    DeferredValue<File> localCert = DeferredValue.of(File.class).from({
        return new File(getHome(), "aerogear.cer")
    })

    // home of the installation in workspace
    DeferredValue<File> home = DeferredValue.of(File.class).from("certs")

    // check whether installation is installed
    DeferredValue<Boolean> isInstalled = DeferredValue.of(Boolean.class).from({
        return (getTruststore().exists() && getKeystore().exists())
    })

    // flag to generate local certificate
    DeferredValue<Boolean> generateLocalCert = DeferredValue.of(Boolean.class).from(true)

    // ips to bind local certificate to
    DeferredValue<List> localCertIps = DeferredValue.of(List.class).from({
        [EnvironmentUtils.ipVersionsToTest().collect { List hostIp ->
            return hostIp[0] == EnvironmentUtils.IP.v6 ? "[${hostIp[1]}]" : hostIp[1]
        }
        // FIXME on purpose we are just returning IPv4 variant of IP address
         .first()]
    })

    KeystoreInstallation(String name, Object parent) {
        super(name, parent)
    }

    KeystoreInstallation(String name, KeystoreInstallation other) {
        super(name, other)

        this.@product = other.@product.copy()
        this.@version = other.@version.copy()
        this.@keystoreSource = other.@keystoreSource.copy()
        this.@keystorePass = other.@keystorePass.copy()
        this.@keystore = other.@keystore.copy()
        this.@truststoreSource = other.@truststoreSource.copy()
        this.@truststorePass = other.@truststorePass.copy()
        this.@truststore = other.@truststore.copy()
        this.@alias = other.@alias.copy()
        this.@localCert = other.@localCert.copy()
        this.@home = other.@home.copy()
        this.@isInstalled = other.@isInstalled.copy()
        this.@generateLocalCert = other.@generateLocalCert.copy()
        this.@localCertIps = other.@localCertIps.copy()
    }

    @Override
    KeystoreInstallation clone(String name) {
        return new KeystoreInstallation(name, this)
    }

    File getLocalCert() {
        return localCert.resolve()
    }

    File getKeystore() {
        return keystore.resolve()
    }

    File getTruststore() {
        return truststore.resolve()
    }

    String getKeystorePass() {
        return keystorePass.resolve()
    }

    String getTruststorePass() {
        return truststorePass.resolve()
    }

    String getAlias() {
        return alias.resolve()
    }

    @Override
    String getProduct() {
        return product.resolve()
    }

    @Override
    String getVersion() {
        return version.resolve()
    }

    @Override
    File getHome() {
        return home.resolve()
    }

    @Override
    boolean isInstalled() {
        return isInstalled.resolve()
    }

    @Override
    void install(Logger logger) {

        // FIXME copy should be done a different way, via Spacelift tasks
        File keystoreSource = keystoreSource.resolve()
        logger.info(":install:${name} Copying keystore from ${keystoreSource} to ${getKeystore()}")
        new GradleSpaceliftDelegate().project().getAnt().invokeMethod("copy", [file: keystoreSource, tofile: getKeystore()])

        File truststoreSource = truststoreSource.resolve()
        logger.info(":install:${name} Copying truststore from ${truststoreSource} to ${getTruststore()}")
        new GradleSpaceliftDelegate().project().getAnt().invokeMethod("copy", [file: truststoreSource, tofile: getTruststore()])

        if(generateLocalCert.resolve()) {
            localCertIps.resolve().each { Object hostName ->
                logger.info("install:${name} Generating local certificate with alias ${getAlias()} for host ${hostName}")
                addHostNameToKeystore(hostName.toString())
            }
        }
    }

    @Override
    void registerTools(TaskRegistry registry) {
    }

    private void addHostNameToKeystore(String ip) {
        // magic from Adam Saleh
        String canonical = ip.split('.').collect { "CN=${it},"}.join(' ')
        def preset = Spacelift.task(KeyTool)
                .keystore(getKeystore())
                .alias(getAlias())
                .keypass(getKeystorePass())
                .storepass(getTruststorePass())

        def trustPreset = Spacelift.task(KeyTool).keytoolAsPreset(preset)
                .keystore(getTruststore())
                .trustcacerts()

        Spacelift.task(KeyTool).keytoolAsPreset(trustPreset)
                .cmdDelete()
                .shouldExitWith(0,1)
                .execute().await()

        Spacelift.task(KeyTool).keytoolAsPreset(preset)
                .cmdDelete()
                .shouldExitWith(0,1)
                .execute().await()

        Spacelift.task(KeyTool).keytoolAsPreset(preset)
                .cmdGenKeyPair("RSA", "3650", "${canonical}OU=Aerogear, O=JBossQA, L=Brno, ST=JM, C=CZ/", "san=ip:${ip}")//,dns:${host}")
                .execute().await()

        File certFile = getLocalCert()
        Spacelift.task(KeyTool).keytoolAsPreset(preset)
                .cmdExport(certFile)
                .execute().await()

        Spacelift.task(KeyTool).keytoolAsPreset(trustPreset)
                .cmdImport(certFile)
                .execute().await()
    }
}
