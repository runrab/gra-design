package org.jeecg.modules.runrab.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.runrab.entity.Message;
import org.jeecg.modules.runrab.service.IMessageService;
import java.util.Date;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

 /**
 * @Description: 消息
 * @Author runrab
 * @Date:   2022-03-28
 * @Version: V1.0
 */
@Slf4j
@Api(tags="消息")
@RestController
@RequestMapping("/runrab/message")
public class MessageController extends JeecgController<Message, IMessageService> {
	@Autowired
	private IMessageService messageService;
	
	/**
	 * 分页列表查询
	 *
	 * @param message
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@AutoLog(value = "消息-分页列表查询")
	@ApiOperation(value="消息-分页列表查询", notes="消息-分页列表查询")
	@GetMapping(value = "/list")
	public Result<?> queryPageList(Message message,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="50") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<Message> queryWrapper = QueryGenerator.initQueryWrapper(message, req.getParameterMap());
		Page<Message> page = new Page<Message>(pageNo, pageSize);
		IPage<Message> pageList = messageService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 * 添加
	 *
	 * @param message
	 * @return
	 */
	@AutoLog(value = "消息-添加")
	@ApiOperation(value="消息-添加", notes="消息-添加")
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody Message message) {
		messageService.save(message);
		return Result.OK("添加成功！");
	}
	
	/**
	 * 编辑
	 *
	 * @param message
	 * @return
	 */
	@AutoLog(value = "消息-编辑")
	@ApiOperation(value="消息-编辑", notes="消息-编辑")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<?> edit(@RequestBody Message message) {
		messageService.updateById(message);
		return Result.OK("编辑成功!");
	}
	
	/**
	 * 通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "消息-通过id删除")
	@ApiOperation(value="消息-通过id删除", notes="消息-通过id删除")
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		messageService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 * 批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "消息-批量删除")
	@ApiOperation(value="消息-批量删除", notes="消息-批量删除")
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.messageService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "消息-通过id查询")
	@ApiOperation(value="消息-通过id查询", notes="消息-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		Message message = messageService.getById(id);
		return Result.OK(message);
	}

  /**
   * 导出excel
   *
   * @param request
   * @param message
   */
  @RequestMapping(value = "/exportXls")
  public ModelAndView exportXls(HttpServletRequest request, Message message) {
      return super.exportXls(request, message, Message.class, "消息");
  }

  /**
   * 通过excel导入数据
   *
   * @param request
   * @param response
   * @return
   */
  @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
  public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response) {
      return super.importExcel(request, response, Message.class);
  }

}
