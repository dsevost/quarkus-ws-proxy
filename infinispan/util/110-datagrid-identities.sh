#!/bin/bash

set -e

trap clean exit

function clean() {
  rm -f $TMP_FILE
}

base=$(realpath -e $(dirname $BASH_SOURCE)/..)

MANIFEST=${1:-${base}/manifest/identities.yaml}
TNS=${TARGET_NS:-example-datagrid-instance}
DATAGRID_NAME=${DATAGRID_NAME:-datagrid-example}
USER_NAME=${USER_NAME:-developer}
USER_PASSWORD=${USER_PASSWORD:-$(openssl rand -base64 10)}

TMP_FILE=$(mktemp)

sed "s/- username:.*/- username: $USER_NAME/ ; s,  password:.*,  password: $USER_PASSWORD, ; /^#.*$/d" $MANIFEST > $TMP_FILE
cat $TMP_FILE
echo "###"

oc -n $TNS create secret generic ${DATAGRID_NAME}-identities -o yaml --dry-run \
	--from-file=identities.yaml=$TMP_FILE

cat << EOF
#
# to apply manifest run with:
# $BASH_SOURCE | oc apply -f -
#
EOF

