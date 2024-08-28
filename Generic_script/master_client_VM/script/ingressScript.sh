#! /bin/bash

set -eu

Namespace=${1}
cd /home/eccd/ingress_certificate_tls_enm_ui_ca/

bash generate_certificate.sh --cenm-name-space=${Namespace}


