FROM ubuntu:24.04
ENV DEBIAN_FRONTEND=noninteractive

ARG APP_DIR=kimlongdaechicken
ENV APP_DIR=${APP_DIR}
ENV APP_PATH=/${APP_DIR}

ENV
# 기본도구
RUN apt-get update && apt-get install -y git curl net-tools tzdata ca-certificates

RUN apt-get install -y openjdk-21-jdk

ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=${JAVA_HOME}/bin:${PATH}

# 시간대, 언어 설정
RUN ln -snf /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
  && echo Asia/Seoul > /etc/timezone
RUN locale-gen ko_KR.UTF-8
ENV LANG=ko_KR.UTF-8
ENV LANGUAGE=ko_KR:ko
ENV LC_ALL=ko_KR.UTF-8

COPY . ${APP_PATH}


RUN bash ${APP_PATH}/setting-script/install_vue.sh
