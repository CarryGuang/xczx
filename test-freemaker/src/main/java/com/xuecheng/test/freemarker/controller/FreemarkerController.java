package com.xuecheng.test.freemarker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/freemarker")
public class FreemarkerController {

    @RequestMapping("/test1")
    public String test1(Map<String,Object> map){
        map.put("name","小陈");
        return "test1";
    }
}
