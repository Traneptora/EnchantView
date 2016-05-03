#!/bin/bash
set -e

function moveback {
	mv -v ./bukkit src/main/java/thebombzen/mods/enchantview/bukkit
}
trap moveback EXIT

VERS=5.0.1
MC_VERS=1.8.9
BASENAME=enchantview
LONGNAME=EnchantView
SHORTNAME=EV

cd $(dirname $0)
ARCH=$LONGNAME-v$VERS-mc$MC_VERS.jar
mv -v src/main/java/thebombzen/mods/enchantview/bukkit ./bukkit
./gradlew build

cp -v build/libs/$BASENAME-$VERS.jar $ARCH
mkdir -v -p META-INF

cat >META-INF/MANIFEST.MF <<EOF
Manifest-Version: 1.0
EOF

echo >>META-INF/MANIFEST.MF "Main-Class: thebombzen.mods.$BASENAME.installer.${SHORTNAME}InstallerFrame"

zip -u $ARCH META-INF/MANIFEST.MF
zip -d $ARCH "thebombzen/mods/thebombzenapi*"
zip -d $ARCH plugin.yml

