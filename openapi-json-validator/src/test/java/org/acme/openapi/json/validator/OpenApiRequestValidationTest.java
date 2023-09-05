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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@QuarkusTest
public class OpenApiRequestValidationTest {

    @Test
    void validRequest() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Lemon\",\"description\":\"Yellow fruit\"}")
                .post("/api/gateway")
                .then()
                .statusCode(201);

        RestAssured.get("/fruits")
                .then()
                .statusCode(200)
                .body(
                        "name[0]", is("Lemon"),
                        "description[0]", is("Yellow fruit")
                );
    }

    @Test
    void invalidRequest() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"bad-name\":\"Bad Name\",\"bad-description\":\"Bad Description\"}")
                .post("/api/gateway")
                .then()
                .statusCode(400)
                .body(containsString("name: is missing"), containsString("description: is missing"));
    }
}
