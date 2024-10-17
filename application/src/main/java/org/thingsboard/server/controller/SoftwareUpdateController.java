package org.thingsboard.server.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.server.queue.util.TbCoreComponent;

/**
 * @Description:
 * @Author: 许鑫
 * @Date: 2024/10/17
 */
@RestController
@TbCoreComponent
@RequestMapping("/api")
public class SoftwareUpdateController {

    @RequestMapping(value = "update", method = RequestMethod.GET)
    @ResponseBody
    public String update() {
        String url = "http://localhost:8090/api/update";
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }
}
