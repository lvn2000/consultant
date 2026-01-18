#!/bin/bash
export DB_URL=${DB_URL:-"jdbc:postgresql://localhost:5432/consultant_db"}
export DB_USER=${DB_USER:-"consultant"}
export DB_PASSWORD=${DB_PASSWORD:-"bW1g55n9"}

sbt "project api" run
