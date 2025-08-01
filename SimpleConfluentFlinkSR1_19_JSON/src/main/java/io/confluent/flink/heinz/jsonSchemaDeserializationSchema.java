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
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public  class jsonSchemaDeserializationSchema implements KafkaRecordDeserializationSchema<JsonMsg>, Serializable {

    transient KafkaJsonSchemaDeserializer<JsonMsg> jsonDeserializer;
    SimpleFlinkJson1_19.ApplicationProperties appProps = SimpleFlinkJson1_19.ApplicationProperties.INSTANCE;

    //constructor
    public jsonSchemaDeserializationSchema() {
        super();
    }

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord, Collector<JsonMsg> collector) throws IOException {


        //Check if the KafkaJsonSchemaDeserializer object has been created
        if (jsonDeserializer == null) {
            log.info("++++++++++++++++++++++++++++++ Initializing KafkaJsonSchemaDeserializer object");

            Map<String, String> deserializerConfig = new HashMap<>();
            deserializerConfig.put("schema.registry.url", appProps.getPropName("schemaRegURL"));
            deserializerConfig.put(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, JsonMsg.class.getName());

            jsonDeserializer = new KafkaJsonSchemaDeserializer<>();
            jsonDeserializer.configure(deserializerConfig, false);
        }



        JsonMsg deserializedMsg = jsonDeserializer.deserialize(appProps.getPropName("topicSource_JSON"), consumerRecord.value());
        //Flink source connector reads from the values placed in the collector
        collector.collect(deserializedMsg);

    }



    @Override
    public TypeInformation<JsonMsg> getProducedType() {
        return TypeInformation.of(JsonMsg.class);
    }
}
