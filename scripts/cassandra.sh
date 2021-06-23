#
# Copyright (c) 2021 Oracle and/or its affiliates.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Cassandra database setup (shell script)

echo 'Using MySQL database'

readonly DB_HOST='127.0.0.1'
readonly DB_PORT='9042'
readonly DB_NAME='pokemon'
readonly DB_USER=''
readonly DB_PASSWORD=''
readonly DB_ROOT_PASSWORD=''
readonly DB_URL="cassandra"

readonly TEST_CONFIG='cassandra.yaml'

# DC name 'single' is used in the source code too.
readonly DOCKER_ENV="-e CASSANDRA_DC=single -e CASSANDRA_ENDPOINT_SNITCH=GossipingPropertyFileSnitch"
readonly DOCKER_IMG='cassandra:3'

echo " - Database URL: ${DB_URL}"
