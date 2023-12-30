from sys import argv, stdout
import requests

# Configuration
version_manifest_url = "http://piston-meta.mojang.com/mc/game/version_manifest.json"

# Util
def get_version_manifest():
    resp = requests.get(version_manifest_url)
    if resp.status_code == 200:
        return resp.json()
    else:
        print(f"[-] Error while fetching version manifest from {version_manifest_url}")
        exit(-1)

def get_version_json(url):
    resp = requests.get(url)
    if resp.status_code == 200:
        return resp.json()
    else:
        print(f"[-] Error while fetching client jar and mappings from {url}")
        exit(-1)

def get_client_download_urls(version_json):
    return (version_json['downloads']['client']['url'], version_json['downloads']['client_mappings']['url'])

def download_file(url, dir):
    resp = requests.get(url, stream=True)
    if resp.status_code != 200:
        print(f"[-] Error while downloading file from {url}")
        exit(-1)
    
    PROGRESS_BAR_LENGTH = 20
    BLOCK_SIZE = 1024

    file_size = int(resp.headers.get('Content-Length', 0))
    try:
        with open(dir + '/' + url.split('/')[-1], "wb") as f:
            for i, chunk in enumerate(resp.iter_content(chunk_size=BLOCK_SIZE)):
                f.write(chunk)
                ratio = (min(file_size, (i+1)*BLOCK_SIZE) / file_size) if file_size > 0 else 0
                stdout.write('\r')
                stdout.write("Downloading... [{:{}}] {:>3}%".format(
                    '=' * int(ratio*PROGRESS_BAR_LENGTH),
                    PROGRESS_BAR_LENGTH, int(ratio*100 + 1))
                )
                stdout.flush()
        
        # Clear progress bar
        stdout.write('\033[2K\033[1G')
        stdout.flush()
    except IOError as e:
        print(f"[-] Could not write to file ({e})")

def download_libraries(version_json):
    library_count = len(version_json['libraries'])
    for i, library in enumerate(version_json['libraries']):
        url = library['downloads']['artifact']['url']
        print(f"({i+1}/{library_count}) {url.split('/')[-1]}")
        download_file(url, "build/"+version_json['id']+"/")

if __name__ == '__main__':
    # Get the version manifest
    version_manifest = get_version_manifest()

    # Get command line arguments
    game_version = -1
    if len(argv) != 2:
        game_version = version_manifest['latest']['release']
        print(f"[~] Defaulting to the latest Minecraft release ({game_version})")
    else:
        game_version = argv[1]

    # Get the information file url corresponding to the requested version
    for version in version_manifest['versions']:
        if version['id'] == game_version:
            game_version_url = version['url']
            break
    else:
        print(f"[-] Unable to find version {game_version}")
        exit(-1)
    print(f"[+] Fetching version {game_version} from {game_version_url}")

    # Get the JSON file and download all the needed libraries and Minecraft sources
    mc_version_json = get_version_json(game_version_url)
    client_jar_url, client_mappings_url = get_client_download_urls(mc_version_json)
    
    print(f"[+] Downloading client.jar for version {game_version}")
    download_file(client_jar_url, f"./downloaded/{game_version}/")
    print(f"[+] Downloading client.txt mappings for version {game_version}")
    download_file(client_mappings_url, f"./downloaded/{game_version}/")
    print(f"[+] Downloading Minecraft Java libraries for version {game_version}")
    download_libraries(mc_version_json)
