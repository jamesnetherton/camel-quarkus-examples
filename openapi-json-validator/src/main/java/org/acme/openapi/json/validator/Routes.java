/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.acme.openapi.json.validator;

import io.quarkus.runtime.LaunchMode;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jsonvalidator.JsonValidationException;
import org.apache.camel.model.rest.RestBindingMode;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

@ApplicationScoped
public class Routes extends RouteBuilder {
    private Set<Fruit> fruits = Collections.newSetFromMap(Collections.synchronizedMap(new LinkedHashMap<>()));

    @Override
    public void configure() throws Exception {
        restConfiguration().bindingMode(RestBindingMode.json);

        rest()
                .post("/api/gateway")
                .bindingMode(RestBindingMode.off)
                .to("direct:validate")

                .get("/fruits")
                .to("direct:getFruits")

                .post("/fruits")
                .type(Fruit.class)
                .to("direct:addFruit");

        from("direct:validate")
                .removeHeaders("CamelHttp*")
                .doTry()
                    .to("json-validator:fruit-schema.json")
                    .toF("rest-openapi:#put?specificationUri=classpath:demo-api-spec.json&host=RAW(http://localhost:%d)", resolvePort())
                .doCatch(JsonValidationException.class)
                    .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(400)
                    .setBody().simple("${exception.message}");

        from("direct:getFruits")
                .setBody().constant(fruits);

        from("direct:addFruit")
                .process().body(Fruit.class, fruits::add)
                .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(201);

    }

    int resolvePort() {
        String portPropName = LaunchMode.current().isDevOrTest() ? "quarkus.http.test-port" : "quarkus.http.port";
        return ConfigProvider.getConfig().getValue(portPropName, int.class);
    }
}
