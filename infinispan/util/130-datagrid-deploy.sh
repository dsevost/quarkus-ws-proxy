#!/bin/bash

set -e

base=$(realpath -e $(dirname $BASH_SOURCE)/..)

MANIFEST=${1:-${base}/manifest/datagrid-minimal.yaml}
TNS=${TARGET_NS:-example-datagrid-instance}
DATAGRID_EXPOSE=${DATAGRID_EXPOSE:-LoadBalancer}
DATAGRID_EXPOSE_HOST=${DATAGRID_EXPOSE_HOST}
DATAGRID_NAME=${DATAGRID_NAME:-datagrid-example}
DATAGRID_KIND=${DATAGRID_KIND:-Cache}
REPLICAS=${REPLICAS:-1}

function update_route() {
  if [ "$DATAGRID_EXPOSE_HOST" != "" -a "$DATAGRID_EXPOSE" = "Route" ] ; then
	  sed "s/#\W\+host: .*/    host: $DATAGRID_EXPOSE_HOST/ ; s/type: LoadBalancer/type: $DATAGRID_EXPOSE/ ;"
  else 
	  cat
  fi
}

sed " s/: example-datagrid-instance/: $TNS/ ; s/: example-infinispan/: $DATAGRID_NAME/ ; s/replicas: 1/replicas: $REPLICAS/i ; \
      s/type: Cache/type: $DATAGRID_KIND/ ; \
      " $MANIFEST | update_route

#$([ "$DATAGRID_EXPOSE_HOST" != "" -a "$DATAGRID_EXPOSE" = "Route" ] && sed "s/#host: .*/host: $DATAGRID_EXPOSE_HOST/" || cat )

cat << EOF
#
# to apply manifest run with:
# $BASH_SOURCE | oc apply -f -
#
EOF

