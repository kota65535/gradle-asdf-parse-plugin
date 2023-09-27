package io.github.kota65535.gradle.plugin

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

class AsdfParsePluginTest extends Specification {
    @TempDir
    File testProjectDir
    File buildFile
    File toolVersionsFile

    def "default pattern"() {
        given:
        buildFile = new File(testProjectDir, 'build.gradle')
        buildFile << """
            plugins {
                id 'io.github.kota65535.asdf-parse'
            }
            
            println "pythonMajorVersion:"      + pythonMajorVersion
            println "pythonMinorVersion:"      + pythonMinorVersion
            println "pythonPatchVersion:"      + pythonPatchVersion
            println "nodejsMajorVersion:"      + nodejsMajorVersion
            println "nodejsMinorVersion:"      + nodejsMinorVersion
            println "nodejsPatchVersion:"      + nodejsPatchVersion
        """
        toolVersionsFile = new File(testProjectDir, '.tool-versions')
        toolVersionsFile << """
        python 3.11.2
        nodejs 18.17.0
        java openjdk-20.0.2
        """
        when:
        def result = GradleRunner.create()
                .withDebug(true)
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .build()

        then:
        result.output.contains("pythonMajorVersion:3")
        result.output.contains("pythonMinorVersion:11")
        result.output.contains("pythonPatchVersion:2")
        result.output.contains("nodejsMajorVersion:18")
        result.output.contains("nodejsMinorVersion:17")
        result.output.contains("nodejsPatchVersion:0")
        result.output.contains("pattern not matches. tool: java, version: openjdk-20.0.2, pattern: %s".formatted(AsdfParsePlugin.DEFAULT_PATTERN));

    }

    def "custom patterns"() {
        given:
        buildFile = new File(testProjectDir, 'build.gradle')
        buildFile << """
            plugins {
                id 'io.github.kota65535.asdf-parse'
            }
            
            asdfParse {
              patterns = [
                '*' : /^(?<majorMinorVersion>(?<majorVersion>\\d+)\\.(?<minorVersion>\\d+))\\.(?<patchVersion>\\d+)\$/,
                java: /^(?<distribution>\\w+)-(?<majorVersion>\\d+)\\.(?<minorVersion>\\d+)\\.(?<patchVersion>[\\d.]+)\$/,
              ]
            }
            
            println "javaDistribution:"        + javaDistribution
            println "javaMajorVersion:"        + javaMajorVersion
            println "javaMinorVersion:"        + javaMinorVersion
            println "javaPatchVersion:"        + javaPatchVersion
            println "pythonMajorVersion:"      + pythonMajorVersion
            println "pythonMajorMinorVersion:" + pythonMajorMinorVersion
            println "pythonMinorVersion:"      + pythonMinorVersion
            println "pythonPatchVersion:"      + pythonPatchVersion
        """
        toolVersionsFile = new File(testProjectDir, '.tool-versions')
        toolVersionsFile << """
        java openjdk-20.0.2
        python 3.11.2
        """
        when:
        def result = GradleRunner.create()
                .withDebug(true)
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .build()

        then:
        result.output.contains("javaDistribution:openjdk")
        result.output.contains("javaMajorVersion:20")
        result.output.contains("javaMinorVersion:0")
        result.output.contains("javaPatchVersion:2")
        result.output.contains("pythonMajorVersion:3")
        result.output.contains("pythonMajorMinorVersion:3.11")
        result.output.contains("pythonMinorVersion:11")
        result.output.contains("pythonPatchVersion:2")
    }
}
