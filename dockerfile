FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive

ARG APP_DIR=kimlongdaechicken
ENV APP_DIR=${APP_DIR}
ENV APP_PATH=/${APP_DIR}

ENV 
RUN apt-get update && apt-get install -y git curl net-tools tzdata

RUN ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
  && echo Asia/Seoul > /etc/timezone


COPY . ${APP_PATH}



RUN bash ${APP_PATH}/setting-script/install_vue.sh
