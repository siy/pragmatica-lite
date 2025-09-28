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

# Verify tests pass
echo "🧪 Running tests..."
./mvnw clean test -q

# Verify GPG setup
echo "🔐 Checking GPG configuration..."
if ! gpg --list-secret-keys | grep -q "sec"; then
    echo "❌ Error: No GPG secret keys found. Please set up GPG signing."
    echo "See: https://central.sonatype.org/publish/requirements/gpg/"
    exit 1
fi

# Build and verify artifacts
echo "📦 Building release artifacts..."
./mvnw clean package -DperformRelease=true -q

# Deploy to staging repository
echo "📤 Deploying to staging repository..."
./mvnw deploy -DperformRelease=true

echo "✅ Release deployed to staging repository!"
echo ""
echo "Next steps:"
echo "1. Log into https://s01.oss.sonatype.org/"
echo "2. Go to 'Staging Repositories'"
echo "3. Find your staging repository"
echo "4. Click 'Close' to validate the release"
echo "5. If validation passes, click 'Release' to publish to Maven Central"
echo ""
echo "📚 For more information, see:"
echo "https://central.sonatype.org/publish/release/"