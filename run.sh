#!/bin/bash

CONFIG=$1
CONTAINER_PREFIX=$2

if [ ! -f "${CONFIG}" ]
then
	CONTAINER_PREFIX="apm-game"
	CONFIG=config.yml
fi

if [ -z "${CONTAINER_PREFIX}" ]
then
	CONTAINER_PREFIX=`basename ${CONFIG%.yml}`
fi

IMAGE_PREFIX="apm-game/"
DOCKER_NETWORK="${CONTAINER_PREFIX}/network"

IS_RUNNING=`docker ps -f name=${CONTAINER_PREFIX} -q`

if [ -n "${IS_RUNNING}" ]
then
	echo "${CONTAINER_PREFIX} is already in use. If you want to run the same application twice, provide a container prefix as second parameter:"
	echo -e "\n\t${0} ${1} ${CONTAINER_PREFIX}-2\n"
	exit
fi

(
  cd master/ || exit
  npm install
);

for DIR in nodes/*;
do
  if [ -d $DIR ] ; then
    echo "Building ${IMAGE_PREFIX}`basename $DIR`..."
    docker build -t "${IMAGE_PREFIX}`basename $DIR`" $DIR;
  fi
done;

for DIR in loaders/*;
do
  if [ -d $DIR ] ; then
    docker build -t "${IMAGE_PREFIX}`basename $DIR`" $DIR;
  fi
done;

docker build -t "${IMAGE_PREFIX}machine" machine;

docker network create ${DOCKER_NETWORK}

node master/index.js ${CONFIG} ${IMAGE_PREFIX} ${DOCKER_NETWORK} ${CONTAINER_PREFIX}

docker network rm ${DOCKER_NETWORK}
