package org.jeecg.modules.runrab.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.runrab.entity.Zhaopin;
import org.jeecg.modules.runrab.service.IZhaopinService;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;

import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.jeecg.common.system.base.controller.JeecgController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;
import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.common.aspect.annotation.AutoLog;

 /**
 * @Description: 招聘信息
 * @Author runrab
 * @Date:   2022-04-02
 * @Version: V1.0
 */
@Api(tags="招聘信息")
@RestController
@RequestMapping("/runrab/zhaopin")
@Slf4j
public class ZhaopinController extends JeecgController<Zhaopin, IZhaopinService> {
	@Autowired
	private IZhaopinService zhaopinService;
	
	/**
	 * 分页列表查询
	 *
	 * @param zhaopin
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	//@AutoLog(value = "招聘信息-分页列表查询")
	@ApiOperation(value="招聘信息-分页列表查询", notes="招聘信息-分页列表查询")
	@GetMapping(value = "/list")
	public Result<IPage<Zhaopin>> queryPageList(Zhaopin zhaopin,
								   @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
								   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
								   HttpServletRequest req) {
		QueryWrapper<Zhaopin> queryWrapper = QueryGenerator.initQueryWrapper(zhaopin, req.getParameterMap());
		Page<Zhaopin> page = new Page<Zhaopin>(pageNo, pageSize);
		IPage<Zhaopin> pageList = zhaopinService.page(page, queryWrapper);
		return Result.OK(pageList);
	}
	
	/**
	 *   添加
	 *
	 * @param zhaopin
	 * @return
	 */
	@AutoLog(value = "招聘信息-添加")
	@ApiOperation(value="招聘信息-添加", notes="招聘信息-添加")
	@PostMapping(value = "/add")
	public Result<String> add(@RequestBody Zhaopin zhaopin) {
		System.out.println(122222);
		System.out.println(zhaopin.toString());
		zhaopinService.save(zhaopin);
		return Result.OK("添加成功！");
	}
	
	/**
	 *  编辑
	 *
	 * @param zhaopin
	 * @return
	 */
	@AutoLog(value = "招聘信息-编辑")
	@ApiOperation(value="招聘信息-编辑", notes="招聘信息-编辑")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<String> edit(@RequestBody Zhaopin zhaopin) {
		zhaopinService.updateById(zhaopin);
		return Result.OK("编辑成功!");
	}
	
	/**
	 *   通过id删除
	 *
	 * @param id
	 * @return
	 */
	@AutoLog(value = "招聘信息-通过id删除")
	@ApiOperation(value="招聘信息-通过id删除", notes="招聘信息-通过id删除")
	@DeleteMapping(value = "/delete")
	public Result<String> delete(@RequestParam(name="id",required=true) String id) {
		zhaopinService.removeById(id);
		return Result.OK("删除成功!");
	}
	
	/**
	 *  批量删除
	 *
	 * @param ids
	 * @return
	 */
	@AutoLog(value = "招聘信息-批量删除")
	@ApiOperation(value="招聘信息-批量删除", notes="招聘信息-批量删除")
	@DeleteMapping(value = "/deleteBatch")
	public Result<String> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		this.zhaopinService.removeByIds(Arrays.asList(ids.split(",")));
		return Result.OK("批量删除成功!");
	}
	
	/**
	 * 通过id查询
	 *
	 * @param id
	 * @return
	 */
	//@AutoLog(value = "招聘信息-通过id查询")
	@ApiOperation(value="招聘信息-通过id查询", notes="招聘信息-通过id查询")
	@GetMapping(value = "/queryById")
	public Result<Zhaopin> queryById(@RequestParam(name="id",required=true) String id) {
		Zhaopin zhaopin = zhaopinService.getById(id);
		if(zhaopin==null) {
			return Result.error("未找到对应数据",null);
		}
		return Result.OK(zhaopin);
	}

    /**
    * 导出excel
    *
    * @param request
    * @param zhaopin
    */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(HttpServletRequest request, Zhaopin zhaopin) {
        return super.exportXls(request, zhaopin, Zhaopin.class, "招聘信息");
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
        return super.importExcel(request, response, Zhaopin.class);
    }

}
