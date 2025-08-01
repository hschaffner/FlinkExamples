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

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

@Slf4j
public class SimpleFlinkJson1_19
{
    public static void main(String[] args){

        ApplicationProperties appProps = ApplicationProperties.INSTANCE;

        Properties jsonProps = new Properties();
        jsonProps.put("schema.registry.url", appProps.getPropName("schemaRegURL"));

        StreamExecutionEnvironment env;
        try {
            env = StreamExecutionEnvironment.getExecutionEnvironment()
                    .enableCheckpointing(10*1000L); //10 seconds
        } catch (Exception e) {
            System.out.println("======================Threw Exception: " + e.getMessage());
            throw new RuntimeException(e);
        }

        KafkaSource<JsonMsg> sourceJson = KafkaSource.<JsonMsg>builder()
                .setBootstrapServers("kafka.confluent.svc:9071")
                .setTopics(appProps.getPropName("topicSource_JSON"))
                .setGroupId(appProps.getPropName("groupID"))
                .setClientIdPrefix("flinkJsonGroup-ins")
                .setProperty("interceptor.classes", "io.confluent.monitoring.clients.interceptor.MonitoringConsumerInterceptor")
                .setDeserializer(new jsonSchemaDeserializationSchema())
                .setStartingOffsets(OffsetsInitializer.latest())
                .build();

        DataStream<JsonMsg> streamJson = env
                //.fromSource(sourceJson, WatermarkStrategy.noWatermarks(), "Kafka Source JSON")
                .fromSource(sourceJson, WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(10)), "Kafka Source JSON")
        ;

        //Output the stream to the console - basically the equivalent to JsonMsg.toString()
        streamJson
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
