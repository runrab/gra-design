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
import org.jeecg.modules.runrab.entity.ZhaoPin;
import org.jeecg.modules.runrab.service.IZhaoPinService;
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
 * @Description: 招聘
 * @Author: jeecg-boot
 * @Date:   2022-03-28
 * @Version: V1.0
 */
@Slf4j
@Api(tags="招聘")
@RestController
@RequestMapping("/runrab/zhaoPin")
public class ZhaoPinController extends JeecgController<ZhaoPin, IZhaoPinService> {
	@Autowired
	private IZhaoPinService zhaoPinService;
	
	/**
	 * 分页列表查询
	 *
	 * @param zhaoPin
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@AutoLog(value = "招聘-分页列表查询")
	@ApiOperation(value="招聘-分页列表查询", notes="招聘-分页列表查询")
	@GetMapping(value = "/list")
	public Result<?> queryPageList(ZhaoPin zhaoPin,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<ZhaoPin> queryWrapper = QueryGenerator.initQueryWrapper(zhaoPin, req.getParameterMap());
		Page<ZhaoPin> page = new Page<ZhaoPin>(pageNo, pageSize);
		IPage<ZhaoPin> pageList = zhaoPinService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 * 添加
	 *
	 * @param zhaoPin
	 * @return
	 */
	@AutoLog(value = "招聘-添加")
	@ApiOperation(value="招聘-添加", notes="招聘-添加")
	@PostMapping(value = "/add")
	public Result<?> add(@RequestBody ZhaoPin zhaoPin) {
		zhaoPinService.save(zhaoPin);
		return Result.OK("添加成功！");
	}
	
	/**
	 * 编辑
	 *
	 * @param zhaoPin
	 * @return
	 */
	@AutoLog(value = "招聘-编辑")
	@ApiOperation(value="招聘-编辑", notes="招聘-编辑")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<?> edit(@RequestBody ZhaoPin zhaoPin) {
		zhaoPinService.updateById(zhaoPin);
		return Result.OK("编辑成功!");
	}
	
	/**
	 * 通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "招聘-通过id删除")
	@ApiOperation(value="招聘-通过id删除", notes="招聘-通过id删除")
	@DeleteMapping(value = "/delete")
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		zhaoPinService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 * 批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "招聘-批量删除")
	@ApiOperation(value="招聘-批量删除", notes="招聘-批量删除")
	@DeleteMapping(value = "/deleteBatch")
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.zhaoPinService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功！");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "招聘-通过id查询")
	@ApiOperation(value="招聘-通过id查询", notes="招聘-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<?> queryById(@RequestParam(name="id",required=true) String id) {
		ZhaoPin zhaoPin = zhaoPinService.getById(id);
		return Result.OK(zhaoPin);
	}

  /**
   * 导出excel
   *
   * @param request
   * @param zhaoPin
   */
  @RequestMapping(value = "/exportXls")
  public ModelAndView exportXls(HttpServletRequest request, ZhaoPin zhaoPin) {
      return super.exportXls(request, zhaoPin, ZhaoPin.class, "招聘");
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
      return super.importExcel(request, response, ZhaoPin.class);
  }

}
