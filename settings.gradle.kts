pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Композитные сборки: координаты com.materialfluid:*, com.suldev.kit:* и
// com.suldev:suldev-ui подменяются локальными проектами экосистемы.
includeBuild("../material-fluid")
includeBuild("../suldev-kit")
includeBuild("../suldev-ui")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "sul-pdd"
include(":app")
