#!/bin/bash

# Pragmatica Lite - Patch Version Bump Script
# This script automatically bumps the patch version (e.g., 0.7.15 -> 0.7.16)

set -e  # Exit on any error

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Pragmatica Lite - Patch Version Bump${NC}"

# Get current version from pom.xml
CURRENT_VERSION=$(grep -o '<version>[^<]*</version>' pom.xml | head -1 | sed 's/<version>\(.*\)<\/version>/\1/')

if [ -z "$CURRENT_VERSION" ]; then
    echo -e "${RED}❌ Could not determine current version from pom.xml${NC}"
    exit 1
fi

echo -e "${BLUE}📋 Current version: ${CURRENT_VERSION}${NC}"

# Parse version parts
IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"

if [ ${#VERSION_PARTS[@]} -ne 3 ]; then
    echo -e "${RED}❌ Version format should be MAJOR.MINOR.PATCH (e.g., 0.7.15)${NC}"
    exit 1
fi

MAJOR=${VERSION_PARTS[0]}
MINOR=${VERSION_PARTS[1]}
PATCH=${VERSION_PARTS[2]}

# Increment patch version
NEW_PATCH=$((PATCH + 1))
NEW_VERSION="${MAJOR}.${MINOR}.${NEW_PATCH}"

echo -e "${BLUE}🔄 Bumping version to: ${NEW_VERSION}${NC}"

# Check if working directory is clean
if [ -n "$(git status --porcelain)" ]; then
    echo -e "${RED}❌ Working directory is not clean. Please commit or stash your changes first.${NC}"
    exit 1
fi

# Update version using Maven versions plugin
echo -e "${BLUE}📦 Updating Maven version...${NC}"
./mvnw versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false

# Verify the change
echo -e "${BLUE}🔍 Verifying version update...${NC}"
UPDATED_VERSION=$(grep -o '<version>[^<]*</version>' pom.xml | head -1 | sed 's/<version>\(.*\)<\/version>/\1/')

if [ "$UPDATED_VERSION" != "$NEW_VERSION" ]; then
    echo -e "${RED}❌ Version update failed. Expected: ${NEW_VERSION}, Got: ${UPDATED_VERSION}${NC}"
    exit 1
fi

# Commit the changes
echo -e "${BLUE}📝 Committing version bump...${NC}"
git add .
git commit -m "chore: bump version to ${NEW_VERSION}

🤖 Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>"

echo -e "${GREEN}✅ Successfully bumped version from ${CURRENT_VERSION} to ${NEW_VERSION}${NC}"
echo -e "${BLUE}💡 Don't forget to push your changes: git push${NC}"