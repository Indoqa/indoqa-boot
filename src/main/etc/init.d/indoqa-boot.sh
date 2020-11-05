#!/bin/sh
# Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
# one or more contributor license agreements. See the NOTICE file distributed
# with this work for additional information regarding copyright ownership.
# Indoqa licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

### BEGIN INIT INFO
# Provides:          indoqa-boot
# Required-Start:    $local_fs $network
# Required-Stop:     $local_fs $network
# Should-Start:      $syslog
# Should-Stop:       $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Manage Indoqa-Boot.
# Description:       Manage Indoqa-Boot.
### END INIT INFO

if [ -e /etc/debian_version ]; then
    . /lib/lsb/init-functions
elif [ -e /etc/init.d/functions ] ; then
    . /etc/init.d/functions
fi

export JAVA_HOME=/srv/j2sdk/archive/jdk1.8.0/current

RUN_AS=user

BASE_PATH=/working/directory/

NAME="Indoqa-Boot"

OPTIONS=$OPTIONS" -Dfile.encoding=UTF-8"
OPTIONS=$OPTIONS" -Dlog-path=/path/to/logs"

RUNNABLE=$BASE_PATH/indoqa-boot-main-runnable.jar

getPid(){
  PID=`pgrep -u $RUN_AS -f $RUNNABLE`
}

start(){
  sudo -u $RUN_AS $JAVA_HOME/bin/java $OPTIONS -jar $RUNNABLE &
  RETURN=$?
}

status(){
  getPid
  if [ "$PID" != "" ]; then
    log_daemon_msg "$NAME is running with PID:" "$PID"
    RETURN=0
    return
  fi

  log_failure_msg "$NAME is NOT running."
  RETURN=3
}

stop(){
  getPid
  if [ "$PID" != "" ]; then
    sudo -u $RUN_AS kill $PID

    sleep 1
    getPid
    if [ "$PID" != "" ]; then
      echo "Process did not stop. Killing it."
      sudo -u $RUN_AS kill -9 $PID

      sleep 1
      getPid
      if [ "$PID" != "" ]; then
        echo "Process did not stop after killing."
        RETURN=1
        return
      fi
    fi

    RETURN=0
  fi
}

case "$1" in
 start)
        status

        if [ $RETURN -ne 3 ]; then
          log_warning_msg "$NAME is already running."
          exit 0;
        fi

        start

        if [ $RETURN -ne 0 ]; then
          echo "Error starting $NAME. Exit value $RETURN"
        fi

        echo "$NAME started."
        exit $RETURN
    ;;
 stop)
        status
        if [ $RETURN -ne 0 ]; then
          log_warning_msg "$NAME is already stopped."
          exit 0;
        fi

        stop

        if [ $RETURN -ne 0 ]; then
          echo "Error stopping $NAME. Exit value $RETURN"
        fi

        echo "$NAME stopped"
        exit $RETURN
     ;;
 restart)
        RETURN=-1

        stop
        if [ $RETURN -ne 0 ]; then
          echo "Error stopping $NAME. Exit value $RETURN"
        fi

        start
        if [ $RETURN -ne 0 ]; then
          echo "Error starting $NAME. Exit value $RETURN"
        fi

        echo "$NAME restarted."
        exit $RETURN
        ;;

 status)
        status
        exit $RETURN
        ;;
 *)
        echo "Usage: $0 {start|stop|restart|status}"
        exit 1
        ;;
esac
