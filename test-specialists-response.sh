#!/bin/bash
# Test specialists endpoint

# Get token
curl -s http://localhost:8090/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"admin","password":"admin"}' | jq -r '.accessToken' > /tmp/token.txt

echo "=== Testing specialists/search endpoint ==="
curl -s "http://localhost:8090/api/specialists/search?offset=0&limit=1000" \
  -H "Authorization: Bearer $(cat /tmp/token.txt)" | jq '.[0] | {id, name, categoryRates}'
