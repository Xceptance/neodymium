#!/bin/bash

if [ -z "$1" ]; then
  echo "Usage: $0 <yaml_file_path | test_id>"
  exit 1
fi

INPUT="$1"

# Function to find project root (directory containing pom.xml)
find_project_root() {
  local DIR="$1"
  while [ "$DIR" != "/" ]; do
    if [ -f "$DIR/pom.xml" ]; then
      echo "$DIR"
      return 0
    fi
    DIR=$(dirname "$DIR")
  done
  # Fallback to current directory if no pom.xml is found
  pwd
}

if [ -f "$INPUT" ]; then
  # Input is an existing file, get absolute path
  ABS_PATH=$(realpath "$INPUT")
  echo "Running Neodymium tests with data file: $ABS_PATH"
  
  # Find the project root from the file's location
  PROJECT_DIR=$(find_project_root "$(dirname "$ABS_PATH")")
  cd "$PROJECT_DIR" || exit 1
  echo "Project directory resolved to: $PROJECT_DIR"
  
  mvn test -Dneodymium.testFileFilter="$ABS_PATH" "${@:2}"
else
  # Assume it's a test ID
  echo "Running Neodymium tests with testId: $INPUT"
  
  # Find the project root from the current working directory
  PROJECT_DIR=$(find_project_root "$(pwd)")
  cd "$PROJECT_DIR" || exit 1
  echo "Project directory resolved to: $PROJECT_DIR"
  
  mvn test -Dneodymium.testIdFilter="$INPUT" "${@:2}"
fi
