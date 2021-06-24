#!/bin/bash

set -e

base=$(realpath -e $(dirname $BASH_SOURCE)/..)

MANIFEST=${1:-${base}/manifest/operator-group-example.yaml}
NS=${NS:-example-datagrid-operator}
TNS=${TARGET_NS:-example-datagrid-instance}

sed "s/: example-datagrid-operator/: $NS/ ; s/- example-datagrid-instance/- $TNS/" $MANIFEST

cat << EOF
#
# to apply manifest run with (permissions reured):
# $BASH_SOURCE | oc apply -f -
#
EOF
