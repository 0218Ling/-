package net.jjjshop.common.settings.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.Accessors;
import net.jjjshop.common.enums.DeliveryTypeEnum;
import net.jjjshop.common.enums.OperateTypeEnum;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
@ApiModel("系统设置VO")
public class StoreVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Boolean isGetLog;

    private List<Integer> deliveryType;

    public StoreVo(){
        this.isGetLog = false;
        this.deliveryType = new ArrayList<>();
    }
}
