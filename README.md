# IrisDataGen

A data generator extracting block shapes and entity dimensions into JSON files for [Iris](https://github.com/Aeldrion/Iris) using data from the Minecraft client.

## Requirements

- JDK 25
- A working Minecraft directory containing a jar for the target version client (you have this if you have already played this version)

## Usage

To extract data for a given Minecraft version, run `./gradlew run -PmcVersion=<version name>`, e.g.
```sh
./gradlew run          # Defaults to 26.1
./gradlew run -PmcVersion=26.1
./gradlew run -PmcVersion=26.2-snapshot-3
```
or on Windows:
```batch
gradlew.bat run -PmcVersion=26.2-snapshot-3
```

If you need to specify another Minecraft directory, you can use the `-PmcDir` option:
```sh
./gradlew run -PmcDir=/home/ael/Games/Minecraft
```
```batch
gradlew.bat run -PmcDir=C:\Users\Ael\Local\Roaming\.minecraft
```

Two JSON files, `blocks.json` and `entities.json`, are generated in `app/generated`.

> [!WARNING]
> IrisDataGen only functions on the unobfuscated Minecraft client and as such is incompatible with versions prior to 26.1-snapshot-1. IrisDataGen also depends on methods from the Minecraft jar, which may be changed in any snapshot.
