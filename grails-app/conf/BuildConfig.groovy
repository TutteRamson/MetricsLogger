grails.project.work.dir = 'target'

grails.project.dependency.resolution = {

    inherits 'global'
    log 'warn'

    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        compile 'joda-time:joda-time:2.3'
        compile 'org.apache.commons:commons-lang3:3.2'

        test 'junit:junit:4.11', {
            export = false
        }
        test 'org.mockito:mockito-core:1.9.0', {
            export = false
        }
        test 'org.hamcrest:hamcrest-core:1.3', {
            export = false
        }
        test 'org.powermock:powermock-api-mockito:1.4.12', {
            export = false
        }
        test 'org.powermock:powermock-module-junit4:1.4.12', {
            export = false
        }
    }

    plugins {
        build ':release:3.0.1', ':rest-client-builder:1.0.3', {
            export = false
        }

        compile ':webxml:1.4.1'
    }
}
