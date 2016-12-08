#!/bin/bash

RESTART_FILE='config/mmcreboot/restart.txt'
RESTART_MTIME=0
if [ -f "${RESTART_FILE}" ]; then
	RESTART_MTIME=`stat -c %Y "${RESTART_FILE}"`
fi

echo "[MMCRestartHelper] Executing: '$@'"
while true; do
	$@
	
	LAST_RESTART_MTIME=$RESTART_MTIME
	if [ -f "${RESTART_FILE}" ]; then
		RESTART_MTIME=`stat -c %Y "${RESTART_FILE}"`
	else
		RESTART_MTIME=0
	fi
	
	if [ $RESTART_MTIME -le $LAST_RESTART_MTIME ]; then
		break
	fi
done