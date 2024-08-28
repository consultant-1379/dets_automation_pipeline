#! /bin/bash
set -eu

## Variables

version=${1}
iccr_IP=${2}
evnfm=${3}
iam=${4}
registry=${5}
helm=${6}
gas=${7}
k8password=${8}
k8username=${9}
eoingressip=${10}
deployment_type=${11}
brovolume=20Gi
rbacpassword=DefaultP12345!
k8registry=$(kubectl get ingress -n kube-system |grep eric-lcm-container-registry-ingress | awk '{print $3}')

#### Edit site values file ####
if [ ${deployment_type} == "install" ]
then
  cd ~/evnfmdeploy
else
  cd ~/evnfmupgrade
fi
echo "##--------------------------- site value preparation - version | ${version} ---------------------------##"
echo ""
touch command.py
chmod 777 command.py
echo "Edit Site values file"
sitefile=`ls ${PWD} | grep site | grep ${version}`
echo "Edit ${sitefile} using python"
editfile=${sitefile}
echo "python3 -c 'import yaml;f=open(\"${sitefile}\");y=yaml.safe_load(f);y[\"global\"][\"hosts\"][\"iam\"] = \"${iam}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file global:host:iam"
./command.py > newfile.yamls

echo "python3 -c 'import yaml;f=open(\"newfile.yamls\");y=yaml.safe_load(f);y[\"global\"][\"hosts\"][\"vnfm\"] = \"${evnfm}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file global:host:vnfm"
./command.py > newfile1.yamls
rm -rf newfile.yamls

echo "python3 -c 'import yaml;f=open(\"newfile1.yamls\");y=yaml.safe_load(f);y[\"global\"][\"hosts\"][\"gas\"] = \"${gas}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file global:host:gas"
./command.py > newfile2.yamls
rm -rf newfile1.yamls

echo "python3 -c 'import yaml;f=open(\"newfile2.yamls\");y=yaml.safe_load(f);y[\"global\"][\"registry\"][\"url\"] = \"${k8registry}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file global:registry:url"
./command.py > newfile3.yamls
rm -rf newfile2.yamls

echo "python3 -c 'import yaml;f=open(\"newfile3.yamls\");y=yaml.safe_load(f);y[\"global\"][\"registry\"][\"username\"] = \"${k8username}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file global:registry:username"
./command.py > newfile4.yamls
rm -rf newfile3.yamls

echo "python3 -c 'import yaml;f=open(\"newfile4.yamls\");y=yaml.safe_load(f);y[\"global\"][\"registry\"][\"password\"] = \"${k8password}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file global:registry:password"
./command.py > newfile5.yamls
rm -rf newfile4.yamls

echo "python3 -c 'import yaml;f=open(\"newfile5.yamls\");y=yaml.safe_load(f);y[\"tags\"][\"eoEvnfm\"] = \"true\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eoEvnfm:true"
./command.py > newfile6.yamls
rm -rf newfile5.yamls

echo "python3 -c 'import yaml;f=open(\"newfile6.yamls\");y=yaml.safe_load(f);y[\"eric-sec-access-mgmt\"][\"ingress\"][\"hostname\"] = \"${iam}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-sec-access-mgmt:ingress:hostname"
./command.py > newfile7.yamls
rm -rf newfile6.yamls

echo "python3 -c 'import yaml;f=open(\"newfile7.yamls\");y=yaml.safe_load(f);y[\"eric-eo-evnfm\"][\"eric-lcm-container-registry\"][\"ingress\"][\"hostname\"] = \"${registry}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-eo-evnfm:eric-lcm-container-registry:ingress"
./command.py > newfile8.yamls
rm -rf newfile7.yamls

echo "python3 -c 'import yaml;f=open(\"newfile8.yamls\");y=yaml.safe_load(f);y[\"eric-eo-evnfm\"][\"eric-lcm-helm-chart-registry\"][\"ingress\"][\"hostname\"] = \"${helm}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-eo-evnfm:eric-lcm-helm-chart-registry:ingress:hostname"
./command.py > newfile9.yamls
rm -rf newfile8.yamls

echo "python3 -c 'import yaml;f=open(\"newfile9.yamls\");y=yaml.safe_load(f);y[\"eric-eo-evnfm\"][\"eric-lcm-helm-chart-registry\"][\"env\"][\"secret\"][\"BASIC_AUTH_USER\"] = \"Test\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-eo-evnfm:eric-lcm-helm-chart-registry:env:secret"
./command.py > newfile10.yamls
rm -rf newfile9.yamls

echo "python3 -c 'import yaml;f=open(\"newfile10.yamls\");y=yaml.safe_load(f);y[\"eric-eo-evnfm\"][\"eric-lcm-helm-chart-registry\"][\"env\"][\"secret\"][\"BASIC_AUTH_PASS\"] = \"Test12345\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-eo-evnfm:eric-lcm-helm-chart-registry:env:secret"
./command.py > newfile11.yamls
rm -rf newfile10.yamls

#
echo "python3 -c 'import yaml;f=open(\"newfile11.yamls\");y=yaml.safe_load(f);y[\"eric-eo-evnfm\"][\"eric-lcm-helm-chart-registry\"][\"ingress\"][\"enabled\"] = \"true\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-eo-evnfm:eric-lcm-helm-chart-registry:ingress:enabled"
./command.py > newfile11a.yamls
rm -rf newfile11.yamls


echo "python3 -c 'import yaml;f=open(\"newfile11a.yamls\");y=yaml.safe_load(f);y[\"eric-eo-evnfm\"][\"eric-vnfm-orchestrator-service\"][\"smallstack\"][\"application\"] = \"false\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-eo-evnfm:eric-vnfm-orchestrator-service:smallstack:application"
./command.py > newfile12.yamls
rm -rf newfile11a.yamls
#

echo "python3 -c 'import yaml;f=open(\"newfile12.yamls\");y=yaml.safe_load(f);y[\"eric-eo-evnfm-nbi\"][\"eric-evnfm-rbac\"][\"defaultUser\"][\"username\"] = \"vnfm-user\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-eo-evnfm-nbi:eric-evnfm-rbac:defaultUser:username"
./command.py > newfile13.yamls
rm -rf newfile12.yamls

echo "python3 -c 'import yaml;f=open(\"newfile13.yamls\");y=yaml.safe_load(f);y[\"eric-eo-evnfm-nbi\"][\"eric-evnfm-rbac\"][\"defaultUser\"][\"password\"] = \"${rbacpassword}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-eo-evnfm-nbi:eric-evnfm-rbac:defaultUser:password"
./command.py > newfile14.yamls
rm -rf newfile13.yamls

echo "python3 -c 'import yaml;f=open(\"newfile14.yamls\");y=yaml.safe_load(f);y[\"gas\"][\"defaultUser\"][\"username\"] = \"vnfm-user\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file gas:defaultUser:username:vnfm-user"
./command.py > newfile15.yamls
rm -rf newfile14.yamls

echo "python3 -c 'import yaml;f=open(\"newfile15.yamls\");y=yaml.safe_load(f);y[\"gas\"][\"defaultUser\"][\"password\"] = \"${rbacpassword}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file gas:defaultUser:password:"
./command.py > newfile16.yamls
rm -rf newfile15.yamls

echo "python3 -c 'import yaml;f=open(\"newfile16.yamls\");y=yaml.safe_load(f);y[\"eric-ctrl-bro\"][\"sftp\"][\"username\"] = \"sftp-user\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-ctrl-bro:sftp:unsrname"
./command.py > newfile17.yamls
rm -rf newfile16.yamls

echo "python3 -c 'import yaml;f=open(\"newfile17.yamls\");y=yaml.safe_load(f);y[\"eric-ctrl-bro\"][\"sftp\"][\"password\"] = \"DefaultP12345\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-ctrl-bro:sftp:password"
./command.py > newfile18.yamls
rm -rf newfile17.yamls

echo "python3 -c 'import yaml;f=open(\"newfile18.yamls\");y=yaml.safe_load(f);y[\"system-user\"][\"credentials\"][\"username\"] = \"vnfm-user\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file system-user:credentials:username"
./command.py > newfile19.yamls
rm -rf newfile18.yamls

echo "python3 -c 'import yaml;f=open(\"newfile19.yamls\");y=yaml.safe_load(f);y[\"system-user\"][\"credentials\"][\"password\"] = \"${rbacpassword}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file system-user:credentials:password"
./command.py > newfile20.yamls
rm -rf newfile19.yamls

echo "python3 -c 'import yaml;f=open(\"newfile20.yamls\");y=yaml.safe_load(f);y[\"eric-tm-ingress-controller-cr\"][\"service\"][\"loadBalancerIP\"] = \"${eoingressip}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-tm-ingress-controller-cr:service:loadBalancerIP"
./command.py > newfile21.yamls
rm -rf newfile20.yamls

##add new key values here ##

#echo "python3 -c 'import yaml;f=open(\"newfile21.yamls\");y=yaml.safe_load(f);y[\"eric-tm-ingress-controller-cr\"] = {\"service\": {\"loadBalancerIP\": \"${eoingressip}\", \"externalTrafficPolicy\": \"Local\"}, \"ingressClass\": \"eo_iccr\", \"resources\": {\"envoy\": {\"limits\": {\"memory\": \"2Gi\"}}}}; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

#echo "Modify site values file eric-tm-ingress-controller-cr:service:loadBalancerIP"
#./command.py > newfile22.yamls
#rm -rf newfile21.yamls

##add new key values here ##

echo "python3 -c 'import yaml;f=open(\"newfile21.yamls\");y=yaml.safe_load(f);y[\"eric-tm-ingress-controller-cr\"] = {\"service\": {\"loadBalancerIP\": \"${eoingressip}\", \"externalTrafficPolicy\": \"Local\"}, \"ingressClass\": \"eo_iccr\", \"resources\": {\"envoy\": {\"limits\": {\"memory\": \"2Gi\"}}, \"contour\": {\"limits\": {\"memory\": \"1Gi\"}}}}; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-tm-ingress-controller-cr:service:loadBalancerIP"
./command.py > newfile22.yamls
rm -rf newfile21.yamls

##modify bro volume size##

echo "python3 -c 'import yaml;f=open(\"newfile22.yamls\");y=yaml.safe_load(f);y[\"eric-ctrl-bro\"][\"persistence\"][\"persistentVolumeClaim\"][\"size\"] = \"${brovolume}\"; print(yaml.dump(y, default_flow_style=False, sort_keys=False))'" > command.py

echo "Modify site values file eric-ctrl-bro:persistence:persistentVolumeClaim"
./command.py > newfile23.yamls
rm -rf newfile22.yamls


## End of Key value addition ##

sed -i -e 's/'\'true\''/true/g'  newfile23.yamls
sed -i -e 's/'\'false\''/false/g' newfile23.yamls

cp -f newfile23.yamls ${sitefile}
rm -rf newfile23.yamls

echo "Site values are now prepared"

echo "##++++++++++++++++++++++++++ site value prepation done here ++++++++++++++++++++++++++##"
echo "Value:"
echo ""

cat site_values_${version}.yaml

echo "-----------------------------------------------------------------------------------------"