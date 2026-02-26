#!/bin/bash
# Test if specialists have categoryRates

TOKEN=$(cat /tmp/token.txt)

echo "=== Testing specialists/search endpoint ==="
curl -s "http://localhost:8090/api/specialists/search?offset=0&limit=1" \
  -H "Authorization: Bearer $TOKEN" | jq '.[0] | {id, name, categoryCount: (.categoryRates | length)}'
