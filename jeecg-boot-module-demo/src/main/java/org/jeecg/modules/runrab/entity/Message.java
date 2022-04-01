package org.jeecg.modules.runrab.entity;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * @Description: 消息
 * @Author runrab
 * @Date:   2022-03-28
 * @Version: V1.0
 */
@Data
@TableName("message")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value="message对象", description="消息")
public class Message {
    
	/**主键id*/
	@TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键id")
	private java.lang.String id;
	/**消息内容*/
	@Excel(name = "消息内容", width = 15)
    @ApiModelProperty(value = "消息内容")
	private java.lang.String context;
	/**发留言用户id可判断是否为本人*/
	@Excel(name = "发留言用户id可判断是否为本人", width = 15)
    @ApiModelProperty(value = "发留言用户id可判断是否为本人")
	private java.lang.String userid;
	/**区分身份信息*/
	@Excel(name = "区分身份信息", width = 15)
    @ApiModelProperty(value = "区分身份信息")
	private java.lang.Integer identity;
	/**可见性0本人可见 1全体 -1删除 */
	@Excel(name = "可见性0本人可见 1全体 -1删除 ", width = 15)
    @ApiModelProperty(value = "可见性0本人可见 1全体 -1删除 ")
	private java.lang.Integer visible;
	/**创建人*/
	@Excel(name = "创建人", width = 15)
    @ApiModelProperty(value = "创建人")
	private java.lang.String createBy;
	/**创建时间*/
	@Excel(name = "创建时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "创建时间")
	private java.util.Date createTime;
	/**更新人*/
	@Excel(name = "更新人", width = 15)
    @ApiModelProperty(value = "更新人")
	private java.lang.String updateBy;
	/**更新时间*/
	@Excel(name = "更新时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(timezone = "GMT+8",pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "更新时间")
	private java.util.Date updateTime;
	/**头像*/
	@Excel(name = "头像", width = 15)
    @ApiModelProperty(value = "头像")
	private java.lang.String avatar;
}
