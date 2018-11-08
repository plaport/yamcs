#!/bin/bash
set -e

# This script generates releases in two steps:
# 1. Compile a fresh clone of the development tree
# 2. Emit artifacts (yamcs, yamcs-simulation, yamcs-client) in various formats.
#
# By design (2) does not require any sort of compilation.

buildweb=1
sign=1

all=1
yamcs=0
yamcsclient=0
yamcssimulation=0

for arg in "$@"; do
    case "$arg" in
    --no-web)
        buildweb=0
        ;;
    --no-sign)
        sign=0
        ;;
    yamcs)
        all=0
        yamcs=1
        ;;
    yamcs-client)
        all=0
        yamcsclient=1
        ;;
    yamcs-simulation)
        all=0
        yamcssimulation=1
        ;;
    *)
        echo "Usage: $0 [--no-web] [--no-sign] [yamcs|yamcs-client|yamcs-simulation]..."
        exit 1;
        ;;
    esac
done

if [ $all -eq 1 ]; then
    yamcs=1
    yamcsclient=1
    yamcssimulation=1
fi

cd `dirname $0`
yamcshome=`pwd`
pomversion=`grep -m 1 '<version>.*</version>' pom.xml | sed -e 's/.*<version>\(.*\)<\/version>.*/\1/'`

if [[ $pomversion == *-SNAPSHOT ]]; then
    d=`date +%Y%m%d%H%M%S`
    version=${pomversion/-SNAPSHOT/}
    release=SNAPSHOT$d
else
    version=$pomversion
    release=1
fi

if [[ -n $(git status -s) ]]; then
    read -p 'Your workspace contains dirty or untracked files. These will not be part of your release. Continue? [Y/n]' yesNo
    if [[ -n $yesNo ]] && [[ $yesNo == 'n' ]]; then
        exit 0
    fi
fi

mvn clean

clonedir=$yamcshome/distribution/target/yamcs-clone

mkdir -p $clonedir
git clone . $clonedir
rm -rf $clonedir/.git

cd $clonedir

if [ $yamcs -eq 1 -a $buildweb -eq 1 ]; then
    cd yamcs-web
    yarn install
    yarn build
    rm -rf `find . -maxdepth 3 -name node_modules`
    cd ..
fi

mvn package -DskipTests

rpmtopdir="$yamcshome/distribution/target/rpmbuild"
mkdir -p $rpmtopdir/{RPMS,BUILD,SPECS,tmp}

if [ $yamcs -eq 1 ]; then
    cp distribution/target/yamcs-$pomversion.tar.gz $yamcshome/distribution/target
    rpmbuilddir="$rpmtopdir/BUILD/yamcs-$version-$release"
    mkdir -p "$rpmbuilddir/opt/yamcs"
    tar -xzf distribution/target/yamcs-$pomversion.tar.gz --strip-components=1 -C "$rpmbuilddir/opt/yamcs"
    mkdir -p "$rpmbuilddir/etc/init.d"
    cp -a distribution/sysvinit/* "$rpmbuilddir/etc/init.d"
    cat distribution/rpm/yamcs.spec | sed -e "s/@@VERSION@@/$version/" | sed -e "s/@@RELEASE@@/$release/" > $rpmtopdir/SPECS/yamcs.spec
    rpmbuild --define="_topdir $rpmtopdir" -bb "$rpmtopdir/SPECS/yamcs.spec"
fi

if [ $yamcssimulation -eq 1 ]; then
    cp -r $clonedir "$rpmtopdir/BUILD/yamcs-simulation-$version-$release"
    cat distribution/rpm/yamcs-simulation.spec | sed -e "s/@@VERSION@@/$version/" | sed -e "s/@@RELEASE@@/$release/" > $rpmtopdir/SPECS/yamcs-simulation.spec
    rpmbuild --define="_topdir $rpmtopdir" -bb "$rpmtopdir/SPECS/yamcs-simulation.spec"
fi

if [ $yamcsclient -eq 1 ]; then
    cp distribution/target/yamcs-client-$pomversion.tar.gz $yamcshome/distribution/target
    rpmbuilddir="$rpmtopdir/BUILD/yamcs-client-$version-$release"
    mkdir -p "$rpmbuilddir/opt/yamcs-client"
    tar -xzf distribution/target/yamcs-client-$pomversion.tar.gz --strip-components=1 -C "$rpmbuilddir/opt/yamcs-client"
    cat distribution/rpm/yamcs-client.spec | sed -e "s/@@VERSION@@/$version/" | sed -e "s/@@RELEASE@@/$release/" > $rpmtopdir/SPECS/yamcs-client.spec
    rpmbuild --define="_topdir $rpmtopdir" -bb "$rpmtopdir/SPECS/yamcs-client.spec"
fi

cd "$yamcshome"
mv distribution/target/rpmbuild/RPMS/noarch/* distribution/target/

rm -rf $clonedir
rm -rf $rpmtopdir

if [ $sign -eq 1 ]; then
    rpmsign --key-id yamcs@spaceapplications.com --addsign distribution/target/*.rpm
fi

echo
echo 'All done. Generated artifacts:'
ls -lh `find distribution/target -type f -maxdepth 1`
