#!/bin/bash
# Wrapper to start blog backend with correct DB password
# This avoids Hermes password masking in commands

cd /home/halo/vibe-code/personal-blog-cms

# Source .env properly (only DB vars)
eval $(grep -E '^DB_' .env | sed 's/^/export /')

# Start backend
cd backend
exec java -jar target/blog-cms-backend.jar --spring.profiles.active=dev
