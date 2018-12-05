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

# Load the user environment, for example, get the secret key of decrypting database password.
. /etc/profile
. /etc/bashrc
. ~/.bash_profile
. ~/.bashrc

# Base directory.
BASE_DIR="$(cd "`dirname "$0"`"/..; pwd)"

#
# Global define.
#
MAIN_JAR_NAME="transport"
MAIN_CLASS="io.transport.Transport"

DATA_DIR="/mnt/disk1/$MAIN_JAR_NAME"
LIBS_DIR=$BASE_DIR"/libs"
CONF_DIR=$BASE_DIR"/conf"
LOGS_DIR="$DATA_DIR/logs"
# Please note that "java -cp" is in order. See: https://www.jianshu.com/p/23e0517d76f7
# and https://docs.oracle.com/javase/8/docs/technotes/tools/unix/classpath.html
APP_CLASSPATH=:$CONF_DIR:

# Correction path(// to /)
DATA_DIR=${DATA_DIR//\/\//\/}
LIBS_DIR=${LIBS_DIR//\/\//\/}
CONF_DIR=${CONF_DIR//\/\//\/}
LOGS_DIR=${LOGS_DIR//\/\//\/}
APP_CLASSPATH=${APP_CLASSPATH//\/\//\/}

#
# Runtime define.
#
# Which java to use
if [ -z "$JAVA_HOME" ]; then
  JAVA="java"
else
  JAVA="$JAVA_HOME/bin/java"
fi

# Memory options
if [ -z "$HEAP_OPTS" ]; then
  HEAP_OPTS="-Xms256M -Xmx1G"
fi

# JVM performance options
if [ -z "$JVM_PERFORMANCE_OPTS" ]; then
  # The `-server` parameter indicates that the current JVM is activated in server or client mode (only the old 32 bit JDK is supported),
  # and is not supported in the current 64 bit JDK. 
  JVM_PERFORMANCE_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=20 -XX:InitiatingHeapOccupancyPercent=35 -XX:+DisableExplicitGC -Djava.awt.headless=true"

  # The -XX:HeapDumpOnOutOfMemoryError parameter generates a snapshot under the XX:HeapDumpPath=./jvm_dump.hprof when a memory overflow occurs in JVM,
  # and the default path is user.dir.
  JVM_PERFORMANCE_OPTS="$JVM_PERFORMANCE_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOGS_DIR}/jvm_dump.hprof"

  # The OnOutOfMemoryError parameter allows the user to specify a callback action when the oom appears, such as the knowledge of the mail.
  #JVM_PERFORMANCE_OPTS="$JVM_PERFORMANCE_OPTS -XX:OnOutOfMemoryError=`sh $BASE_DIR/example-notification.sh`"
fi

# JMX settings
if [ -z "$JMX_OPTS" ]; then
  JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
fi

# JMX port to use
if [ "$JMX_PORT" ]; then
  JMX_OPTS="$JMX_OPTS -Dcom.sun.management.jmxremote.port=$JMX_PORT"
fi

# Generic JVM settings you want to add
if [ -z "$JAVA_OPTS" ]; then
  # Add JAVA_OPTS for debugger.
  JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
  # http://netty.io/wiki/reference-counted-objects.html
  # DISABLED - disables leak detection completely. Not recommended. SIMPLE - tells if there is a leak or 
  # not for 1% of buffers. Default. ADVANCED - tells where the leaked buffer was accessed for 1% of buffers.
  # PARANOID - Same with ADVANCED except that it's for every single buffer. Useful for automated testing phase.
  # You could fail the build if the build output contains 'LEAK:'.
  #JAVA_OPTS="$JAVA_OPTS -Dio.netty.leakDetection.level=ADVANCED"
  #JAVA_OPTS="$JAVA_OPTS $JMX_OPTS"
fi

while [ $# -gt 0 ]; do
  COMMAND=$1
  case $COMMAND in
    -name)
      DAEMON_NAME=$2
      CONSOLE_OUTPUT_FILE=$LOGS_DIR/$DAEMON_NAME.out
      shift 2
      ;;
    -loggc)
      if [ -z "$GC_LOG_OPTS" ]; then
        GC_LOG_ENABLED="true"
      fi
      shift
      ;;
    -daemon)
      DAEMON_MODE="true"
      shift
      ;;
    *)
      break
      ;;
  esac
done

# Check directory existence.
if [ ! -x "$DATA_DIR" ]; then
  mkdir -p "$DATA_DIR"
fi

if [ ! -x "$LOGS_DIR" ]; then
  mkdir -p "$LOGS_DIR"
fi

# GC options.
GC_FILE_SUFFIX='-gc.log'
GC_LOG_FILE_NAME=''
if [ "x$GC_LOG_ENABLED" = "xtrue" ]; then
  GC_LOG_FILE_NAME=$DAEMON_NAME$GC_FILE_SUFFIX
  GC_LOG_OPTS="-Xloggc:$LOGS_DIR/$GC_LOG_FILE_NAME -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCTimeStamps "
fi

# APP classpath addition for release.
#
# There is a problem when using shell to set a class path, that is, when there is a jar conflict, you need to adjust the order of
# the "CLASSPATH" path yourself, or you can use the "Maven jar plug-in" and "Maven assembler plug-in". 
# See: https://yq.aliyun.com/articles/604026?spm=a2c4e.11155435.0.0.32d633123UyO88
# Why do you want to use "sort -r" ? 
# The problem of the order of "Java -cp" loading. See: https://www.jianshu.com/p/23e0517d76f7
# You also use the way "sort -k 1" and so on to generate the desired order.
for file in `ls -a "$BASE_DIR"/libs/* | sort`;
do
  APP_CLASSPATH="$APP_CLASSPATH":"$file"
done

# Repair class path order (reordering) to solve jar packet conflicts.
#
# The jar package path that needs to be repaired is defined as an array: "REPAIR_ARR", the fix rule is that the location of the I
# element and the i+1 element is interchanged, so the length of the array REPAIR_ARR must be an even number. EG:
# REPAIR_ARR=(aaa.jar bbb.jar ccc.jar ddd.jar) It means changing the location of aaa.jar and bbb.jar, and changing the location between ccc.jar and ddd.jar.
# 
#REPAIR_ARR=(phoenix-client-4.7.0-Hbase-1.1.jar logback-classic-1.1.11.jar phoenix-client-4.7.0-Hbase-1.1.jar tomcat-embed-core-8.5.31.jar)
REPAIR_LEN=${#REPAIR_ARR[@]}
if [ $REPAIR_LEN -gt 1 ]; then
  echo "Repair classpath..."
  TMP_STR="__tmp_string_"
  # Check repair array string legality.
  if [ $(($REPAIR_LEN % 2)) != 0 ]; then
    echo "The length of the classpath repair path array must be even!"
    exit 0;
  fi

  # Repair replace process.
  for((i=0;i<${#REPAIR_ARR[@]}-1;i++)) do
    APP_CLASSPATH=${APP_CLASSPATH/${REPAIR_ARR[i]}/$TMP_STR}
    APP_CLASSPATH=${APP_CLASSPATH/${REPAIR_ARR[i+1]}/${REPAIR_ARR[i]}}
    APP_CLASSPATH=${APP_CLASSPATH/$TMP_STR/${REPAIR_ARR[i+1]}}
  done;
fi

# Application options settings.
if [ -z "$APP_OPTS" ]; then
  APP_OPTS="$APP_OPTS --spring.profiles.active=prod"
  APP_OPTS="$APP_OPTS --server.tomcat.basedir=$DATA_DIR"
fi
