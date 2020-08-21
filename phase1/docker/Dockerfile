FROM openjdk:14-slim-buster

RUN adduser --disabled-password --gecos "" --home /opt/teku teku && \
    chown teku:teku /opt/teku

USER teku
WORKDIR /opt/teku

# copy application (with libraries inside)
COPY --chown=teku:teku eth2-client /opt/teku/

# specify default command
ENTRYPOINT ["/opt/teku/bin/eth2-client"]
