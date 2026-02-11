#!/bin/bash

# Rebuild Script for Client App
# Clears build cache and performs a clean build

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🧹 Cleaning build cache..."
rm -rf .nuxt .output

echo "🔨 Building client-app..."
npx nuxt build

echo "✅ Client app build complete!"
