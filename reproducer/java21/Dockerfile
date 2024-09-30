FROM <anyPathToImage>/ubuntu:20.04
# Use zulu jdk
RUN sudo wget <pathto_jdk_tar_file>/filename.tar.gz \
&& sudo tar -xzvf filename.tar.gz \
&& sudo chmod 777 -R /filename \
&& sudo rm filename.tar.gz
WORKDIR /app
ENV PATH="/filename/bin:${PATH}"
COPY target/reproducer-5.1.0.Final.jar .


EXPOSE 8080
EXPOSE 8000

ENTRYPOINT java -Xms2200m -Xmx2200m $JVM_ARGS -jar reproducer-5.1.0.Final.jar  $APP_ARGS > app_logs.log