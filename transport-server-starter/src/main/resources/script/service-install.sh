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

# Current bash user define.
APP_USER=$USER

# Current dir define.
CURR_DIR="$(cd "`dirname "$0"`"/.; pwd)"

# Reference external variable definition.
VAR_PATH=$CURR_DIR"/*-variable.sh"
. $VAR_PATH

# APP log define.
LOG_SERV_PATH=$LOGS_DIR/$MAIN_JAR_NAME"Server.out"

# Check installed.
if [ -f "/etc/init.d/$MAIN_JAR_NAME" ]; then
  while true
  do
    read -t 10 -p "Service has been installed, does it cover? (y/n)" cover
    if [ "$cover" == "y" ]; then
      break;
    elif [ "$cover" == "n" ]; then
      exit 0;
    else
      echo "Please reenter it!"
    fi
  done
fi

# Gen service script.
#
cat<<EOF>$CURR_DIR/$MAIN_JAR_NAME".tmp"
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

# Source networking configuration.
. /etc/sysconfig/network

# Check that networking is up.
[ "\$NETWORKING" = "no" ] && exit 0

# Load the user environment, for example, get the secret key of decrypting database password.
. /etc/profile
. /etc/bashrc
. /$APP_USER/.bash_profile
. /$APP_USER/.bashrc

# Get the execution command arg1.
COMMAND=\$1

# Execution command
EXEC_CMD="$JAVA -server $HEAP_OPTS $JVM_PERFORMANCE_OPTS $GC_LOG_OPTS $JMX_OPTS $JAVA_OPTS -cp $APP_CLASSPATH $MAIN_CLASS $APP_OPTS"

#
# Definition function
#
# Start function.
function start(){
  PIDS=\$(getPids) # Get current process code.
  if [ -z "\$PIDS" ]; then
    nohup \$EXEC_CMD > $LOG_SERV_PATH 2>&1 < /dev/null &
    while true
    do
      PIDS=\$(getPids)
      if [ "\$PIDS" == "" ]; then
        sleep 0.1;
      else
        break;
      fi
    done
    echo "Started on "\$PIDS
  else
    echo "Server is running "\$PIDS
  fi
}

# Stop function.
function stop(){
  PIDS=\$(getPids) # Get current process code.
  if [ -z "\$PIDS" ]; then
    echo "Not process $MAIN_JAR_NAME stop."
  else
    echo -n "Stopping $MAIN_JAR_NAME \$PIDS .."
    kill -s TERM \$PIDS
    while true
    do
      PIDS=\$(getPids)
      if [ "\$PIDS" == "" ]; then
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
  PIDS=\$(ps ax | grep java | grep -i $LIBS_DIR | grep -v grep | awk '{print \$1}')
  echo \$PIDS # Output execution result value.
  return 0 # Return the execution result code.
}

# Executive operations.
case \$COMMAND in
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
  echo \$"Usage:{start|stop|restart|status}"
  exit 2
esac
EOF

# Grant tmp bash script.
chmod 700 $CURR_DIR/$MAIN_JAR_NAME".tmp" && mv $CURR_DIR/$MAIN_JAR_NAME".tmp" /etc/init.d/$MAIN_JAR_NAME

echo -e "Please use the UNIX user installed at the moment to execute \
\"service {name} start|restart|stop\"... Etc., otherwise there may be \
\na problem of insufficient privileges.\n\nInstalled $MAIN_JAR_NAME service successfully."
