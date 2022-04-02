package org.jeecg.modules.runrab.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.jeecg.common.aspect.annotation.Dict;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @Description: 留言信息
 * @Author runrab
 * @Date:   2022-04-02
 * @Version: V1.0
 */
@Data
@TableName("message")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@ApiModel(value="message对象", description="留言信息")
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

	/**主键*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;
	/**创建人*/
    @ApiModelProperty(value = "创建人")
    private java.lang.String createBy;
	/**创建日期*/
    @ApiModelProperty(value = "创建日期")
    private java.util.Date createTime;
	/**更新人*/
    @ApiModelProperty(value = "更新人")
    private java.lang.String updateBy;
	/**更新日期*/
    @ApiModelProperty(value = "更新日期")
    private java.util.Date updateTime;
	/**所属部门*/
    @ApiModelProperty(value = "所属部门")
    private java.lang.String sysOrgCode;
	/**留言用户*/
	@Excel(name = "留言用户", width = 15)
    @ApiModelProperty(value = "留言用户")
    private java.lang.String userid;
	/**用户头像*/
	@Excel(name = "用户头像", width = 15)
    @ApiModelProperty(value = "用户头像")
    private java.lang.String avatar;
	/**身份信息*/
	@Excel(name = "身份信息", width = 15)
    @ApiModelProperty(value = "身份信息")
    private java.lang.Integer identity;
	/**可见性*/
	@Excel(name = "可见性", width = 15)
    @ApiModelProperty(value = "可见性")
    private java.lang.Integer visible;
	/**留言内容*/
	@Excel(name = "留言内容", width = 15)
    @ApiModelProperty(value = "留言内容")
    private java.lang.String context;
}
