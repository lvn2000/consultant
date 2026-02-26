#!/bin/bash
# Test slots endpoint

# Get token
curl -s http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin"}' | jq -r '.accessToken' > /tmp/token.txt

echo "=== Testing slots endpoint ==="
echo "Token: $(head -c 50 /tmp/token.txt)..."
echo ""

# Test slots
curl -s "http://localhost:8090/api/specialists/cf9b7fb4-7dc1-4a39-80ed-e0d7aa4b6fa3/availability/slots?date=2026-02-22" \
  -H "Authorization: Bearer $(cat /tmp/token.txt)" | jq .
