#!/bin/bash
set -e

echo "Installing SDKMAN!"
curl -s "https://get.sdkman.io" | bash
echo sdkman_auto_answer=true > $HOME/.sdkman/etc/config
source "/root/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.11-jbr

echo "Installing Utils"
cd /mnt/jetbrains/work/azure-tools-for-intellij
./mvnw clean install -DskipTests -f Utils/pom.xml

echo "Building plugin"
cd /mnt/jetbrains/work/azure-tools-for-intellij/PluginsAndFeatures/azure-toolkit-for-rider
./gradlew buildPlugin