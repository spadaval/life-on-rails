#!/bin/bash
# One-time setup of a boat VM for android/boat-build.sh: JDK 21 and the SDK pieces the app needs,
# in ~/opt, and ~/lor-env.sh. Run on the VM: `boat scp android/boat-setup.sh <id>:/home/user/`,
# then `boat exec <id> --detach -- 'bash ~/boat-setup.sh > ~/setup.log 2>&1'`.
# Afterwards copy buildbot's debug key so the VM's APKs update the installed app:
# `boat scp ~/.android/debug.keystore <id>:/home/user/.android/debug.keystore`.
set -euo pipefail
mkdir -p ~/opt ~/.android && cd ~/opt
curl -fsSL -o jdk.tgz "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse"
mkdir -p jdk21 && tar xzf jdk.tgz -C jdk21 --strip-components=1 && rm jdk.tgz
ZIP=$(curl -fsSL https://dl.google.com/android/repository/repository2-3.xml | grep -o 'commandlinetools-linux-[0-9]*_latest.zip' | sort -u | sort -t- -k3 -n | tail -1)
curl -fsSL -o clt.zip "https://dl.google.com/android/repository/$ZIP"
mkdir -p sdk/cmdline-tools && unzip -q clt.zip -d sdk/cmdline-tools && mv sdk/cmdline-tools/cmdline-tools sdk/cmdline-tools/latest && rm clt.zip
export JAVA_HOME=~/opt/jdk21 ANDROID_HOME=~/opt/sdk
set +o pipefail
yes | sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-37.0" "build-tools;37.0.0" "platform-tools" >/dev/null
printf 'export JAVA_HOME=$HOME/opt/jdk21 ANDROID_HOME=$HOME/opt/sdk PATH=$HOME/opt/jdk21/bin:$PATH\n' > ~/lor-env.sh
echo SETUP_DONE
