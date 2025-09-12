#!/bin/bash

# Script to run the Posturely desktop app with proper resource loading

# Ensure resources are copied
mkdir -p composeApp/build/processedResources/desktop/main/composeResources/posturelynew.composeapp.generated.resources/drawable
cp composeApp/src/commonMain/composeResources/drawable/* composeApp/build/processedResources/desktop/main/composeResources/posturelynew.composeapp.generated.resources/drawable/

# Build the classes
./gradlew :composeApp:compileKotlinDesktop

# Run the app with proper classpath including resources
java -cp "composeApp/build/classes/kotlin/desktop/main:composeApp/build/processedResources/desktop/main:$(find ~/.gradle/caches -name "*.jar" | tr '\n' ':')" com.example.posturelynew.DesktopAppKt
