package net.jjjshop.shop.service.page.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import net.jjjshop.common.entity.page.Page;
import net.jjjshop.common.mapper.page.PageMapper;
import net.jjjshop.common.util.PageUtils;
import net.jjjshop.common.vo.page.PageVo;
import net.jjjshop.config.properties.SpringBootJjjProperties;
import net.jjjshop.framework.common.service.impl.BaseServiceImpl;
import net.jjjshop.framework.core.pagination.PageInfo;
import net.jjjshop.framework.core.pagination.Paging;
import net.jjjshop.common.util.diy.DefaultItems;
import net.jjjshop.shop.param.CommonPageParam;
import net.jjjshop.shop.service.page.PageService;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * diy页面表 服务实现类
 * @author jjjshop
 * @since 2022-07-28
 */
@Slf4j
@Service
public class PageServiceImpl extends BaseServiceImpl<PageMapper, Page> implements PageService {

    /**
     * 列表
     * @param commonPageParam
     * @param pageType
     * @return
     */
    public Paging<Page> getList(CommonPageParam commonPageParam, Integer pageType){
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Page> page = new PageInfo<>(commonPageParam);
        IPage<Page> iPage = this.page(page, new LambdaQueryWrapper<Page>()
                .eq(Page::getIsDelete, false)
                .eq(Page::getPageType, pageType)
                .orderByDesc(Page::getIsDefault)
                .orderByDesc(Page::getCreateTime));
        return new Paging(iPage);
    }

    /**
     * 默认首页
     * @param
     * @return
     */
    public synchronized PageVo getDefault(Integer pageType){
        PageVo vo = new PageVo();
        Page page = this.getOne(new LambdaQueryWrapper<Page>()
                .eq(Page::getPageType, pageType)
                .eq(Page::getIsDelete, 0)
                .eq(Page::getIsDefault, true));
        if(page == null){
            //生成默认页
            page = PageUtils.getPage(pageType);
            int num  = this.count(new LambdaQueryWrapper<Page>()
                    .eq(Page::getPageType, pageType)
                    .eq(Page::getIsDelete, 0)
                    .eq(Page::getIsDefault, true));
            //防止重复
            if(num == 0){
                this.save(page);
            }
        }
        BeanUtils.copyProperties(page, vo);
        vo.setPageDataJson(JSON.parseObject(page.getPageData()));
        vo.setItems((JSONArray) JSON.parseObject(page.getPageData()).get("items"));
        vo.setPage((JSONObject) JSON.parseObject(page.getPageData()).get("page"));
        vo.setPageData("");
        return vo;
    }

    /**
     * 详情
     * @param pageId
     * @return
     */
    public PageVo detail(Integer pageId){
        PageVo vo = new PageVo();
        Page page = this.getById(pageId);
        BeanUtils.copyProperties(page, vo);
        vo.setPageDataJson(JSON.parseObject(page.getPageData()));
        vo.setItems((JSONArray) JSON.parseObject(page.getPageData()).get("items"));
        vo.setPage((JSONObject) JSON.parseObject(page.getPageData()).get("page"));
        vo.setPageData("");
        return vo;
    }

    /**
     * 修改
     * @param pageId
     * @param params
     * @return
     */
    public Boolean edit(Integer pageId, String params){
        Page page = new Page();
        page.setPageId(pageId);
        String jsonText = StringEscapeUtils.unescapeHtml4(params);
        page.setPageData(jsonText);
        // 获取标题
        JSONObject json = JSONObject.parseObject(jsonText);
        if(json.getJSONObject("page").getJSONObject("page") != null){
            page.setPageName(json.getJSONObject("page").getJSONObject("page").getJSONObject("params").getString("name"));
        }else {
            page.setPageName(json.getJSONObject("page").getJSONObject("params").getString("name"));
        }
        return this.updateById(page);
    }

    /**
     * 修改默认首页
     * @param pageId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean setDefaultPage(Integer pageId,Integer pageType){
        // 先设置其他页面非默认
        Page updateBean = new Page();
        updateBean.setIsDefault(false);
        this.update(updateBean, new LambdaQueryWrapper<Page>().eq(Page::getPageType, pageType));
        // 当前设置为默认
        Page page = new Page();
        page.setPageId(pageId);
        page.setIsDefault(true);
        return this.updateById(page);
    }

    /**
     * 新增
     * @param pageType
     * @param params
     * @return
     */
    public Boolean add(Integer pageType, String params){
        Page page = new Page();
        page.setPageType(pageType);
        String jsonText = StringEscapeUtils.unescapeHtml4(params);
        page.setPageData(jsonText);
        // 获取标题
        JSONObject json = JSONObject.parseObject(jsonText);
        page.setPageName(json.getJSONObject("page").getJSONObject("params").getString("name"));
        return this.save(page);
    }

    /**
     * 删除
     * @param pageId
     * @return
     */
    public Boolean setDelete(Integer pageId){
        Page updateBean = new Page();
        updateBean.setPageId(pageId);
        updateBean.setIsDelete(1);
        return this.updateById(updateBean);
    }

    /**
     * 所有
     * @param pageType
     * @return
     */
    public List<Page> getAll(Integer pageType){
        return this.list(new LambdaQueryWrapper<Page>().eq(Page::getIsDelete, 0)
                .eq(Page::getPageType, 20).orderByDesc(Page::getCreateTime));
    }
}
