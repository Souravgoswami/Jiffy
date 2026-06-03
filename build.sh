#!/usr/bin/env bash
set -euo pipefail

export JIFFY_KEYSTORE="$PWD/jiffy-release.jks"
export JIFFY_KEY_ALIAS="jiffy"

read -srp "Keystore password: " JIFFY_STORE_PASSWORD
echo
read -srp "Key password: " JIFFY_KEY_PASSWORD
echo

export JIFFY_STORE_PASSWORD
export JIFFY_KEY_PASSWORD

gradle --no-daemon clean assembleRelease
