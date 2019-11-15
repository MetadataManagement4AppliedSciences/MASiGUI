#!/bin/sh
export MAVEN_OPTS="-DskipTests=true"
export KIT_DM_VERSION=1.5
export REPOCLIENT_TAG=GenericRepoClient_1.5.1
export BASE_DIR=`pwd`

# Delete old temporary files
mkdir -p $BASE_DIR/tmpbuilds
rm -rf $BASE_DIR/tmpbuilds/*

echo "\e[1mDownloading KITDM version $KIT_DM_VERSION and $REPOCLIENT_TAG\e[0m"

# Get release of KIT Data Manager and build it
cd $BASE_DIR/tmpbuilds
wget https://github.com/kit-data-manager/base/archive/KITDM_$KIT_DM_VERSION.zip
unzip KITDM_$KIT_DM_VERSION.zip
cd $BASE_DIR/tmpbuilds/base-KITDM_$KIT_DM_VERSION
mvn $MAVEN_OPTS clean install

# Build RepoClient
cd $BASE_DIR/tmpbuilds
git clone -b 'GenericRepoClient_1.5.1' --single-branch --depth 1 https://github.com/kit-data-manager/generic-repo-client.git $REPOCLIENT_TAG
cd $BASE_DIR/tmpbuilds/$REPOCLIENT_TAG
mvn clean install

# Get additional files
cd $BASE_DIR/tmpbuilds
echo "\e[1mTUD-Fusionforge username: \e[0m"
read USERNAME
git clone git+ssh://$USERNAME@scm.fusionforge.zih.tu-dresden.de/srv/git/masidev/miscmasi.git 

cd $BASE_DIR/
mvn clean dependency:copy-dependencies && cp -rf target/dependency/* lib/ 

echo "\e[1mDownloading vaadin and its dependencies to ./deploy\e[0m"
mkdir -p $BASE_DIR/deploy
rm -rf $BASE_DIR/deploy/*
cd $BASE_DIR/deploy
wget https://vaadin.com/download/release/8.4/8.4.5/vaadin-all-8.4.5.zip
unzip vaadin-all-8.4.5.zip
mv lib/gentyref-1.2.0.vaadin1.jar lib/jsoup-1.11.2.jar ./
find . ! -name '*.jar' -execdir rm -rf {} +


echo "\e[1mNow add your database password in src/main/resources/META-INF/persistence.xml on every occurence of 'password' and build the portlet: $ \e[7mmvn clean package\e[0m"
echo "\e[1mIf packaging is successfull you can copy ./deploy/* and ./target/MASiGenGUI*.jar to your liferay delpoy/ folder.\e[0m"
echo "\e[1mRemember to redeploy vaadin-liferay-integration-*.jar to mitigate a vaadin bug. For futher information see README.md/Troubleshooting.\e[0m"


