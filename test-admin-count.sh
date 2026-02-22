#!/bin/bash
# Test admin-count endpoint

echo "=== Step 1: Login ==="
TOKEN=$(curl -s http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin"}' | jq -r '.accessToken')

echo "Token: ${TOKEN:0:50}..."

echo ""
echo "=== Step 2: Get Admin Count ==="
curl -s http://localhost:8090/api/admin-count \
  -H "Authorization: Bearer $TOKEN" | jq .
