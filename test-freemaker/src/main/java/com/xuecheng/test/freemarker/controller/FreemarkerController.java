package com.xuecheng.test.freemarker.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Controller
@RequestMapping("/freemarker")
public class FreemarkerController {
    @Autowired
    private RestTemplate restTemplate;

    @RequestMapping("/test1")
    public String test1(Map<String,Object> map){
        map.put("name","小陈");
        return "test1";
    }

    @RequestMapping("/banner")
    public String indexBanner(Map<String,Object> map){
        ResponseEntity<Map> forEntity = restTemplate.getForEntity("http://localhost:31001/getModel/5a791725dd573c3574ee333f", Map.class);
        Map body = forEntity.getBody();
        map.putAll(body);
        return "index_banner";

    }
}
