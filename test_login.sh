#!/bin/bash
DB_URL="jdbc:postgresql://localhost:5432/consultant_db" DB_USER="consultant" DB_PASSWORD="bW1g55n9" sbt "project api" run &
SERVER_PID=$!
sleep 15
curl -X POST http://127.0.0.1:8090/api/users/login -H "Content-Type: application/json" -d "{\"login\":\"admin\",\"password\":\"admin\"}" -w "\nHTTP Status: %{http_code}\n"
kill $SERVER_PID 2>/dev/null
wait $SERVER_PID 2>/dev/null

