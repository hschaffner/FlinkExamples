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
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializerConfig;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

public class JsonAvroDeserializationSchema implements KafkaRecordDeserializationSchema<Tuple2<avroMsgK, JsonMsg>>, Serializable {
    private CFLTAvroKeyDeserializeSchema avroKeyDeserializer;
    private CFLTJsonValueDeserializeSchema jsonValueDeserializer;
    private final SimpleFlinkAvroJson1_19.ApplicationProperties appProps = SimpleFlinkAvroJson1_19.ApplicationProperties.INSTANCE;


    @Override
    public void open(DeserializationSchema.InitializationContext context) throws Exception {
        Properties avroProps = new Properties();
        avroProps.put("schema.registry.url", appProps.getPropName("schemaRegURL"));
        avroKeyDeserializer = new CFLTAvroKeyDeserializeSchema(appProps.getPropName("topicSource_JSON"), avroProps);
        avroKeyDeserializer.open(context);

        Properties jsonProps = new Properties();
        jsonProps.put("schema.registry.url", appProps.getPropName("schemaRegURL"));
        jsonProps.put(KafkaJsonSchemaDeserializerConfig.JSON_VALUE_TYPE, JsonMsg.class.getName());
        jsonValueDeserializer = new CFLTJsonValueDeserializeSchema(appProps.getPropName("topicSource_JSON"), jsonProps);
        jsonValueDeserializer.open(context);
    }

    @Override
    public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord, Collector<Tuple2<avroMsgK, JsonMsg>> collector) throws IOException {
        avroMsgK deserializedKey = avroKeyDeserializer.deserialize(consumerRecord.key());
        JsonMsg deserializedValue = jsonValueDeserializer.deserialize(consumerRecord.value());
        collector.collect(Tuple2.of(deserializedKey, deserializedValue));
    }

    @Override
    public TypeInformation<Tuple2<avroMsgK, JsonMsg>> getProducedType() {
        return TypeInformation.of(new TypeHint<Tuple2<avroMsgK, JsonMsg>>() {
        });
    }

    public void close() throws Exception {
        if (jsonValueDeserializer != null) {
            jsonValueDeserializer.close();
        }
    }
}
