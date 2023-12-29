# IrisDataGen

A version-independent data generator for [Iris](https://github.com/Aeldrion/Iris). Downloads Minecraft jar files and extracts block shapes and entity dimensions into JSON files.

## Dependencies

**Not automatically installed**:
- python3 (requests)
- wget (Linux only)
- java and javac (JDK)

**Automatically installed**:
- [Reconstruct](https://github.com/LXGaming/Reconstruct) ProGuard deobfuscator
- [CFR](https://github.com/leibnitz27/cfr) Java decompiler
- [Vineflower](https://github.com/Vineflower/vineflower) Java decompiler

## Installing

### Windows

Install [Java 17 JDK](https://download.oracle.com/java/17/latest/jdk-17_windows-x64_bin.exe), [Python3](https://www.python.org/downloads/windows/) (or from the Windows Store) and the Python [requests](https://requests.readthedocs.io/en/latest/user/install/#install) module from the websites given in the README.

Update your PATH for Java and Python3 if it has not already been done before.

### Linux

#### Debian/Ubuntu

```
sudo apt install openjdk-17-jdk python3-requests wget
```

#### Fedora

```
sudo dnf install java-17-openjdk-devel python3-requests wget
```

#### Archlinux

```
sudo pacman -S jdk17-openjdk python-requests wget
```

### OSX

**NOT TESTED**

Install the JDK for Java, `wget` and Python3 and the module `requests` using `brew` or something else.


## Usage

To extract data for a given Minecraft version, run `./extract.sh <version number>`, e.g.
```sh
./extract.sh 1.20.4
```
or on Windows:
```batch
extract.bat 1.20.4
```

To clean everything up, run `./clean.sh`

For example:
```
./clean.sh
./extract 1.20.4
```

### Options

- `--decompile [cfr|vineflower]`: enable decompilation of the deobfuscated Minecraft jar file, optionally add a specific decompiler (default is CFR)
