/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the 
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arquillian.spacelift.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

/**
 * @author <a href="mailto:smikloso@redhat.com">Stefan Miklosovic</a>
 *
 */
public class ScriptParsingTest {
    @Test
    public void executeTestReport() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.repositories { mavenCentral() }

        project.configurations { junitreport }

        project.dependencies { junitreport 'org.apache.ant:ant-junit:1.9.4' }

        project.spacelift {
            tests {
                openshiftIntegrationTests {
                    execute {
                        println "openshiftIntegrationTests"
                    }
                }
                localIntegrationTests {
                    execute {
                        printn "localIntegrationTests"
                    }
                }
            }
            tools {
            }
            profiles {
                openshift {
                    enabledInstallations 'a', 'b'
                    tests 'openshiftIntegrationTests'
                }
                local {
                    tests 'localIntegrationTests'
                }
                eap6(inherits: local) {
                    enabledInstallations 'c', 'd'
                }
        
            }
            installations {
                a {}
                b {}
                c {}
                d {}
            }
        }

        project.getTasks()['init'].execute()
    }
}
