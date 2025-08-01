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

package io.confluent.heinz.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
@SpringBootApplication
public class AvroInJsonOutTest {

	private final Log logger = LogFactory.getLog(getClass());

	public static void main(String[] args) {
		SpringApplication.run(AvroInJsonOutTest.class,  args);
	}


	@Bean
	public Function<avroMsg, Message<JsonMsg>> inAvroOutJson() {
		return value -> {
			//take avro message from one topic,
			// form JSON Pojo object that is used to send a JSON record to another topic
			JsonMsg msg = new JsonMsg();
			msg.setFirstName(value.getFirstName());
			msg.setLastName(value.getLastName());
			msg.setCustomerId(value.getCustomerId());

			//create the avro key
			avroMsgK avroK = new avroMsgK();
			avroK.setClient("Heinz57");
			avroK.setClientID(value.getCustomerId());


			//use header to set record key, see application.yaml
			Message<JsonMsg> mb = MessageBuilder.withPayload(msg)
					//.setHeader("recKey", Integer.toString(value.getCustomerId()))
					.setHeader("recKey", avroK)
					.build();
			return mb;
			//return msg;
		};
	}

	@Bean
	public Consumer<avroMsg> avroListen() {
		return value -> {
			//System.out.println( "Retrieved: " + value.toString());
			logger.info(String.format("Avro Kafka Record -> %s " , value.toString()));

		};
	}

	@Bean
	public Consumer<JsonMsg> sinkListen() {
		return value -> {
			//System.out.println( "Retrieved: " + value.toString());
			logger.info(String.format("JSON Kafka Record -> %s " , value.toString()));

		};
	}
}
