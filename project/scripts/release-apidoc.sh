#!/bin/bash

gitrepo="git@github.com:mrico/async-dbx-client.git"
tmpdir="${TMPDIR:-/tmp}"
localrepo="${tmpdir}/async-dbx-client"

# cleanup
if [ -d $localrepo ]; then
  rm -Rf $localrepo
fi

# clone git git repo to tmp folder
git clone $gitrepo $localrepo

# change working directory
cd $localrepo

# generate api doc
sbt -batch doc
git checkout gh-pages

apidoc="api/latest"
apigen=$(find target/scala* -maxdepth 1 -type d -name api)
if [ ! -d $apigen ]; then
  echo "Generated api doc not found: $apigen"
  exit 1
fi

# replace latest api doc with generated
git rm -r $apidoc
mkdir -p $apidoc
cp -R ${apigen}/* $apidoc

# commit changes
git add $apidoc
git commit -m "update latest api doc"
git push origin gh-pages

# cleanup
rm -Rf $localrepo
