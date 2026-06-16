pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/spring")
        }
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "campus-wall"
