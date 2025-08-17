#!/bin/bash

# Rabia Consensus Performance Test Runner
# Executes comprehensive performance benchmarks and generates reports

set -e

echo "=================================================="
echo "Rabia Consensus Performance Test Suite"
echo "=================================================="

# Set up environment
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

echo_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

echo_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Maven is available
if ! command -v ./mvnw &> /dev/null; then
    echo_error "Maven wrapper (./mvnw) not found"
    exit 1
fi

# Create results directory
RESULTS_DIR="performance-results/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$RESULTS_DIR"

echo_info "Performance test results will be saved to: $RESULTS_DIR"

# Function to run a specific test class
run_test() {
    local test_class="$1"
    local test_name="$2"
    local output_file="$RESULTS_DIR/${test_name}.log"
    
    echo_info "Running $test_name..."
    
    if MAVEN_OPTS="--enable-preview" ./mvnw test -Dtest="$test_class" -q > "$output_file" 2>&1; then
        echo_success "$test_name completed successfully"
        
        # Extract key metrics from log
        if grep -q "improvement\|speedup\|faster" "$output_file"; then
            echo_info "Key results from $test_name:"
            grep -E "(improvement|speedup|faster|reduction)" "$output_file" | head -5
        fi
    else
        echo_error "$test_name failed. Check $output_file for details"
        return 1
    fi
}

# Warm up JVM
echo_info "Warming up JVM..."
MAVEN_OPTS="--enable-preview" ./mvnw compile -pl cluster -q

echo ""
echo_info "Starting performance benchmark suite..."
echo ""

# Run individual benchmarks
echo "1. Vote Counting Performance Test"
echo "--------------------------------"
run_test "VoteCountingPerformanceTest" "vote-counting"
echo ""

echo "2. Memory Leak Validation Test"
echo "------------------------------"
run_test "MemoryLeakValidationTest" "memory-leak"
echo ""

echo "3. Before/After Performance Comparison"
echo "-------------------------------------"
run_test "BeforeAfterPerformanceComparison" "before-after"
echo ""

echo "4. Precise Performance Measurement"
echo "---------------------------------"
run_test "PrecisePerformanceMeasurement" "precise-measurement"
echo ""

# Generate summary report
echo_info "Generating performance summary report..."

SUMMARY_FILE="$RESULTS_DIR/performance-summary.txt"

cat > "$SUMMARY_FILE" << EOF
Rabia Consensus Performance Test Results
=======================================
Test Date: $(date)
Project: Pragmatica Lite - Cluster Module
Test Environment: $(uname -a)
Java Version: $(java -version 2>&1 | head -1)

Test Results Summary:
EOF

# Extract key metrics from all test outputs
for log_file in "$RESULTS_DIR"/*.log; do
    if [[ -f "$log_file" ]]; then
        test_name=$(basename "$log_file" .log)
        echo "" >> "$SUMMARY_FILE"
        echo "=== $test_name ===" >> "$SUMMARY_FILE"
        
        # Extract performance metrics
        grep -E "(improvement|speedup|faster|reduction|ms|ops/sec)" "$log_file" | head -10 >> "$SUMMARY_FILE" || echo "No metrics found" >> "$SUMMARY_FILE"
    fi
done

echo_success "Performance summary saved to: $SUMMARY_FILE"

# Check for any test failures
failed_tests=0
for log_file in "$RESULTS_DIR"/*.log; do
    if grep -q "FAILED\|ERROR" "$log_file"; then
        failed_tests=$((failed_tests + 1))
    fi
done

echo ""
echo "=================================================="
if [[ $failed_tests -eq 0 ]]; then
    echo_success "All performance tests completed successfully!"
    echo_info "Results available in: $RESULTS_DIR"
    echo_info "Summary report: $SUMMARY_FILE"
else
    echo_warning "$failed_tests test(s) had issues. Check individual log files."
fi
echo "=================================================="

# Display quick summary
echo ""
echo_info "Quick Performance Summary:"
echo "-------------------------"

if [[ -f "$RESULTS_DIR/vote-counting.log" ]]; then
    vote_improvement=$(grep -o "[0-9]\+% faster\|[0-9]\+\.[0-9]\+x speedup" "$RESULTS_DIR/vote-counting.log" | head -1 || echo "N/A")
    echo "• Vote Counting: $vote_improvement"
fi

if [[ -f "$RESULTS_DIR/memory-leak.log" ]]; then
    memory_reduction=$(grep -o "[0-9]\+% reduction\|[0-9]\+% less memory" "$RESULTS_DIR/memory-leak.log" | head -1 || echo "N/A")
    echo "• Memory Usage: $memory_reduction"
fi

if [[ -f "$RESULTS_DIR/before-after.log" ]]; then
    overall_improvement=$(grep -o "OVERALL.*[0-9]\+\.[0-9]\+%" "$RESULTS_DIR/before-after.log" || echo "N/A")
    echo "• Overall: $overall_improvement"
fi

echo ""
echo_info "For detailed results, see: $RESULTS_DIR"

exit 0