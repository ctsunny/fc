pluginManagement {
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.android.application" ->
                    useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
    repositories {
        // 国内网络不稳定时优先尝试阿里云/腾讯镜像；如镜像异常，可临时注释后回退官方仓库。
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        maven("https://mirrors.cloud.tencent.com/nexus/repository/google/")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内网络不稳定时优先尝试阿里云/腾讯镜像；如镜像异常，可临时注释后回退官方仓库。
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        maven("https://mirrors.cloud.tencent.com/nexus/repository/google/")
        google()
        mavenCentral()
    }
}

rootProject.name = "fc"
include(":app")
