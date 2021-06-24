#!/bin/bash

set -e

trap clean exit

function clean() {
  rm -f $TMP_FILE
}

base=$(realpath -e $(dirname $BASH_SOURCE)/..)

P12_KEYSTORE=${1:-${base}/cert/datagrid.pfx}
P12_KEY_ALIAS=${KEY_ALIAS:-datagrid}
TNS=${TARGET_NS:-example-datagrid-instance}
DATAGRID_NAME=${DATAGRID_NAME:-datagrid-example}
P12_KEYSTORE_PASSWORD=${KEYSTORE_PASSWORD:-changeme}

oc -n $TNS create secret generic ${DATAGRID_NAME}-tls -o yaml --dry-run \
	--from-literal=alias=${P12_KEY_ALIAS} \
	--from-literal=password=${P12_KEYSTORE_PASSWORD} \
	--from-file=keystore.p12=$P12_KEYSTORE

cat << EOF
#
# to apply manifest run with:
# $BASH_SOURCE | oc apply -f -
#
EOF

