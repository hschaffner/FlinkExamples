FROM confluentinc/cp-flink:1.19.1-cp2-java17

RUN mkdir -p /opt/flink/usrlib

COPY target/SimpleConfluentFlinkSR1_19-1.0.jar /opt/flink/usrlib/job.jar
COPY externalJars/flink-connector-files-1.19.1.jar /opt/flink/usrlib/flink-connector-files-1.19.1.jar
