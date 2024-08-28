#! /bin/bash

rm -rf not_to_remove
rm -rf cENM_backup_to_remove

echo "DEFAULT Backup dir set"
cENM_default=$(find /sftp_new/CNIS/ -type d | grep DEFAULT)

echo "rollback one"
cENM_rollback=$(find /sftp_new/CNIS/ -type d | grep ROLLBACK)

echo "removal of default one"
for i in ${cENM_default} ${cENM_rollback}
do
  count=$(ls -lrth ${i} | awk '{print $9}' | wc -l)
  if [[ ${count} -gt 4 ]]
  then
    trim=$((${count} - 4))
    echo "trim for ${i}"
    echo ${trim}
    ls -lrth ${i} | awk '{print $9}' | head -${trim} >> cENM_backup_to_remove
    ls -lrth ${i} | tail -4 | awk '{print $9}' >> not_to_remove
  fi
done

### remove one #####

for i in $(cat cENM_backup_to_remove)
do
  echo "File removing ${i}"
  #find /sftp_new/CNIS -name ${i}
  find /sftp_new/CNIS -name ${i} -exec rm -rf {} \;
    echo "==File removing ${i}=="
done

rm -rf not_to_remove
rm -rf cENM_backup_to_remove