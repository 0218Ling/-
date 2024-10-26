package net.jjjshop.shop.service.statistics.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.jjjshop.common.entity.shop.ShopUser;
import net.jjjshop.common.entity.supplier.Supplier;
import net.jjjshop.framework.util.ShopLoginUtil;
import net.jjjshop.shop.param.order.OrderPageParam;
import net.jjjshop.shop.service.order.OrderService;
import net.jjjshop.shop.service.shop.ShopUserService;
import net.jjjshop.shop.service.statistics.HomeDataService;
import net.jjjshop.shop.service.statistics.OrderRankingService;
import net.jjjshop.shop.service.supplier.SupplierService;
import net.jjjshop.shop.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

/**
 * 首页统计数据 服务实现类
 * @author jjjshop
 * @since 2022-06-28
 */

@Slf4j
@Service
public class HomeDataServiceImpl implements HomeDataService {

    @Autowired
    private OrderRankingService orderRankingService;
    @Autowired
    private ProductRankingServiceImpl productRankingService;
    @Autowired
    private UserRankingServiceImpl userRankingService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private UserService userService;
    @Autowired
    private ShopUserService shopUserService;
    @Autowired
    private SupplierService supplierService;

    /**
     * 获取首页显示数据
     * @param
     * @return
     */
    public JSONObject getHomeData() throws ParseException {
        JSONObject result = new JSONObject();
        OrderPageParam param = new OrderPageParam();
        ShopUser shopUser = shopUserService.getById(ShopLoginUtil.getUserId());
        //账号类型0总台1门店
        if(shopUser.getUserType() == 1){
            param.setShopSupplierId(shopUser.getShopSupplierId());
        }
        //获取今天时间
        String today = DateUtil.format(DateUtil.offsetDay(new Date(), 0), "yyyy-MM-dd");
        //获取昨天时间
        String yesterday = DateUtil.format(DateUtil.offsetDay(new Date(), -1), "yyyy-MM-dd");
        //获取七天前的时间
        String lately7days = DateUtil.format(DateUtil.offsetDay(new Date(), -7), "yyyy-MM-dd");

        //获取商品销售榜单
        JSONObject topData = new JSONObject();
        // 商品总量
        topData.put("productTotal",productRankingService.getProductTotal());
        // 用户总量
        topData.put("userTotal",userRankingService.getUserTotal());
        // 订单总量
        topData.put("orderTotal",Integer.parseInt(orderService.getOrderData(null, null, "order_total",0,-1).toString()));
        // 店铺总量
        topData.put("supplierTotal",supplierService.count(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getIsDelete,0)));
        // 营业额
        topData.put("totalMoney",orderService.getOrderData(null, null, "order_total_price",0,-1));
        //预计收入
        topData.put("incomeMoney",orderService.getOrderData(null, null, "income_price",0,-1));
        result.put("topData",topData);

        //获取商品销售榜单
        JSONObject waitData = new JSONObject();
        //待处理订单
        waitData.put("orderDisposal",orderService.getReviewOrderTotal());
        //库存告急商品
        waitData.put("stockProduct",productRankingService.getProductStockTotal());
        result.put("waitData",waitData);

        JSONObject todayData = new JSONObject();
        // 销售额(元)
        todayData.put("orderTotalPriceT",orderService.getOrderData(today, null, "order_total_price",0,-1));
        todayData.put("orderTotalPriceY",orderService.getOrderData(yesterday, null, "order_total_price",0,-1));
        // 支付订单数
        todayData.put("orderTotalT",Integer.parseInt(orderService.getOrderData(today, null, "order_total",0,-1).toString()));
        todayData.put("orderTotalY",Integer.parseInt(orderService.getOrderData(yesterday, null, "order_total",0,-1).toString()));
        // 新增用户数
        todayData.put("newUserTotalT",userService.getUserData(today, null, "user_total"));
        todayData.put("newUserTotalY",userService.getUserData(yesterday, null, "user_total"));
        //新供应商数
        todayData.put("newSupplierTotalT",supplierService.getSupplierTotalByDay(today));
        todayData.put("newSupplierTotalY",supplierService.getSupplierTotalByDay(yesterday));
        // 下单用户数
        todayData.put("orderUserTotalT",orderRankingService.getPayOrderUserTotal(today));
        todayData.put("orderUserTotalY",orderRankingService.getPayOrderUserTotal(yesterday));
        // 预计收入(元)
        result.put("todayData",todayData);

        //获取商品销售榜单
        JSONObject productData = new JSONObject();
        //销量排行榜
        productData.put("salesMoneyRank",productRankingService.getSaleMoneyRank(param));
        //销售额排行榜
        productData.put("salesNumRank",productRankingService.getSaleNumRank(param));
        result.put("productData",productData);

        return result;

    }
}
