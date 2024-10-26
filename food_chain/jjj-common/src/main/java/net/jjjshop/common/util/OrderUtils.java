package net.jjjshop.common.util;

import cn.binarywang.wx.miniapp.api.WxMaOrderShippingService;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.shop.request.shipping.*;
import cn.binarywang.wx.miniapp.bean.shop.response.WxMaOrderShippingInfoBaseResponse;
import cn.binarywang.wx.miniapp.bean.shop.response.WxMaOrderShippingIsTradeManagedResponse;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import net.jjjshop.common.entity.app.AppWx;
import net.jjjshop.common.entity.order.*;
import net.jjjshop.common.entity.supplier.Supplier;
import net.jjjshop.common.entity.user.User;
import net.jjjshop.common.entity.user.UserAddress;
import net.jjjshop.common.enums.OrderTypeEnum;
import net.jjjshop.common.enums.SettingEnum;
import net.jjjshop.common.factory.product.vo.UpdateProductStockVo;
import net.jjjshop.common.service.app.AppWxService;
import net.jjjshop.common.service.order.*;
import net.jjjshop.common.service.supplier.SupplierService;
import net.jjjshop.common.service.user.UserAddressService;
import net.jjjshop.common.service.user.UserService;
import net.jjjshop.common.settings.vo.StoreVo;
import net.jjjshop.common.settings.vo.TradeVo;
import net.jjjshop.common.util.wx.AppWxUtils;
import net.jjjshop.common.vo.order.OrderVo;
import net.jjjshop.framework.common.exception.BusinessException;
import net.jjjshop.framework.core.util.RequestDetailThreadLocal;
import net.jjjshop.framework.util.PhoneUtil;
import org.apache.commons.lang3.StringUtils;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.function.SupplierUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderUtils {
    @Autowired
    private OrderProductService orderProductService;
    @Autowired
    private SettingUtils settingUtils;
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserUtils userUtils;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private OrderAddressService orderAddressService;
    @Autowired
    private AppWxService appWxService;
    @Autowired
    private WxMaService wxMaService;
    @Autowired
    private AppWxUtils appWxUtils;
    @Autowired
    private OrderDeliverService orderDeliverService;
    @Autowired
    private UserAddressService userAddressService;
    /**
     * 生成订单号
     * @return
     */
    public static String geneOrderNo(Integer userId){

        String date = DateUtil.format(new Date(), "yyyyMMdd");

        Random random = new Random();

        int rannum = (int) (random.nextDouble() * (9999 - 1000 + 1)) + 1000;// 获取4位随机数

        //8位用户id
        int subStrLength = 8;
        String sUserId = userId.toString();
        int length = sUserId.length();
        String str;
        if (length >= subStrLength) {
            str = sUserId.substring(length - subStrLength, length);
        } else {
            str = String.format("%0" + subStrLength + "d", userId);
        }

        return date + str + rannum;// 当前时间 + 用户id + 随机数
    }

    /**
     * 设置订单状态
     * @param order
     */
    public static String getOrderStatusText(Order order){
        //订单状态10=>进行中，20=>已经取消，30=>已完成
        if(order.getOrderStatus() == 20){
            return "已取消";
        }
        if(order.getOrderStatus() == 30){
            return "已完成";
        }
        //付款状态(10未付款 20已付款)
        if(order.getPayStatus() == 10){
            return "待付款";
        }
        //配送方式(10外卖配送 20上门取30打包带走40店内就餐
        if(order.getDeliveryType() == 10){
            // 发货状态(10未发货 20已发货)
            if (order.getDeliveryStatus() == 10) {
                return "待配送";
            }
            //收货状态(10未收货 20已收货)
            if (order.getReceiptStatus() == 10) {
                return "配送中";
            }
        }else {
            return "进行中";
        }
        return "";
    }

    /**
     * 通过订单id，查询商品
     * @return
     */
    public List<UpdateProductStockVo> getOrderProduct(Integer orderId){
        List<OrderProduct> list = orderProductService.list(new LambdaQueryWrapper<OrderProduct>().eq(OrderProduct::getOrderId, orderId));
        // 转成vo
        return list.stream().map(e -> {
            UpdateProductStockVo vo = new UpdateProductStockVo();
            BeanUtils.copyProperties(e, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 订单配送
     */
    public void sendOrder(Order order){
        this.sendLocal(order);
    }

    /**
     * 商家配送
     */
    public void sendLocal(Order order){
        //配送状态，待接单＝1,待取货＝2,配送中＝3,已完成＝4
        order.setDeliverStatus(3);
        //10商家配送20达达30配送员
        order.setDeliverSource(10);
        //发货状态(10未发货 20已发货)
        order.setDeliveryStatus(20);
        orderService.updateById(order);

        Supplier supplier = supplierService.getById(order.getShopSupplierId());
        OrderDeliver orderDeliver = new OrderDeliver();
        BeanUtils.copyProperties(order,orderDeliver);
        //配送公司(10商家配送20达达30配送员)
        orderDeliver.setDeliverSource(10);
        orderDeliver.setPrice(BigDecimal.ZERO);
        //配送状态(待接单＝1,待取货＝2,配送中＝3,已完成＝4,已取消＝5)
        orderDeliver.setDeliverStatus(3);
        orderDeliver.setPhone(supplier.getLinkPhone());
        UserAddress address = userAddressService.getById(userService.getById(order.getUserId()).getAddressId());
        if(address != null){
            //配送距离
            orderDeliver.setDistance(getDistance(supplier,address.getLongitude(),address.getLatitude()).intValue());
        }else {
            orderDeliver.setDistance(0);
        }
        orderDeliverService.save(orderDeliver);
    }

    //距离门店距离
    public Double getDistance(Supplier supplier,String longitude,String latitude){
        if(StringUtils.isEmpty(longitude) || StringUtils.isEmpty(latitude)){
            return 0.0;
        }
        if(StringUtils.isEmpty(supplier.getLongitude()) || StringUtils.isEmpty(supplier.getLatitude())){
            return 0.0;
        }
        //门店坐标经度
        double ulon = Double.parseDouble(supplier.getLongitude());
        //门店坐标纬度
        double ulat = Double.parseDouble(supplier.getLatitude());
        //用户坐标经度
        double slon = Double.parseDouble(longitude);
        //用户坐标纬度
        double slat = Double.parseDouble(latitude);
        GlobalCoordinates source = new GlobalCoordinates(ulon, ulat);
        GlobalCoordinates target = new GlobalCoordinates(slon, slat);
        //创建GeodeticCalculator，调用计算方法，传入坐标系、经纬度用于计算距离
        GeodeticCurve geoCurve = new GeodeticCalculator().calculateGeodeticCurve(Ellipsoid.Sphere, source, target);
        return geoCurve.getEllipsoidalDistance();
    }

    /**
     * 订单完成
     */
    public void complete(List<Order> orderList, Integer appId){
        this.settled(orderList);
    }

    /**
     * 执行订单结算
     */
    @Transactional(rollbackFor = Exception.class)
    public void settled(List<Order> orderList)
    {
        // 累积用户实际消费金额
        this.setIncUserData(orderList);
        // 订单id集
        List<Integer> orderIds = orderList.stream().map(Order::getOrderId).collect(Collectors.toList());
        // 供应商结算
        this.setIncSupplierMoney(orderList);
        // 将订单设置为已结算
        orderService.update(new LambdaUpdateWrapper<Order>().in(Order::getOrderId, orderIds)
                .set(Order::getIsSettled, 1));
    }

    /**
     * 供应商金额=支付金额-运费
     */
    private void setIncSupplierMoney(List<Order> orderList)
    {
        // 累计消费金额
        Map<Integer,BigDecimal> supplierData = new HashMap<>();
        // 计算并累积实际消费金额(需减去售后退款的金额)
        for (Order order:orderList) {
            if(order.getShopSupplierId() == 0 || order.getIsSettled() == 1){
                continue;
            }
            // 供应价格+运费
            BigDecimal supplierMoney = order.getPayPrice().subtract(order.getRefundMoney());
            //线下支付不累积余额
            if(order.getPayType() == 10 || order.getPayType() == 20){
                if(supplierData.get(order.getShopSupplierId()) == null){
                    supplierData.put(order.getShopSupplierId(), supplierMoney);
                }else{
                    supplierData.put(order.getShopSupplierId(), supplierData.get(order.getShopSupplierId()).add(supplierMoney));
                }
            }
        }
        // 累累积供应商结算金额 (批量)
        supplierData.forEach((key, value) -> {
            supplierService.update(new LambdaUpdateWrapper<Supplier>().eq(Supplier::getShopSupplierId, key)
                    .setSql("`total_money` = `total_money` + " + value)
                    .setSql("`money` = `money` + " + value));
        });
    }

    /**
     * 累积用户实际消费金额/赠送积分
     */
    private void setIncUserData(List<Order> orderList)
    {
        // 累计消费金额
        Map<Integer,BigDecimal> expendMoneyData = new HashMap<>();
        // 计算并累积实际消费金额(需减去售后退款的金额)
        for (Order order:orderList) {
            // 订单实际支付金额
            BigDecimal expendMoney = order.getPayPrice();
            if(expendMoney.compareTo(BigDecimal.ZERO) > 0){
                if(expendMoneyData.get(order.getUserId()) == null){
                    expendMoneyData.put(order.getUserId(), expendMoney);
                }else{
                    expendMoneyData.put(order.getUserId(), expendMoneyData.get(order.getUserId()).add(expendMoney));
                }
            }
        }
        // 累积到会员消费金额
        expendMoneyData.forEach((key, value) -> {
            userService.update(new LambdaUpdateWrapper<User>().eq(User::getUserId, key)
                    .setSql("`expend_money` = `expend_money` + " + value));
        });
    }

    /**
     * 批量获取订单列表
     */
    public List<OrderVo> getListByIds(List<Integer> orderIds){
        List<Order> list = orderService.list(new LambdaQueryWrapper<Order>().in(Order::getOrderId, orderIds).eq(Order::getIsDelete,0));
        List<OrderVo> voList = list.stream().map(e -> {
            OrderVo vo = new OrderVo();
            BeanUtils.copyProperties(e, vo);
            vo.setProduct(orderProductService.list(new LambdaQueryWrapper<OrderProduct>()
                    .eq(OrderProduct::getOrderId, e.getOrderId())));
            return vo;
        }).collect(Collectors.toList());
        return voList;
    }


    /**
     * 获取微信接口调用凭证
     */
    public String getAccessToken(Integer appId) {
        if(appId == null){
            appId = RequestDetailThreadLocal.getRequestDetail().getAppId().intValue();
        }
        AppWx appWx = appWxService.getById(appId);
        if(appWx == null || StringUtils.isEmpty(appWx.getWxappId()) || StringUtils.isEmpty(appWx.getWxappSecret())){
            throw new BusinessException("未设置微信小程序参数");
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=" + appWx.getWxappId() + "&secret=" + appWx.getWxappSecret();
        String loginJson = null;
        try {
            loginJson = HttpUtils.doGet(url);
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException(e.getMessage());
        }
        cn.hutool.json.JSONObject wxAccessToken =  JSONUtil.parseObj(loginJson);
        return wxAccessToken.getStr("access_token");
    }

}
