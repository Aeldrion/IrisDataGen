#!/bin/sh

# Init
version=""
decompile=""

# Parse arguments
while [ "$#" -gt 0 ]; do
  case "$1" in
    --decompile=*) decompile="${1#*=}"; shift 1;;
    --decompile) decompile="cfr"; shift 1;;

    -*) echo "[-] Unknown option: $1" >&2; exit 1;;
    *) version="$1"; shift 1;;
  esac
done

# Check the given argument
if [ "$version" = "" ]; then
    echo "[-] Missing Minecraft version argument, usage: ./extract.sh <version number>"
    exit 1
fi

# Download the requested jar
echo "[+] Downloading official obfuscated jar client archive and mappings"
mkdir -pv "$version"_downloaded
mkdir -pv build/"$version"
if [ ! -f "$version"_downloaded/client.jar ] || [ ! -f "$version"_downloaded/client.txt ]; then
    python3 download_mc_jar.py "$version"
else
    echo "[~] Files already exist, skipping"
fi

# Download dependencies
echo "[+] Downloading Java decompiling dependencies"
mkdir -pv dependencies
wget --input-file=./dependencies_links.txt --directory-prefix=./dependencies/ --quiet --show-progress -nc

# Run deobfuscator
echo "[+] Deobfuscating the jar archive for version $version"
if [ ! -f build/"$version"/client_"$version"_deobf.jar ]; then
    cd dependencies
    # Reconstruct ProGuard Deobfuscator
    java -Xmx2G -cp ".:*" -jar reconstruct-cli-1.3.25.jar -jar ../"$version"_downloaded/client.jar -mapping ../"$version"_downloaded/client.txt \
        -output ../build/"$version"/client_"$version"_deobf.jar -agree # -exclude <Excluded Packages>
    cd ..
else
    echo "[~] Files already exist, skipping"
fi

# Run the decompiler
echo "[+] Decompiling the jar archive for version $version"
if [ ! -d "$version"_sources/ ]; then
    if [ "$decompile" = "" ]; then # Check if the decompile flag is set
        echo "[~] Decompiling disabled"
    else
        mkdir -pv "$version"_sources
        decompile=$(echo "$decompile" | awk '{print tolower($0)}') # Make lowercase
        if [ "$decompile" = "vineflower" ]; then # Vineflower
            java -Xmx2G -jar dependencies/vineflower-1.9.3.jar build/"$version"/client_"$version"_deobf.jar \
                --rer=0 \
                --folder "$version"_sources
        elif [ "$decompile" = "cfr" ]; then # CFR
            java -Xmx2G -jar dependencies/cfr-0.152.jar build/$version/client_"$version"_deobf.jar \
                --outputdir "$version"_sources
        else
            echo "[-] Unknown decompiler"
        fi
    fi
else
    echo "[~] Files already exist, skipping"
fi

# Extract
echo "[+] Compiling the extractor"
javac -classpath "build/$version/*" \
    -sourcepath . -d "build/$version" src/Extract.java
echo "[+] Running the extractor"
cd "build/$version"
java -Xmx2G -classpath ".:*" \
    src.Extract "$version"
cd ../..
