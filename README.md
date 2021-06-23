# Cassandra Integration Tests with Native image

## Building the Tests

To build the tests, execute Maven build

    mvn clean install

## Running the tests

To run the tests, use test.sh Bash shell script in the root of the project:

    $> ./test.sh -h
    Usage: test.sh [-hcjn] -d <database>

      -h print this help and exit
      -c start and stop Docker containers
      -j execute remote application tests in Java VM mode (default)
      -n execute remote application tests in native image mode

To run the tests in Java VM mode with Cassandra Docker container, execute:

    ./test.sh -cj

To run the tests in Native Image mode with Cassandra Docker container, execute:

    ./test.sh -cn

Tests in Native Image mode will fail in native image build phase.
