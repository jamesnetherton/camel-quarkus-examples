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
package org.acme.extraction;

import java.time.Duration;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.ollama.OllamaChatModel;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.component.langchain4j.agent.api.AgentWithMemory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Langchain4jAgentConfiguration {
    public static final String AGENT_MEMORY_ID = "data-extract-agent-memory";
    public static final String AGENT_ID = "data-extract-agent";

    @ConfigProperty(name = "langchain4j.ollama.base-url")
    String baseUrl;

    @ConfigProperty(name = "langchain4j.ollama.chat-model.model-id")
    String chatModelId;

    @Singleton
    @Identifier(AGENT_ID)
    Agent agent() {
        ChatMemoryProvider chatMemoryProvider = new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object o) {
                return MessageWindowChatMemory.builder()
                        .id(AGENT_MEMORY_ID)
                        .maxMessages(1)
                        .build();
            }
        };

        return new AgentWithMemory(new AgentConfiguration()
                .withChatMemoryProvider(chatMemoryProvider)
                .withChatModel(OllamaChatModel.builder()
                        .baseUrl(baseUrl)
                        .topK(1)
                        .topP(0.1)
                        .responseFormat(new ResponseFormat.Builder()
                                .type(ResponseFormatType.JSON)
                                .jsonSchema(JsonSchema.builder()
                                        .name("CustomPojo")
                                        .rootElement(JsonObjectSchema.builder()
                                                .addBooleanProperty("customerSatisfied")
                                                .addStringProperty("customerName")
                                                .addProperty("customerBirthday", JsonObjectSchema.builder()
                                                        .addIntegerProperty("year")
                                                        .addIntegerProperty("month")
                                                        .addIntegerProperty("day")
                                                        .build())
                                                .addStringProperty("summary")
                                                .required("customerSatisfied", "customerName", "customerBirthday", "summary")
                                                .build())
                                        .build())
                                .build())
                        .modelName(chatModelId)
                        .temperature(0.0)
                        .timeout(Duration.ofMinutes(3))
                        .build()));
    }
}
