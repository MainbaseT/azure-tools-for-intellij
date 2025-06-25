#! /bin/sh
set -eu

./gradlew prepareDotNetPart
dotnet build ./ReSharper.Azure.sln
