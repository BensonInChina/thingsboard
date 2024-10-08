/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
@Slf4j
public class myRegister {
    private static final String userName = "sysadmin@thingsboard.org";
    private static final String password = "sysadmin";
    private static String token = "default";
    public static String refreshToken = "default";
    public static final String SERVER_URL = "http://localhost:8080";
    private static String tenantId;

    @RequestMapping(value = "/doRegister/getVerificationCode", method = RequestMethod.POST)
    @ResponseBody
    public String getVerificationCode(@RequestBody String email) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8090/api/register/getVerificationCode";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        System.out.println(email);
        body.put("email", email);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        ResponseEntity<String> exchange = restTemplate.postForEntity(url, entity, String.class);
        return exchange.getBody();
    }

    @RequestMapping(value = "/doRegister/verify", method = RequestMethod.POST)
    @ResponseBody
    public String verify(@RequestBody String json) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8090/api/register/verify";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        System.out.println(json);
        body.put("json", json);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        ResponseEntity<String> exchange = restTemplate.postForEntity(url, entity, String.class);
        System.out.println(exchange.getBody());
        return exchange.getBody();
    }

    @RequestMapping(value = "/doRegister/do", method = RequestMethod.POST)
    @ResponseBody
    public String doRegister(@RequestBody RegisterData registerData) {
        String response = loginAsSysAdmin();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            token = rootNode.get("token").asText();
            refreshToken = rootNode.get("refreshToken").asText();
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        String replyStr = createEnterprise(registerData);
        try {
            JsonNode reply = objectMapper.readTree(replyStr);
            tenantId = reply.get("id").get("id").asText();
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        String result = postForRegister(registerData);
        System.out.println(result);
        String link = getForEmailLink(result);
        System.out.println(link);
        String tokenStart = "activateToken=";
        String activateToken = link.substring(link.indexOf(tokenStart) + tokenStart.length());
        System.out.println(activateToken);
        return activate(registerData, activateToken);
    }

    private String activate(RegisterData registerData, String activateToken) {
        String url = SERVER_URL + "/api/noauth/activate?sendActivationMail=false";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        body.put("activateToken", activateToken);
        body.put("password", registerData.getPassword());
        HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
        return response.getBody();
    }

    private String getForEmailLink(String result) {
        ObjectMapper objectMapper = new ObjectMapper();
        String id;
        try {
            JsonNode rootNode = objectMapper.readTree(result);
            JsonNode idNode = rootNode.get("id");
            id = idNode.get("id").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String url = SERVER_URL + "/api/user/" + id + "/activationLink";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Authorization", "Bearer " + token);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            return response.getStatusCode().toString();
        }
    }

    private String loginAsSysAdmin() {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER_URL + "/api/auth/login";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject json = new JSONObject();
        json.put("username", userName);
        json.put("password", password);
        HttpEntity<String> postRequest = new HttpEntity<>(json.toString(), headers);
        ResponseEntity<String> exchange = restTemplate.postForEntity(url, postRequest, String.class);
        return exchange.getBody();
    }

    private String createEnterprise(RegisterData registerData) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER_URL + "/api/tenant";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Authorization", "Bearer " + token);
        JSONObject json = new JSONObject();
        json.put("title", registerData.getEnterprise());
        HttpEntity<String> request = new HttpEntity<>(json.toString(), headers);
        ResponseEntity<String> exchange = restTemplate.postForEntity(url, request, String.class);
        return exchange.getBody();
    }

    private String postForRegister(RegisterData registerData) {
        RestTemplate restTemplate = new RestTemplate();
        String url = SERVER_URL + "/api/user?sendActivationMail=false";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Authorization", "Bearer " + token);
        HttpEntity<String> postRequest = getStringHttpEntity(registerData, headers);
        ResponseEntity<String> exchange = restTemplate.postForEntity(url, postRequest, String.class);
        return exchange.getBody();
    }

    @NotNull
    private static HttpEntity<String> getStringHttpEntity(RegisterData registerData, HttpHeaders headers) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = objectMapper.createObjectNode();
        ObjectNode idNode = objectMapper.createObjectNode();
        idNode.put("id", tenantId);
        idNode.put("entityType", "TENANT");
        rootNode.set("tenantId", idNode);
        rootNode.put("email", registerData.getUsername());
        rootNode.put("authority", "TENANT_ADMIN");
        rootNode.put("firstName", registerData.getName());
        rootNode.put("lastName", registerData.getName());
        rootNode.put("phone", registerData.getPhone());
        return new HttpEntity<>(rootNode.toString(), headers);
    }

    @Setter
    @Getter
    public static class RegisterData {
        private String enterprise;
        private String username;
        private String password;
        private String name;
        private String phone;

        @Override
        public String toString() {
            return "RegisterData{" +
                    "username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    ", name='" + name + '\'' +
                    ", phone='" + phone + '\'' +
                    '}';
        }
    }
}
