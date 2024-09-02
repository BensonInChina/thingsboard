/**
 * Copyright © 2016-2024 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Objects;

/**
 * @Description: 为服务器提供初始化设备的接口
 * @Author: 许鑫
 * @Date: 2024/9/2
 */
@RestController
@TbCoreComponent
@RequiredArgsConstructor
@RequestMapping("/api")
public class DeviceInitController {

    public static final String FAIL = "fail";
    public static final String REGISTERED = "registered";
    public static final String SUCCESS = "success";
    public static final String SN_NOT_EXIST = "sn_not_exist";


    /**
     * @Description: 注册设备入口
     * @Param:
        * deviceInit: 注册的设备属性
        * header: 用于关联账户设备权限
     * */
    @RequestMapping(value = "/device/register", method = RequestMethod.POST)
    @ResponseBody
    public String registerDevice(@RequestBody DeviceInit deviceInit, @RequestHeader("X-Authorization") String header) {
        String findResult = findDevice(deviceInit.sn);
        if (findResult.equals(FAIL)) {
            return FAIL;
        } else if (findResult.equals(REGISTERED)) {
            return REGISTERED;
        }
        ResponseEntity<String> response = registerLocally(deviceInit, header);
        if (response.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode id = jsonNode.get("id");
                String deviceId = id.get("id").asText();
                deviceInit.setDeviceId(deviceId);
            } catch (JsonProcessingException e) {
                return e.getMessage();
            }
            ResponseEntity<String> result = registerInServer(deviceInit);
            if (Objects.equals(result.getBody(), SUCCESS)) {
                return SUCCESS;
            } else if (Objects.equals(result.getBody(), SN_NOT_EXIST)) {
                return SN_NOT_EXIST;
            } else {
                return FAIL;
            }
        } else {
            return "关联产品失败";
        }
//        return "OK";
    }

    /**
    * @Description: 申请设备序列号接口
    * @Param：无
    * */
    @RequestMapping(value = "/device/applySN", method = RequestMethod.GET)
    @ResponseBody
    public String applySN() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = "http://localhost:8090/api/device/applySN";
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        System.out.println(response.getBody());
        return response.toString();
    }

    @RequestMapping(value = "/device/detach", method = RequestMethod.POST)
    @ResponseBody
    public String detachDevice(@RequestBody String deviceId) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://localhost:8090/api/device/detach";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("deviceId", deviceId);
        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();
    }

    private String findDevice(String sn) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        String url = "http://localhost:8090/api/device/findDevice/" + sn;
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    private ResponseEntity<String> registerLocally(DeviceInit deviceInit, String header) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        String url = "http://localhost:8080/api/device";
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Authorization", header);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", deviceInit.deviceName);
        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);
        return restTemplate.postForEntity(url, entity, String.class);
    }

    private ResponseEntity<String> registerInServer(DeviceInit deviceInit) {
        String url = "http://localhost:8090/api/device/register";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("sn", deviceInit.sn);
        jsonObject.put("deviceName", deviceInit.deviceName);
        jsonObject.put("deviceId", deviceInit.deviceId);
        jsonObject.put("tenantId", deviceInit.tenantId);
        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toString(), headers);
        return restTemplate.postForEntity(url, entity, String.class);
    }

    @Getter
    @Setter
    public static class DeviceInit {
        private String sn;
        private String deviceName;
        private String deviceId;
        private String tenantId;
    }
}
