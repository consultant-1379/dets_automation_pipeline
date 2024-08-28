#! /bin/bash
set -eu
#condition check

if [ $# -lt 1 ]; then
  echo 1>&2 "Usage: $0 <DC>p<number>"
  exit 3
fi

NS=${1}cenm
clusterID=${1}
serverurl=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${NAME} | cut -d "," -f4)
clusterID=$(cat /home/cenmbuild/AUTO/script/bin/inventory.csv | grep ${NAME} | cut -d "," -f5)
sacheck=$(kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${NAME} get sa -n ${NS}| grep custom-enduser-exec-get| awk '{print $1}')

echo " = kubectl command check = "
kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${1} get ns
echo ""

if [ -z ${sacheck} ]
then
  echo "create sa on target cluster"
  kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${1} create sa custom-enduser-exec-get -n ${NS}
else
  echo "sa already present with the same name"
fi

sceret=$(kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${NAME} get secrets -n ${NS} | grep custom-enduser-exec-get | awk '{print $1}')
echo $sceret

echo "get token value"

token=$(kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${NAME} get secrets $sceret -n ${NS} -o yaml| grep token: | awk '{print $2}')
certificate=$(kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${NAME} get secrets $sceret -n ${NS} -o yaml| grep ca.crt: | awk '{print $2}')

dtoken=$(echo -n $token | base64 -d)
echo "set the use config"

kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${1} config set-credentials custom-enduser-exec-get --token=${dtoken}


cat <<EOF > /home/cenmbuild/AUTO/storeFile/RBAC/${NAME}_custom_kubeconfigFile
apiVersion: v1
kind: Config
clusters:
- cluster:
    certificate-authority-data: ${certificate}
    server: https://${serverurl}
  name: ${clusterID}
contexts:
- context:
    cluster: ${clusterID}
    namespace: ${NS}
    user: custom-enduser-exec-get
  name: custom-enduser-exec-get
current-context: custom-enduser-exec-get
users:
- name: custom-enduser-exec-get
  user:
    token: ${dtoken}

EOF

cat <<EOF > /home/cenmbuild/AUTO/storeFile/RBAC/${NAME}_custom_role
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  namespace: ${NS}
  name: custom-enduser-exec-get-role
rules:
- apiGroups: [""]
  resources: ["pods"]
  verbs: ["exec", "get", "list"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: custom-enduser-exec-get-rb
  namespace: ${NS}
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: custom-enduser-exec-get-role
subjects:
- namespace: ${NS}
  kind: ServiceAccount
  name: custom-enduser-exec-get
EOF

echo "role and rolebinding creation"

rbCheck=$(kubectl get rolebinding -n ${NAME} | grep custom-enduser-exec-get|awk '{print $1}')

if [ -z ${rbCheck} ]
then
  kubectl --kubeconfig=/home/cenmbuild/AUTO/kubeConfigFile/${NAME} apply -f /home/cenmbuild/AUTO/storeFile/RBAC/${NAME}_custom_role
else
  echo "role already present"
fi

echo "                               ==========checks========="


echo "                               ===============should work"
kubectl --kubeconfig=/home/cenmbuild/AUTO/storeFile/RBAC/${NAME}_custom_kubeconfigFile get pod

echo "                               ================should not work"
kubectl --kubeconfig=/home/cenmbuild/AUTO/storeFile/RBAC/${NAME}_custom_kubeconfigFile get configmap

