#!/bin/bash

mvn clean package -DskipTests
cp target/memshell-killer.jar skills/memshell-killer/scripts/