#!/bin/bash

mvn clean package -Pnative
mv target/dict-1.0.0-SNAPSHOT-runner target/dict

pushd target/quarkus-app
  java -cp $(ls lib/main | awk '{print "lib/main/" $1}' | tr "\n" ":")../dict-1.0.0-SNAPSHOT.jar picocli.AutoComplete de.bender.dict.boundary.DictCommand
  mv dict_completion ..
popd

./target/dict -h
