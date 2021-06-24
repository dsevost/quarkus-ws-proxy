#!/bin/bash

set -e

base=$(realpath -e $(dirname $BASH_SOURCE)/..)

MANIFEST=${1:-${base}/manifest/operator-subscription.yaml}
TNS=${TARGET_NS:-example-datagrid-instance}

sed "s/: example-datagrid-instance/: $TNS/" $MANIFEST

cat << EOF
#
# to apply manifest run with:
# $BASH_SOURCE | oc apply -f -
#
EOF
