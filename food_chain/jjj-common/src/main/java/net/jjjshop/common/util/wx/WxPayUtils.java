package net.jjjshop.common.util.wx;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.github.binarywang.wxpay.v3.auth.Verifier;
import lombok.extern.slf4j.Slf4j;
import net.jjjshop.common.entity.app.App;
import net.jjjshop.common.entity.app.AppWx;
import net.jjjshop.common.service.app.AppService;
import net.jjjshop.common.service.app.AppWxService;
import net.jjjshop.framework.common.exception.BusinessException;
import net.jjjshop.framework.core.util.RequestDetailThreadLocal;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 维信小程序工具类
 */
@Slf4j
@Configuration
public class WxPayUtils {

    @Lazy
    @Autowired
    private AppService appService;
    @Lazy
    @Autowired
    private AppWxService appWxService;

    @Bean
    public WxPayService wxPayService() {
        WxPayService wxPayService = new WxPayServiceImpl();
        WxPayConfig payConfig = new WxPayConfig();
        payConfig.setMchId("jjjshop");
        wxPayService.addConfig("jjjshop", payConfig);
        return wxPayService;
    }

    public String getConfig(WxPayService wxPayService, String paySource, Long appId){
        if(appId == null){
            appId = RequestDetailThreadLocal.getRequestDetail().getAppId();
        }
        App app = appService.getById(appId);
        if(app == null || StringUtils.isEmpty(app.getMchid())){
            throw new BusinessException("未设置微信支付");
        }
        String mchIdKey = "";
        if ("mp".equals(paySource) || "h5".equals(paySource)) {
            mchIdKey = "mp";
        } else if ("wx".equals(paySource)) {
            mchIdKey = "wx";
        } else if ("android".equals(paySource) || "ios".equals(paySource)) {
            mchIdKey = "open";
        }
        mchIdKey = app.getMchid() + "_" + mchIdKey;
        try{
            wxPayService.switchoverTo(mchIdKey);
        }catch (Exception e){
            WxPayConfig payConfig = new WxPayConfig();
            payConfig.setMchId(StringUtils.trimToNull(app.getMchid()));
            payConfig.setMchKey(StringUtils.trimToNull(app.getApikey()));
            payConfig.setApiV3Key(StringUtils.trimToNull(app.getApikey()));
            payConfig.setSubAppId(null);
            payConfig.setSubMchId(null);
            payConfig.setKeyContent(app.getP12());
            payConfig.setPrivateKeyContent(app.getKeyPem().getBytes());
            payConfig.setPrivateCertContent(app.getCertPem().getBytes());
            // 可以指定是否使用沙箱环境
            payConfig.setUseSandboxEnv(false);
            AppWx appWx = appWxService.getById(appId);
            payConfig.setAppId(StringUtils.trimToNull(appWx.getWxappId()));
            wxPayService.addConfig(mchIdKey, payConfig);
            if(app.getWxPayKind() == 3){
                Verifier verifier = payConfig.getVerifier();
                String serialNo = verifier.getValidCertificate().getSerialNumber().toString(16).toUpperCase();
                if(StringUtils.isEmpty(serialNo)){
                    throw new BusinessException("证书错误，请检查或重试");
                }
                appService.update(new LambdaUpdateWrapper<App>().eq(App::getAppId,app.getAppId()).set(App::getWechatpaySerial, serialNo));
            }

        }
        return mchIdKey;
    }
}
