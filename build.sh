#!/bin/bash
set -e

LOG_DIR="logs"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${LOG_DIR}/docker_build_${TIMESTAMP}.log"

mkdir -p "${LOG_DIR}"

echo "Starting docker build... Log: ${LOG_FILE}"

docker-compose build 2>&1 | tee "${LOG_FILE}"

EXIT_CODE=${PIPESTATUS[0]}

if [ $EXIT_CODE -ne 0 ]; then
    echo "BUILD FAILED (exit code: ${EXIT_CODE}) - see log: ${LOG_FILE}"
else
    echo "BUILD SUCCEEDED - log: ${LOG_FILE}"
fi

exit ${EXIT_CODE}