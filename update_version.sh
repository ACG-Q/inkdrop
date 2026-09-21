#!/bin/bash

# Version update script for InkDrop Wallpaper
# Usage: ./update_version.sh <version>
# Example: ./update_version.sh 1.2.3

if [ -z "$1" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.2.3"
    exit 1
fi

VERSION="$1"

# Validate version format
if ! [[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: Invalid version format. Use X.Y.Z (e.g., 1.2.3)"
    exit 1
fi

# Extract version components
MAJOR=$(echo $VERSION | cut -d. -f1)
MINOR=$(echo $VERSION | cut -d. -f2)
PATCH=$(echo $VERSION | cut -d. -f3)

# Calculate versionCode (major * 10000 + minor * 100 + patch)
VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))

echo "Updating to version $VERSION (code: $VERSION_CODE)"

# Update build.gradle
sed -i "s/versionCode [0-9]*/versionCode $VERSION_CODE/" app/build.gradle
sed -i "s/versionName \"[^\"]*\"/versionName \"$VERSION\"/" app/build.gradle

echo "Done! Updated build.gradle:"
grep -E "versionCode|versionName" app/build.gradle
