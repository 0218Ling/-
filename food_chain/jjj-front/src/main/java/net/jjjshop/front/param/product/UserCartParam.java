

package net.jjjshop.front.param.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 购物车参数对象
 */
@Data
@Accessors(chain = true)
@ApiModel(value = "CartParam", description = "购物车参数对象")
public class UserCartParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty("商品id")
    private Integer productId;

    @ApiModelProperty("商品数量")
    private Integer totalNum;

    @ApiModelProperty("店铺id")
    private Integer cartId;

    @ApiModelProperty("添加:up, 减少:down")
    private String type;

    @ApiModelProperty("购物车类型0外卖1店内")
    @NotNull(message = "购物车类型不能为空")
    private Integer cartType;

    @ApiModelProperty("dinnerType")
    private Integer dinnerType;

    @ApiModelProperty("店铺id")
    @NotNull(message = "店铺id不能为空")
    private Integer shopSupplierId;

    @ApiModelProperty("就餐类型,10外卖配送,20外卖自提,30店内打包40店内堂食")
    private Integer delivery;

    @ApiModelProperty("用户ID")
    private Integer userId;


}
