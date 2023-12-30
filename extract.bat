@cls
@title Extract
@echo off

:: Init
set version=""
set decompile=""

:: Parse arguments
:args_loop
if not "%1"=="" (
    if "%1"=="--decompile" (
    	if /i "%2"=="cfr" goto :args_else
        if /i "%2"=="vineflower" goto :args_else
        set decompile="cfr"
        goto :args_end
        :args_else
        set decompile=%2
        shift
        goto :args_end
    )
    set tmp=%1
    if %tmp:~0,1%==- (
        echo "[-] Unknown option: %1"
        goto :omega
    )
    set version=%1
    :args_end
    shift
    goto :args_loop
)

:: DEBUG
echo Decompile = %decompile%
echo Version = %version%

:: Check the given argument
if %version%=="" (
    echo "[-] Missing Minecraft version argument, usage: extract.bat <version number>"
    goto :omega
)

:: Download the requested jar
echo "[+] Downloading official obfuscated jar client archive and mappings"
mkdir downloaded 
mkdir downloaded/%version%
mkdir build/%version%
if exist downloaded/%version%/client.jar goto :jar_dl_else
if exist downloaded/%version%\client.txt goto :jar_dl_else
python3 download_mc_jar.py %version%
goto :jar_dl_end
:jar_dl_else
    echo "[~] Files already exist, skipping"
:jar_dl_end

:: Download dependencies
echo "[+] Downloading Java decompiling dependencies"
:: Ensure the destination directory exists
if exist dependencies goto :deps_dl_end
mkdir dependencies
for /f "delims=*" %%A in (dependencies_links.txt) do (
    curl -L -o "dependencies\%%~nxA" "%%A"
)
:deps_dl_end

:: Run deobfuscator
echo "[+] Deobfuscating the jar archive for version %version%"
if exist build\%version%\client_%version%_deobf.jar goto :deobf_else
cd dependencies
:: Reconstruct ProGuard Deobfuscator
java -Xmx2G -cp ".;*" -jar reconstruct-cli-1.3.25.jar -jar ..\downloaded\%version%\client.jar -mapping ..\downloaded\%version%\client.txt -output ..\build\%version%\client_%version%_deobf.jar -agree
cd ..
goto :deobf_end
:deobf_else
echo "[~] Files already exist, skipping"
:deobf_end

:: Run the decompiler
echo "[+] Decompiling the jar archive for version %version%"
if %decompile%=="" goto :decomp_else
mkdir %version%_sources
if /i %decompile%==vineflower goto :decomp_vineflower
if /i %decompile%==cfr goto :decomp_cfr
echo "[-] Unknown decompiler"
goto :decomp_end
:decomp_vineflower
java -Xmx2G -jar dependencies\vineflower-1.9.3.jar build/%version%/client_%version%_deobf.jar --rer=0 --folder %version%_sources
goto :decomp_end
:decomp_cfr
java -Xmx2G -jar dependencies\cfr-0.152.jar build/%version%/client_%version%_deobf.jar --outputdir %version%_sources
goto :decomp_end
:decomp_else
echo "[~] Decompiling disabled"
:decomp_end

:: Extract
echo "[+] Compiling the extractor"
javac -classpath "build\%version%\*" -sourcepath . -d "build\%version%" src\Extract.java
echo "[+] Running the extractor"
cd build\%version%
java -Xmx2G -classpath ".;*" src.Extract %version%
cd ../..

:: Don't close the cmd prompt
:omega
cmd /k
