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
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

public class CFLTJsonValueDeserializeSchema implements DeserializationSchema<JsonMsg>, Serializable {

    private transient Deserializer<JsonMsg> jsonSchemaDeserializer;
    private final Properties properties;
    private final String topic;

    //Constructor
    public CFLTJsonValueDeserializeSchema(String topic, Properties properties) {
        this.topic = topic;
        this.properties = properties;
    }

    @Override
    public void open(InitializationContext context) throws Exception {
        // Initialize the Confluent JSON Schema Deserializer
        jsonSchemaDeserializer = new KafkaJsonSchemaDeserializer<>();
        // Convert Properties to Map<String, ?> for Confluent deserializer configuration
        Map<String, String> propsMap = (Map) properties;
        jsonSchemaDeserializer.configure(propsMap, false); // false for value deserialization
    }

    @Override
    public JsonMsg deserialize(byte[] msg) throws IOException {
        if (msg == null) {
            return null;
        }
        // The Confluent KafkaJsonSchemaDeserializer requires the topic name for deserialization
        return jsonSchemaDeserializer.deserialize(topic, msg);
    }

    @Override
    public boolean isEndOfStream(JsonMsg jsonMsg) {
        return false;
    }

    @Override
    public TypeInformation<JsonMsg> getProducedType() {
        return TypeExtractor.getForClass(JsonMsg.class);
    }

    public void close() throws Exception {
        if (jsonSchemaDeserializer != null) {
            jsonSchemaDeserializer.close();
        }
    }
}
