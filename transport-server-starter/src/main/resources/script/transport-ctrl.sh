#!/bin/bash
# Copyright (c) 2017 ~ 2025, the original author wangl.sir individual Inc,
# All rights reserved. Contact us 983708408@qq.com
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Source function library.
. /etc/rc.d/init.d/functions

# Source networking configuration.
. /etc/sysconfig/network

# Check that networking is up.
[ "$NETWORKING" = "no" ] && exit 0

# Check java env variable.
if [ -z "${JAVA_HOME}" ]; then
  echo "Please perfect the Java environment variable `JAVA_HOME`."
  exit 1
fi

# Current directory.
CURR_DIR="$(cd "`dirname "$0"`"/.; pwd)"

# Reference external variable definition.
VAR_PATH=$CURR_DIR"/*-variable.sh"
. $VAR_PATH

# Get the execution command, last arguments.
COMMAND="${!#}"

# Execution command
EXEC_CMD="$JAVA -server $HEAP_OPTS $JVM_PERFORMANCE_OPTS $GC_LOG_OPTS $JMX_OPTS $JAVA_OPTS -cp $APP_CLASSPATH $MAIN_CLASS $APP_OPTS"

#
# Definition function
#
# Start function.
function start(){
  PIDS=$(getPids) # Get current process code.
  if [ -z "$PIDS" ]; then
    if [ "x$DAEMON_MODE" = "xtrue" ]; then
      nohup $EXEC_CMD > "$CONSOLE_OUTPUT_FILE" 2>&1 < /dev/null &
    else
      exec $EXEC_CMD
    fi
    while true
    do
      PIDS=$(getPids)
      if [ "$PIDS" == "" ]; then
        sleep 0.1;
      else
        break;
      fi
    done
    echo "Started on "$PIDS
  else
    echo "Server is running "$PIDS
  fi
}

# Stop function.
function stop(){
  PIDS=$(getPids)
  if [ -z "$PIDS" ]; then
    echo "Not process $MAIN_JAR_NAME stop."
  else
    echo -n "Stopping $MAIN_JAR_NAME $PIDS .."
    kill -s TERM $PIDS
    while true
    do
      PIDS=$(getPids)
      if [ "$PIDS" == "" ]; then
        break;
      else
        echo -n ".";
        sleep 0.8;
      fi
    done
    echo -e "\nStop successfully."
  fi
}

# Status function.
function status(){
  ps -ef | grep -v grep | grep $LIBS_DIR
}

# Get pids.
function getPids(){
  PIDS=$(ps ax | grep java | grep -i $LIBS_DIR | grep -v grep | awk '{print $1}')
  echo $PIDS # Output execution result value.
  return 0 # Return the execution result code.
}

# Executive operations.
case $COMMAND in
  status)
    status
    ;;
  start)
    start
    ;;
  stop)
    stop
    ;;
  restart)
    stop
    start
    ;;
    *)
  echo $"Usage:{start|stop|restart|status}"
  exit 2
esac

