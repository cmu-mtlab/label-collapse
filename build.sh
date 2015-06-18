#!/bin/env bash

LC_HOME=$(dirname $0)

mkdir -p $LC_HOME/bin
javac -d $LC_HOME/bin  $LC_HOME/src/*.java
