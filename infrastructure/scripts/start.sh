#!/bin/bash
set -e
cd "$(dirname "$0")/.."
docker compose up -d
echo "Waiting for services..."
sleep 5
docker compose ps
