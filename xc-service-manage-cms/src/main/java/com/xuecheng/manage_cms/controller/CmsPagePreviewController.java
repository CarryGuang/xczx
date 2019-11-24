package com.xuecheng.manage_cms.controller;

import com.xuecheng.framework.web.BaseController;
import com.xuecheng.manage_cms.service.PageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.ServletOutputStream;
import java.io.IOException;

@Controller
public class CmsPagePreviewController extends BaseController {
    @Autowired
    private PageService pageService;


    //接受到页面id
    @RequestMapping(value = "",method = RequestMethod.GET)
    public void  preview(@PathVariable("pageId")String pageId)  {
        String pageHtml = pageService.getPageHtml(pageId);
        if (StringUtils.isNoneEmpty(pageHtml)){
            try {
                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(pageHtml.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
