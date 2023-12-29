from sys import argv, stdout
import requests

# Configuration
version_manifest_url = "http://piston-meta.mojang.com/mc/game/version_manifest.json"


# Util
def get_version_manifest_JSON():
    resp = requests.get(version_manifest_url)
    if resp.status_code == 200:
        return resp.json()
    else:
        print(f"[-] Error while fetching the version manifest file from: {version_manifest_url}")
        exit(-1)


def get_version_url_from_manifest(manifest_json_file, mc_version):
    for version in manifest_json_file['versions']:
        if version['id'] == mc_version:
            return version['url']
    return ""


def get_version_json(version_url):
    resp = requests.get(version_url)
    if resp.status_code == 200:
        return resp.json()
    else:
        print(f"[-] Error while fetching the client jar and mappings from: {version_url}")
        exit(-1)


def get_client_download_links_from_version(version_json):
    return (version_json['downloads']['client']['url'], version_json['downloads']['client_mappings']['url'])


def download_file(file_url, folder_prefix):
    resp:requests.Response = requests.get(file_url, stream=True)
    if resp.status_code != 200:
        print(f"[-] Error while downloading the file from: {file_url}")
        exit(-1)
    bar_length = 20
    block_size = 1024
    file_size = int(resp.headers.get('Content-Length', 0))
    try:
        with open(folder_prefix+file_url.split('/')[-1], "wb") as f:
            for i, chunk in enumerate(resp.iter_content(chunk_size=block_size)):
                f.write(chunk)
                if file_size > 0:
                    ratio = (i*block_size+1)/file_size
                    stdout.write('\r')
                    stdout.write("Downloading: [{:{}}] {:>3}%"
                             .format('='*int(ratio*bar_length+1),
                                     bar_length, int(ratio*100.0+1)))
                    stdout.flush()
        stdout.write('\r')
        stdout.write("Downloading: [{:{}}] {:>3}%\n"
                        .format('='*bar_length,
                         bar_length, 100))
        stdout.flush()

    except IOError as e:
            print(f"[-] Couldn't write to file ({e})")


def download_libraries(version_json):
    for library in version_json['libraries']:
        lib_url = library['downloads']['artifact']['url']
        print(lib_url.split('/')[-1]+":")
        download_file(lib_url, "build/"+version_json['id']+"/")


## MAIN CODE
if __name__ == '__main__':
    version_manifest_json = get_version_manifest_JSON()

    # Get command line argument
    mc_version = -1
    if len(argv) != 2:
        mc_version = version_manifest_json['latest']['release']
        print(f"[~] Defaulting to the latest Minecraft release {mc_version}")
    else:
        mc_version = argv[1]

    # Get the information file url corresponding to that version
    mc_version_url = get_version_url_from_manifest(version_manifest_json, mc_version)

    if mc_version_url == "":
        print(f"[-] Unable to find the requested {mc_version} version...")
        exit(-1)
    print(f"[+] Fetching the requested {mc_version} version from {mc_version_url}")

    # Get the JSON file and download all the needed libraries and Minecraft sources
    mc_version_json = get_version_json(mc_version_url)
    client_jar_url, client_mappings_url = get_client_download_links_from_version(mc_version_json)
    print(f"[+] Downloading client.jar for version {mc_version}")
    download_file(client_jar_url, f"./{mc_version}_downloaded/")
    print(f"[+] Downloading client.txt mappings for version {mc_version}")
    download_file(client_mappings_url, f"./{mc_version}_downloaded/")
    print(f"[+] Downloading Minecraft Java libraries for version {mc_version}")
    download_libraries(mc_version_json)
