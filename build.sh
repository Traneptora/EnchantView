#!/bin/sh

VERS=5.0.0
MC_VERS=1.8
BASENAME=enchantview
LONGNAME=EnchantView
SHORTNAME=EV

cd $(dirname $0)
ARCH=$LONGNAME-v$VERS-mc$MC_VERS.jar
mv src/main/java/thebombzen/mods/enchantview/bukkit ./bukkit
./gradlew build
FAIL=$?
mv ./bukkit src/main/java/thebombzen/mods/enchantview/bukkit
if [ $FAIL -ne 0 ]; then exit $FAIL; fi

cp build/libs/$BASENAME-$VERS.jar $ARCH
mkdir -p META-INF

cat >META-INF/MANIFEST.MF <<EOF
Manifest-Version: 1.0
EOF

echo >>META-INF/MANIFEST.MF "Main-Class: thebombzen.mods.$BASENAME.installer.${SHORTNAME}InstallerFrame"

zip -u $ARCH META-INF/MANIFEST.MF
zip -d $ARCH "thebombzen/mods/thebombzenapi*"


