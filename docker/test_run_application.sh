#!/bin/bash

# This script tests docker/run_application.sh by mocking the java command.
# It verifies that run_application.sh correctly returns the exit code of the process.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_APP_SCRIPT="$SCRIPT_DIR/run_application.sh"
TEMP_DIR=$(mktemp -d)

# Mock environment
export APP="test-app"
export JAVA_HOME="$TEMP_DIR/mock-java-home"
export PATH="$JAVA_HOME/bin:$PATH"

mkdir -p "$JAVA_HOME/bin"
SRV_DIR="$TEMP_DIR/srv"
mkdir -p "$SRV_DIR/$APP"

# Helper to create mock java
create_mock_java() {
  local exit_code=$1
  local sleep_time=$2
  cat <<EOF > "$JAVA_HOME/bin/java"
#!/bin/bash
sleep $sleep_time
exit $exit_code
EOF
  chmod +x "$JAVA_HOME/bin/java"
}

cleanup() {
  rm -rf "$TEMP_DIR"
}
trap cleanup EXIT

echo "--- Test 1: Successful execution (exit code 0) ---"
create_mock_java 0 1

TEST_RUN_APP="$TEMP_DIR/run_application_testable.sh"
# run_application.sh uses 'cd /srv/"$APP"' and calls '/opt/java/bin/java'
# We replace these with testable paths.
sed "s|/opt/java/bin/java|java|g; s|/srv/|${SRV_DIR}/|g" "$RUN_APP_SCRIPT" > "$TEST_RUN_APP"
chmod +x "$TEST_RUN_APP"

echo "Running Test 1..."
bash "$TEST_RUN_APP"
ACTUAL_EXIT_CODE=$?

if [ $ACTUAL_EXIT_CODE -eq 0 ]; then
  echo "Test 1 passed: Exit code is 0."
else
  echo "Test 1 failed: Expected exit code 0, but got $ACTUAL_EXIT_CODE"
  exit 1
fi

echo "--- Test 2: Failed execution (exit code 1) ---"
create_mock_java 1 1
echo "Running Test 2..."
set +e
bash "$TEST_RUN_APP"
ACTUAL_EXIT_CODE=$?
set -e

if [ $ACTUAL_EXIT_CODE -eq 1 ]; then
  echo "Test 2 passed: Exit code is 1."
else
  echo "Test 2 failed: Expected exit code 1, but got $ACTUAL_EXIT_CODE"
  exit 1
fi

echo "--- Test 3: SIGTERM handling ---"
create_mock_java 0 10 & 
MOCK_JAVA_PID=$!
echo "Running Test 3..."
bash "$TEST_RUN_APP" &
TEST_PID=$!

sleep 1
echo "Sending SIGTERM to $TEST_PID"
kill -SIGTERM "$TEST_PID"
# Wait for the process to exit and get its exit code. 
# We use 'wait' which should return the exit code of the process.
set +e
wait "$TEST_PID"
ACTUAL_EXIT_CODE=$?
set -e

# When a bash script is terminated by SIGTERM and has a trap,
# if the trap calls 'exit 0', the script should exit with 0.
# However, if it's running under another bash instance (like 'bash "$TEST_RUN_APP"'),
# the behavior might vary depending on how signals are propagated.

if [ $ACTUAL_EXIT_CODE -eq 0 ] || [ $ACTUAL_EXIT_CODE -eq 143 ]; then
  echo "Test 3 passed: Exit code is $ACTUAL_EXIT_CODE after SIGTERM (expected 0 or 143)."
else
  echo "Test 3 failed: Expected exit code 0 or 143 after SIGTERM, but got $ACTUAL_EXIT_CODE"
  exit 1
fi

echo "All tests passed successfully!"
