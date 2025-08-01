/*
 * Copyright Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.confluent.flink.heinz;

import io.confluent.heinz.test.avroMsg;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroSerializationSchema;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;


import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class SimpleFlinkSR1_19
{
    public static void main(String[] args){

        StreamExecutionEnvironment env;
        ApplicationProperties appProps = ApplicationProperties.INSTANCE;
        try {
            env = StreamExecutionEnvironment.getExecutionEnvironment()
                    .enableCheckpointing(10*1000L); //10 seconds
        } catch (Exception e) {
            System.out.println("======================Threw Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }



        Map<String, String> registryMap = Map.of(
                "specific.avro.reader", "true",
                "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"
        );


        KafkaSink<avroMsg> sinkAvro =
                KafkaSink.<avroMsg>builder()
                        .setBootstrapServers("kafka.confluent.svc:9071")
                        .setProperty("interceptor.classes", "io.confluent.monitoring.clients.interceptor.MonitoringProducerInterceptor")
                        //.setDeliveryGuarantee(DeliveryGuarantee.EXACTLY_ONCE)
                        .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                        .setRecordSerializer(
                                KafkaRecordSerializationSchema.builder()
                                        .setValueSerializationSchema(
                                                ConfluentRegistryAvroSerializationSchema
                                                        .forSpecific(
                                                                avroMsg.class,
                                                                "flinkAvroTopicSink",
                                                                appProps.getPropName("schemaRegURL")))
                                        .setTopic(appProps.getPropName("topicSink"))
                                        .build())
                        .build();



        KafkaSource<avroMsg> sourceAvro = KafkaSource.<avroMsg>builder()
                .setBootstrapServers("kafka.confluent.svc:9071")
                .setTopics(appProps.getPropName("topicSource"))
                .setGroupId(appProps.getPropName("groupID"))
                .setClientIdPrefix("flinkAvroGroup-ins")
                .setProperty("interceptor.classes", "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor")
                .setDeserializer(KafkaRecordDeserializationSchema.valueOnly(ConfluentRegistryAvroDeserializationSchema.forSpecific(avroMsg.class, appProps.getPropName("schemaRegURL"), 5, registryMap)))
                .setStartingOffsets(OffsetsInitializer.latest())
                .build();

        //DataStream<avroMsg> streamAvro = env.fromSource(sourceAvro, WatermarkStrategy.noWatermarks(), "Kafka Source Avro").setMaxParallelism(3);
        DataStream<avroMsg> streamAvro = env
                //.fromSource(sourceAvro, WatermarkStrategy.noWatermarks(), "Kafka Source Avro")
                .fromSource(sourceAvro, WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(10)), "Kafka Source Avro")
                //.setMaxParallelism(3)
                .setParallelism(Integer.parseInt(appProps.getPropName("parallelism")))
                ;

        streamAvro.print().setParallelism(1);


        streamAvro.map(avroMsg -> {
            log.info("Avro Message : {}", avroMsg.toString());
            return avroMsg;
        }) .setParallelism(Integer.parseInt(appProps.getPropName("parallelism")))
                .sinkTo(sinkAvro).name("Kafka Sink Avro")
                .setParallelism(2)
            ;




        // Execute the Flink job
        try {
            env.execute("flink-test-sr");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
    public enum ApplicationProperties {
        INSTANCE;

        private final Properties properties;

        ApplicationProperties() {
            properties = new Properties();
            try {
                properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
            } catch (IOException e) {
                //Logger.getLogger(getClass().getName()).log(Level.SEVERE, e.getMessage(), e);
                log.info("Properties Error: {} ", e.getMessage());
            }
        }

        public String getPropName(String key) {
            return properties.getProperty(key);
        }
    }
}
