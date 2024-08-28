#! /bin/bash

kubectl get node -o wide | awk '{print $1,"","ansible_host="$6}' > inventory || true
sed -i 1d inventory || true
sed -i '1 i [all]' inventory || true
echo '[all:vars]' >> inventory || true
echo 'ansible_python_interpreter=/usr/bin/python3' >> inventory || true
echo 'ansible_ssh_common_args='-o StrictHostKeyChecking=no'' >> inventory || true

# ansible thing
echo "################## Backup existing sys conf files ##################"
mkdir -p "$PWD/sysconf_backups"
ansible all -b -i inventory -m fetch -a "src=/etc/sysctl.conf dest=./sysconf_backups/sysctl.conf-{{ inventory_hostname }} flat=yes"
if [[  $? -eq 0 ]]
then
  tar -czvf sysconf_backups.tar.gz sysconf_backups/
  echo "Sysconf files backed up successfully."
  echo "################## current status ##################"
  ansible all -b -i inventory -m shell -a "cat /etc/sysctl.conf | egrep -i 'net.ipv4.conf.all.rp_filter|net.ipv4.conf.ccd_int.rp_filter|net.ipv4.vs.conntrack'"

  echo "--------------------------------------------------------"
  echo "----------------- Applying SNMP WA ---------------------"
  echo "--------------------------------------------------------"
  # ansible all -b -i inventory -m shell -a "sudo sed -i '/net.ipv4.conf.all.rp_filter=1/d' /etc/sysctl.conf; sudo sysctl -p"
  echo "Changing net.ipv4.conf.all.rp_filter from default=1 to 0"
  ansible all -b -i inventory -m shell -a "sed -i 's/^net.ipv4.conf.all.rp_filter = 1$/net.ipv4.conf.all.rp_filter = 0/' /etc/sysctl.conf; sysctl -p"

  echo "Changing net.ipv4.conf.ccd_int.rp_filter from default=1 to 0"
  ansible all -b -i inventory -m shell -a "sed -i 's/^net.ipv4.conf.ccd_int.rp_filter = 1$/net.ipv4.conf.ccd_int.rp_filter = 0/' /etc/sysctl.conf; sysctl -p"

  echo "Changing net.ipv4.vs.conntrack from default=0 to 1"
  ansible all -b -i inventory -m shell -a "sed -i 's/^net.ipv4.vs.conntrack = 0$/net.ipv4.vs.conntrack = 1/' /etc/sysctl.conf; sysctl -p"


  echo "################## post status ##################"
  ansible all -b -i inventory -m shell -a "cat /etc/sysctl.conf | egrep -i 'net.ipv4.conf.all.rp_filter|net.ipv4.conf.ccd_int.rp_filter|net.ipv4.vs.conntrack'"
  rm -rf "$PWD/sysconf_backups"
else
  tar -czvf sysconf_backups.tar.gz sysconf_backups/
  rm -rf "$PWD/sysconf_backups"
  echo "Failed to backup Sysconf files. Not applying SNMP W/A. Exiting script"
fi
