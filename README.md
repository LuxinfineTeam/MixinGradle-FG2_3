# MixinGradle 0.6 extended
Это форк проекта https://github.com/SpongePowered/MixinGradle (ветка 0.6)
Плагин расчитан на использование в связке с https://github.com/LuxinfineTeam/ForgeGradle-2_3

### Список изменений:
- Поддержка запуска gradle на JDK 17-21
- Подгрузка main/resources в класспаз runClient и runServer конфигураций, чтобы миксины загружали mixins.modid.json файлы в IDE
- Обновление зависимости org.ow2.asm:asm-debug-all:5.0.3 до org.ow2.asm:asm:9.10.1, отказ от tree api, использование ASM9 API вместо ASM5

### Пример подключения плагина совместно с FG 2.3:
```groovy
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
        maven {
            name 'forge'
            url 'https://maven.minecraftforge.net'
        }
    }
    dependencies {
        classpath('com.github.LuxinfineTeam:ForgeGradle-2_3:main-SNAPSHOT') {
            changing = true
        }
        classpath('com.github.LuxinfineTeam:MixinGradle-FG2_3:main-SNAPSHOT')
    }
}

apply plugin: 'net.minecraftforge.gradle.forge'
apply plugin: 'org.spongepowered.mixin'

[compileJava, compileTestJava]*.options*.encoding = 'UTF-8'
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

version = "1.0"
base {
    archivesName = "MyModName"
}

repositories {
    maven {
        name = 'sponge-repo'
        url = 'https://repo.spongepowered.org/repository/maven-public/'
    }
    mavenCentral()
}

minecraft {
    version = "1.12.2-14.23.5.2859"
    runDir = "run"
    mappings = "stable_39"
    makeObfSourceJar = false
    replace '@VERSION@', version
    
    clientRunArgs = ['--tweakClass', 'org.spongepowered.asm.launch.MixinTweaker']
}

dependencies {
    implementation 'org.spongepowered:mixin:0.8.7'
    annotationProcessor 'org.spongepowered:mixin:0.8.7:processor'
}

mixin {
    add sourceSets.main, "mixins.modid.refmap.json"
}
```

### Требования
- Gradle 7.0+
- JDK 17-21 для запуска gradle
