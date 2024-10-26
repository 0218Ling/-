

package net.jjjshop.shop.param.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import net.jjjshop.framework.core.pagination.BasePageOrderParam;

import java.util.List;

/**
 * 部门 查询参数对象
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "OrderPageParam对象", description = "订单分页参数")
public class OrderPageParam extends BasePageOrderParam {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("搜索订单号")
    private String orderNo;

    @ApiModelProperty("查询条件：起始日期")
    private String startDate;

    @ApiModelProperty("查询条件：结束日期")
    private String endDate;

    @ApiModelProperty("查询条件：配送方式,10外卖配送,20外卖自提,30店内打包40店内堂食")
    private Integer styleId;

    @ApiModelProperty("查询条件：订单状态")
    private String dataType;

    @ApiModelProperty("用餐方式0外卖1店内")
    private Integer orderType;

    @ApiModelProperty("门店ID")
    private Integer shopSupplierId;

    @ApiModelProperty("0外卖1店内")
    private Integer productType;

    @ApiModelProperty("配送公司(10商家配送20达达30配送员)")
    private Integer deliversource;

    @ApiModelProperty("查询日期")
    private List<String> createTime;
}
