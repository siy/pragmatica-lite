#!/bin/bash

# Release script for Maven Central publishing
# Usage: ./scripts/release.sh

set -e

echo "🚀 Starting Maven Central release process..."

# Verify we're on main branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "❌ Error: Must be on main branch for release. Current branch: $CURRENT_BRANCH"
    exit 1
fi

# Verify working directory is clean
if [ -n "$(git status --porcelain)" ]; then
    echo "❌ Error: Working directory is not clean. Please commit or stash changes."
    git status --short
    exit 1
fi

# Verify tests pass (use verify to install locally before testing)
echo "🧪 Running tests..."
mvn clean verify -q

# Verify GPG setup
echo "🔐 Checking GPG configuration..."
if ! gpg --list-secret-keys | grep -q "sec"; then
    echo "❌ Error: No GPG secret keys found. Please set up GPG signing."
    echo "See: https://central.sonatype.org/publish/requirements/gpg/"
    exit 1
fi

# Build and verify artifacts
echo "📦 Building release artifacts..."
mvn clean package -DperformRelease=true -q

# Deploy with auto-publish enabled
echo "📤 Deploying to Maven Central (auto-publish enabled)..."
mvn deploy -DperformRelease=true

echo "✅ Release published to Maven Central!"
echo ""
echo "Artifacts will be available at:"
echo "https://central.sonatype.com/artifact/org.pragmatica-lite/core"