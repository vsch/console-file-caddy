#!/usr/bin/env bash
HOME_DIR="/Users/vlad/src/projects/console-file-caddy"
PLUGIN="console-file-caddy"
PLUGIN_JAR=
OLD_PLUGIN=
IDE_VERSION=
SANDBOX_NAME=

cd "${HOME_DIR}" || exit

../update-plugin.sh "${HOME_DIR}" "${PLUGIN}" "${PLUGIN_JAR}" "${OLD_PLUGIN}" "${IDE_VERSION}" "${SANDBOX_NAME}"

