package com.xuecheng.manage_cms.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.dao.CmsConfigRepository;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateRepository;
import freemarker.cache.StringTemplateLoader;
import freemarker.core.Configurable;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
public class PageService {

    @Autowired
    private CmsPageRepository cmsPageRepository;

    @Autowired
    private CmsConfigRepository cmsConfigRepository;

    @Autowired
    private CmsTemplateRepository cmsTemplateRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    GridFSBucket gridFSBucket;

    /**
     * 页面列表分页查询
     *
     * @param page             当前页码
     * @param size             页面显示个数
     * @param queryPageRequest 查询条件
     * @return 页面列表
     */
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest) {
        if (queryPageRequest == null) {
            queryPageRequest = new QueryPageRequest();
        }
        //条件匹配器
        //条件名称模糊查询,需要自定义字符串的匹配器实现模糊查询
        ExampleMatcher matcher = ExampleMatcher.matching()
                .withMatcher("pageAliase", ExampleMatcher.GenericPropertyMatchers.contains());
        //条件值
        CmsPage cmsPage = new CmsPage();
        //站点id
        if (StringUtils.isEmpty(queryPageRequest.getSiteId())) {
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        //模板id
        if (StringUtils.isEmpty(queryPageRequest.getTemplateId())) {
            cmsPage.setTemplateId(queryPageRequest.getTemplateId());
        }
        //页面别名
        if (StringUtils.isEmpty(queryPageRequest.getPageAliase())) {
            cmsPage.setTemplateId(queryPageRequest.getPageAliase());
        }
        //创建条件实例
        Example<CmsPage> example = Example.of(cmsPage, matcher);

        if (page < 0) {
            page = 1;
        }
        page = page - 1;
        if (size < 0) {
            size = 10;
        }
        //分页对象
        Pageable pageable = new PageRequest(page, size);
        Page<CmsPage> all = cmsPageRepository.findAll(pageable);
        QueryResult<CmsPage> cmsPageQueryResult = new QueryResult<>();
        cmsPageQueryResult.setList(all.getContent());
        cmsPageQueryResult.setTotal(all.getTotalElements());
        //返回结果
        return new QueryResponseResult(CommonCode.SUCCESS, cmsPageQueryResult);

    }

    //新增页面
    public CmsPageResult add(CmsPage cmsPage) {
        //通过用户输入的条件查询数据库里面的对象
        CmsPage cmsPage1 = cmsPageRepository.findByPageNameAndPageWebPathAndSiteId(cmsPage.getPageName(), cmsPage.getPageWebPath(), cmsPage.getSiteId());
        if (cmsPage1 != null) {
            //如果页面存在
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }

        //如果不存在则保存到mongodb数据库
        if (cmsPage1 == null) {
            cmsPage.setPageId(null);//添加页面主键由spring data 自动生成
            cmsPageRepository.save(cmsPage);
            //返回结果
            return new CmsPageResult(CommonCode.SUCCESS, cmsPage);
        }
        return new CmsPageResult(CommonCode.FAIL, null);

    }

    public CmsPage findById(String id) {
        Optional<CmsPage> optional = cmsPageRepository.findById(id);
        if (optional.isPresent()) {
            CmsPage cmsPage = optional.get();
            return cmsPage;
        }
        //如果查询不到就返回空
        return null;

    }

    public CmsPageResult update(String id, CmsPage cmsPage) {
        CmsPage one = this.findById(id);
        if (one != null) {
            one.setTemplateId(cmsPage.getTemplateId());
            //更新所属站点
            one.setSiteId(cmsPage.getSiteId());
            //更新页面别名
            one.setPageAliase(cmsPage.getPageAliase());
            //更新页面名称
            one.setPageName(cmsPage.getPageName());
            //更新访问路径
            one.setPageWebPath(cmsPage.getPageWebPath());
            //更新物理路径
            one.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
            //更新dataUrl
            one.setDataUrl(cmsPage.getDataUrl());
            CmsPage save = cmsPageRepository.save(one);
            if (save != null) {//返回成功
                return new CmsPageResult(CommonCode.SUCCESS, save);
            }


        }
        //返回失败
        return new CmsPageResult(CommonCode.FAIL, null);
    }

    /**
     * 删除页面
     *
     * @param id
     * @return
     */
    public ResponseResult delete(String id) {
        CmsPage cmsPage = this.findById(id);
        if (cmsPage != null) {
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }


    //根据id查询配置管理信息
    public CmsConfig getConfigById(String id) {
        Optional<CmsConfig> optional = cmsConfigRepository.findById(id);
        if (optional.isPresent()) {
            CmsConfig cmsConfig = optional.get();
            return cmsConfig;
        }
        return null;
    }


    //页面静态化
    public String getPageHtml(String pageId) {
        //获取数据模型
        Map model = getModelByPageId(pageId);
        if (model == null) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }
        String template = getTemplateByPageId(pageId);
        if (StringUtils.isEmpty(template)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //执行静态化
        String html = generateHtml(template, model);
        return html;
    }

    //执行静态化
    private String generateHtml(String templateContent, Map model) {
        //创建配置对象
        Configuration configurable = new Configuration(Configuration.getVersion());
        //创建模板加载器
        StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
        stringTemplateLoader.putTemplate("template", templateContent);
        //像configurable配置模板加载器
        configurable.setTemplateLoader(stringTemplateLoader);
        //调用api进行静态化
        try {
            Template template = configurable.getTemplate("template");
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    //获取页面模板信息
    private String getTemplateByPageId(String pageId) {
        //取出页面的信息
        CmsPage cmsPage = this.findById(pageId);
        if (cmsPage == null) {
            //页面不存在
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }

        //获取模板id
        String templateId = cmsPage.getTemplateId();
        if (StringUtils.isEmpty(templateId)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //查询模板信息
        Optional<CmsTemplate> optional = cmsTemplateRepository.findById(templateId);
        if (optional.isPresent()) {
            CmsTemplate cmsTemplate = optional.get();
            //获取模板文件id
            String templateFileId = cmsTemplate.getTemplateFileId();

            GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
            //打开下载流对象
            GridFSDownloadStream stream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
            //创建gridFsResource，用于获取流对象
            GridFsResource resource = new GridFsResource(gridFSFile, stream);
            //获取流中的数据
            String s = null;
            try {
                s = IOUtils.toString(resource.getInputStream());
                return s;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return null;


    }

    //获取数据模型
    private Map getModelByPageId(String pageId) {
        //取出页面信息
        CmsPage cmsPage = this.findById(pageId);
        if (cmsPage == null) {
            //页面不存在
            ExceptionCast.cast(CmsCode.CMS_PAGE_NOTEXISTS);
        }

        //取出页面的dataURL
        String dataUrl = cmsPage.getDataUrl();
        if (StringUtils.isEmpty(dataUrl)) {
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        //通过restTemplate请求dataurl资源
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        Map body = forEntity.getBody();
        return body;

    }
}
