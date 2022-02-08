buildscript {
    ext.kotlin_version = "1.5.30"
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version", "org.apache.tuweni:tuweni-toml:2.1.0"
    }
}


plugins {
    id "org.jetbrains.intellij" version "1.3.1"
}

intellij {
    pluginName = project.name
    version = "2021.3"
    type = "PC"
    updateSinceUntilBuild = false
    downloadSources = true
    plugins = ["python-ce"]
}


patchPluginXml {
    sinceBuild = "213.5744.223"
    untilBuild = "213.*"
}


allprojects {
    apply plugin: "org.jetbrains.intellij"
    apply plugin: "kotlin"
    apply plugin: "jacoco"
    repositories {
        mavenCentral()
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
            languageVersion = "1.4"
            apiVersion = "1.4"
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "11"
            languageVersion = "1.4"
            apiVersion = "1.4"
        }
    }

    dependencies {
        implementation 'org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.5.21'
        compile 'org.apache.tuweni:tuweni-toml:2.0.0'
        compile group: 'org.ini4j', name: 'ini4j', version: '0.5.4'
        testImplementation group: 'org.junit.jupiter', name: 'junit-jupiter-api', version: '5.8.2'
        compile group: 'org.jetbrains', name: 'annotations', version: '23.0.0'
    }

    jacocoTestReport {
        reports {
            xml.enabled true
            html.enabled true
        }
    }
    sourceCompatibility = 11
    targetCompatibility = 11
}

sourceSets {
    main {
        java.srcDir 'src'
        resources.srcDir 'resources'
    }
    test {
        java.srcDir 'testSrc'
        resources.srcDir 'testData'
    }
}
