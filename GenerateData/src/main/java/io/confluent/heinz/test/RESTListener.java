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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/")
class restController {


    private final Log logger = LogFactory.getLog(getClass());
    private int counter = 0;

    @Value("${server.port")
    private String PORT;

    @Autowired
    private StreamBridge streamBridge;


    @PostMapping("/test")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void postMessage(@RequestBody JsonMsg request,
                            HttpServletRequest httpRequest) {

        counter = counter + 1;

        //Create a JSON string from JsonMsg POJO object
        String JsonStr = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonStr = mapper.writeValueAsString(request);
        } catch (JsonProcessingException je){
            System.out.println("JSON Error: \n:");
            je.printStackTrace();
        }
        //System.out.println("JSON POST Request: " + JsonStr);
        logger.info(String.format("JSON REST POST Data -> %s " , JsonStr));

        // Create avroMsg Object that will be sent to Kafka Topic
        // defined in toStream-out-o definitions in application.yaml
        avroMsg msg = new avroMsg();
        msg.setFirstName(request.getFirstName());
        msg.setLastName(request.getLastName());
        msg.setCustomerId(request.getCustomerId() + counter);

        //use header to set record key, see application.yaml
        Message<avroMsg> mb = MessageBuilder.withPayload(msg)
                //.setHeader("recKey", request.getCustomerId().toString())
                .setHeader("recKey", String.valueOf(msg.getCustomerId()))
                .build();

        System.out.print("Key: " + String.valueOf(msg.getCustomerId()));
        // Bind to output reference without Source producer function to send to kafka
        //streamBridge.send("toStream-out-0", msg);
        streamBridge.send("toStream-out-0", mb);
    }


}
