#! /bin/bash

#sudo su


rsync -avz -e "ssh" /home/cenmbuild/AUTO/* cenmbuild@214.14.16.36:/home/cenmbuild/AUTO/
