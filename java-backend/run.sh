#!/bin/bash
# Simple script to download and use Maven if not available
export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-11-graalvm}

if ! command -v mvn &> /dev/null; then
    echo "Maven not found. Please install Maven:"
    echo "  Arch/Manjaro: sudo pacman -S maven"
    echo "  Ubuntu/Debian: sudo apt-get install maven"
    echo "  Or download from: https://maven.apache.org/download.cgi"
    exit 1
fi

mvn spring-boot:run
