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

import io.confluent.heinz.test.JsonMsg;

import io.confluent.heinz.test.avroMsgK;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;

import java.time.Duration;
import java.util.Properties;

@Slf4j
public class SimpleFlinkAvroJson1_19 {
    public static void main(String[] args) {

        StreamExecutionEnvironment env;
        ApplicationProperties appProps = ApplicationProperties.INSTANCE;
        try {
            env = StreamExecutionEnvironment.getExecutionEnvironment()
                    .enableCheckpointing(10 * 1000L); //10 seconds
        } catch (Exception e) {
            System.out.println("======================Threw Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }

        KafkaSource<Tuple2<avroMsgK, JsonMsg>> sourceJsonAvro = KafkaSource.<Tuple2<avroMsgK, JsonMsg>>builder()
                .setBootstrapServers("kafka.confluent.svc:9071")
                .setTopics(appProps.getPropName("topicSource_JSON"))
                .setGroupId(appProps.getPropName("groupID"))
                .setClientIdPrefix("flinkAvroJsonGroup-ins")
                .setProperty("interceptor.classes", "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor")
                .setDeserializer(new JsonAvroDeserializationSchema())
                .setStartingOffsets(OffsetsInitializer.latest())
                .build();

        DataStream<Tuple2<avroMsgK, JsonMsg>> streamAvroJson = env
                //.fromSource(sourceJsonAvro, WatermarkStrategy.noWatermarks(), "Kafka Source JSON");
                .fromSource(sourceJsonAvro, WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(10)), "Kafka Source JSON");


        //Output the stream to the console - basically the equivalent to JsonMsg.toString()
        streamAvroJson.map(record -> "Received Key: " + record.f0.toString() +
                        ", Value: " + record.f1.toString())
                .print()
                .setParallelism(3);

        // Execute the Flink job
        try {
            env.execute("flink-test-json-sr");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    //Get all the details from the application.properties file
    public enum ApplicationProperties {
        INSTANCE;

        private final Properties properties;

        ApplicationProperties() {
            properties = new Properties();
            try {
                properties.load(getClass().getClassLoader().getResourceAsStream("application.properties"));
            } catch (IOException e) {
                log.info("Properties Error: {} ", e.getMessage());
            }
        }

        public String getPropName(String key) {
            return properties.getProperty(key);
        }
    }

}
