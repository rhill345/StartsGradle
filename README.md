# STARTS (*STA*tic *R*egression *T*est *S*election) Overview


This is a gradle port of the maven STARTS tool.  STARTS is a tool for static class-level regression test selection tool
for Gradle-based Java and Android programs.

## Prerequisites

1. Java 1.8 and above
2. Operating System: Linux or OSX

## Integrating STARTS Plugin

Change the gradle.settings to add the configuration for the STARTS plugin:

```
{
    buildscript {
      repositories {
        mavenLocal()
      }
      dependencies {
        classpath "edu.illinois:starts-maven-plugin:1.4-SNAPSHOT"
      }
    }
}
```

## Building STARTS from source

1. `git clone https://github.com/rhill345/StartsGradle`
2. `cd starts`
3. `gradle install`

## Using the STARTS Gradle Plugin

### Available Options

1. To see all the goals that STARTS provides, run `gradlew tasks --all`

### Major Functionality

1. To see the **types** that changed since the last time STARTS was run:
`gradlew startsDiff`

2. To see the **types** that may be impacted by changes since the last
time STARTS was run: `gradlew startsImpacted`

3. To see the **tests** that are affected by the most recent changes:
`gradlew startsSelect`

4. To perform RTS using STARTS (i.e., select tests and run the
selected tests): `gradlew starts`

5. To remove all artifacts that STARTS stores between versions
(i.e. in the `.starts` directories): `gradlew startsClean`

__NOTE:__ By default, commands (1) - (3) *will not* update the
checksums of files in the latest version, while the command in (4)
*will* update the checksums of the files. Each command has a
`update**Checksums` option that can be used to change the default
behavior. For example, to update the checksums while checking the
diff, add the following to gradle.build and re-run

```

apply plugin: 'edu.illinois.starts'

startsSettings {
    updateDiffChecksum = true
}
```
