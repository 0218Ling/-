

package net.jjjshop.front.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import net.jjjshop.common.entity.user.User;
import net.jjjshop.common.enums.SettingEnum;
import net.jjjshop.common.util.SettingUtils;
import net.jjjshop.framework.common.api.ApiResult;
import net.jjjshop.framework.log.annotation.OperationLog;
import net.jjjshop.front.param.order.CategoryListParam;
import net.jjjshop.front.service.supplier.SupplierService;
import net.jjjshop.front.service.page.PageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Api(value = "index", tags = {"index"})
@RestController
@RequestMapping("/front/index")
public class IndexController extends BaseController {

    @Autowired
    private PageService pageService;
    @Autowired
    private SettingUtils settingUtils;
    @Autowired
    private SupplierService supplierService;


    @RequestMapping(value = "/index", method = RequestMethod.GET)
    @OperationLog(name = "index")
    @ApiOperation(value = "index", response = String.class)
    public ApiResult<Map<String, Object>> index(Integer pageId) {
        Map<String, Object> result = new HashMap<>();
        User user = this.getUser(false);
        result.put("page", pageService.getPageData(user, pageId,10));
        CategoryListParam param = new CategoryListParam();
        result.put("supplier", supplierService.getDetail(param));
        result.put("user", user);
        return ApiResult.ok(result);
    }

    @RequestMapping(value = "/nav", method = RequestMethod.GET)
    @OperationLog(name = "nav")
    @ApiOperation(value = "nav", response = String.class)
    public ApiResult<Map<String, Object>> nav() {
        Map<String, Object> result = new HashMap<>();
        result.put("theme", settingUtils.getShopSetting(SettingEnum.THEME.getKey(), null));
        return ApiResult.ok(result);
    }

    @RequestMapping(value = "/userInfo", method = RequestMethod.GET)
    @OperationLog(name = "userInfo")
    @ApiOperation(value = "userInfo", response = String.class)
    public ApiResult<User> userInfo(){
        return ApiResult.ok(this.getUser(false));
    }

    //登录设置
    @RequestMapping(value = "/loginSetting", method = RequestMethod.GET)
    @OperationLog(name = "loginSetting")
    @ApiOperation(value = "loginSetting", response = String.class)
    public ApiResult<JSONObject> loginSetting(){
        JSONObject vo = settingUtils.getShopSetting(SettingEnum.STORE.getKey(), null);
        return ApiResult.ok(vo);
    }
}
