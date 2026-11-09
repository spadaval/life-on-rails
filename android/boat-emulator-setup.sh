#!/bin/bash
# One-time setup, after boat-setup.sh, of the boat VM lor-android-build as a device host for
# T3 Code: Tailscale, the Android emulator and LoR's two test AVDs (T3_Phone_API35,
# T3_Fold_API35), laid out where T3 Code's SSH device host looks for them. Safe to run again.
# Run on the VM: `boat scp android/boat-emulator-setup.sh <id>:/home/user/`, then
# `boat exec <id> --detach -- 'bash ~/boat-emulator-setup.sh > ~/emulator-setup.log 2>&1'`.
# Joining the tailnet is a separate step, with a key minted on buildbot (see lor-57bm's Report).
set -euo pipefail
source ~/lor-env.sh

# Tailscale from its own apt repository; tailscaled starts at every boot, so the VM rejoins
# after a resume (/var/lib/tailscale, which holds the node's identity, is in boat's snapshots).
if ! command -v tailscale >/dev/null; then
  . /etc/os-release
  curl -fsSL "https://pkgs.tailscale.com/stable/ubuntu/$VERSION_CODENAME.noarmor.gpg" \
    | sudo tee /usr/share/keyrings/tailscale-archive-keyring.gpg >/dev/null
  curl -fsSL "https://pkgs.tailscale.com/stable/ubuntu/$VERSION_CODENAME.tailscale-keyring.list" \
    | sudo tee /etc/apt/sources.list.d/tailscale.list >/dev/null
  sudo apt-get update -q && sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -q tailscale
fi
sudo systemctl enable --now tailscaled

# The emulator and the one system image both AVDs use.
IMG="system-images;android-35;google_apis;x86_64"
set +o pipefail
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" emulator "$IMG" >/dev/null
set -o pipefail

# T3 Code's device host script uses $ANDROID_HOME when set and ~/Android/Sdk otherwise, and runs
# over non-interactive ssh, which reads neither ~/lor-env.sh nor nvm. Give it the SDK, Node,
# and the java that avdmanager needs.
mkdir -p ~/Android ~/.local/bin
ln -sfn "$ANDROID_HOME" ~/Android/Sdk
for b in node npm npx; do ln -sfn "$(command -v $b)" ~/.local/bin/$b; done
# Ubuntu's Java 11 in /usr/bin comes first on that PATH; avdmanager needs 17 or later. Bash reads
# ~/.bashrc for an ssh command, and lines above its interactive-only guard apply there too.
rm -f ~/.local/bin/java
grep -q '^# LOR JAVA START' ~/.bashrc || sed -i '1i # LOR JAVA START\nexport JAVA_HOME=$HOME/opt/jdk21 PATH=$HOME/opt/jdk21/bin:$PATH\n# LOR JAVA END' ~/.bashrc

# The AVDs, made like buildbot's: same names, device profiles, image, RAM and data size.
avd() { # name device ram data
  local dir=~/.android/avd/$1.avd
  [ -d "$dir" ] || echo no | "$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager" create avd -n "$1" -k "$IMG" -d "$2" >/dev/null
  sed -i -E '/^(hw\.ramSize|disk\.dataPartition\.size|hw\.gpu\.(enabled|mode)|hw\.cpu\.ncore|PlayStore\.enabled)[ =]/d' "$dir/config.ini"
  printf 'hw.ramSize=%s\ndisk.dataPartition.size=%s\nhw.gpu.enabled=yes\nhw.gpu.mode=swiftshader_indirect\nhw.cpu.ncore=4\nPlayStore.enabled=no\n' "$3" "$4" >> "$dir/config.ini"
}
avd T3_Phone_API35 pixel_6 2048 6G
avd T3_Fold_API35 pixel_9_pro_fold 2048 10G
"$ANDROID_HOME/emulator/emulator" -list-avds
echo EMULATOR_SETUP_DONE
