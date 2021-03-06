package org.jeecg.modules.system.controller;


import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.PermissionData;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.modules.base.service.BaseCommonService;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.*;
import org.jeecg.modules.system.entity.*;
import org.jeecg.modules.system.model.DepartIdModel;
import org.jeecg.modules.system.model.SysUserSysDepartModel;
import org.jeecg.modules.system.service.*;
import org.jeecg.modules.system.vo.SysDepartUsersVO;
import org.jeecg.modules.system.vo.SysUserRoleVO;
import org.jeecgframework.poi.excel.ExcelImportUtil;
import org.jeecgframework.poi.excel.def.NormalExcelConstants;
import org.jeecgframework.poi.excel.entity.ExportParams;
import org.jeecgframework.poi.excel.entity.ImportParams;
import org.jeecgframework.poi.excel.view.JeecgEntityExcelView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * ????????? ???????????????
 * </p>
 *
 * @Author runrab
 * @since 2018-12-20
 */
@Slf4j
@RestController
@RequestMapping("/sys/user")
public class SysUserController {
	@Autowired
	private ISysBaseAPI sysBaseAPI;
	
	@Autowired
	private ISysUserService sysUserService;

    @Autowired
    private ISysDepartService sysDepartService;

	@Autowired
	private ISysUserRoleService sysUserRoleService;

	@Autowired
	private ISysUserDepartService sysUserDepartService;

	@Autowired
	private ISysUserRoleService userRoleService;

    @Autowired
    private ISysDepartRoleUserService departRoleUserService;

    @Autowired
    private ISysDepartRoleService departRoleService;

	@Autowired
	private RedisUtil redisUtil;

    @Value("${jeecg.path.upload}")
    private String upLoadPath;

    @Resource
    private BaseCommonService baseCommonService;

    /**
     * ????????????????????????
     * @param user
     * @param pageNo
     * @param pageSize
     * @param req
     * @return
     */
    @PermissionData(pageComponent = "system/UserList")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public Result<IPage<SysUser>> queryPageList(SysUser user,@RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
									  @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
                                      @RequestParam(name="identity", required = false) Integer identity,
                                                HttpServletRequest req) {
		Result<IPage<SysUser>> result = new Result<IPage<SysUser>>();
		QueryWrapper<SysUser> queryWrapper = QueryGenerator.initQueryWrapper(user, req.getParameterMap());
        
        //??????ID
        String departId = req.getParameter("departId");

//        System.out.println(departId);

        if(oConvertUtils.isNotEmpty(departId)){
            LambdaQueryWrapper<SysUserDepart> query = new LambdaQueryWrapper<>();
            query.eq(SysUserDepart::getDepId,departId);
            List<SysUserDepart> list = sysUserDepartService.list(query);
            List<String> userIds = list.stream().map(SysUserDepart::getUserId).collect(Collectors.toList());
            queryWrapper.in("id",userIds);
        }
        //??????ID
        String code = req.getParameter("code");
        if(oConvertUtils.isNotEmpty(code)){
            queryWrapper.in("id",Arrays.asList(code.split(",")));
            pageSize = code.split(",").length;
        }

        String status = req.getParameter("status");
        if(oConvertUtils.isNotEmpty(status)){
            queryWrapper.eq("status", Integer.parseInt(status));
        }
        //???????????????????????? online??????????????????????????????????????????????????????????????????

        //TODO ????????????????????????????????????????????????
        queryWrapper.ne("username","_reserve_user_external");

        // ?????? ???????????? ?????? ?????????
        if(identity!=null&&(identity==0||identity==1)){
            queryWrapper.eq("identity",Integer.valueOf(identity));
        }
        
		Page<SysUser> page = new Page<SysUser>(pageNo, pageSize);
		IPage<SysUser> pageList = sysUserService.page(page, queryWrapper);

        //?????????????????????????????????
        //step.1 ?????????????????? useids
        //step.2 ?????? useids?????????????????????????????????????????????
        List<String> userIds = pageList.getRecords().stream().map(SysUser::getId).collect(Collectors.toList());
        if(userIds!=null && userIds.size()>0){
            Map<String,String>  useDepNames = sysUserService.getDepNamesByUserIds(userIds);
            pageList.getRecords().forEach(item->{
                item.setOrgCodeTxt(useDepNames.get(item.getId()));
            });
        }
		result.setSuccess(true);
		result.setResult(pageList);
		log.info(pageList.toString());
		return result;
	}

    //?????? ??????app?????????????????????
    //@RequiresRoles({"admin"})
    //@RequiresPermissions("user:add")
	@RequestMapping(value = "/add", method = RequestMethod.POST)
	public Result<SysUser> add(@RequestBody JSONObject jsonObject) {
		Result<SysUser> result = new Result<SysUser>();
		String selectedRoles = jsonObject.getString("selectedroles");
		String selectedDeparts = jsonObject.getString("selecteddeparts");

//        System.out.println(jsonObject.toJSONString());
//        System.out.println(selectedRoles);
//        System.out.println(selectedDeparts);
		try {
			SysUser user = JSON.parseObject(jsonObject.toJSONString(), SysUser.class);
			user.setCreateTime(new Date());//??????????????????
			String salt = oConvertUtils.randomGen(8);
			user.setSalt(salt);
			String passwordEncode = PasswordUtil.encrypt(user.getUsername(), user.getPassword(), salt);
			user.setPassword(passwordEncode);
			user.setStatus(1);
			user.setDelFlag(CommonConstant.DEL_FLAG_0);
			// ?????????????????????service ????????????

//            System.out.println(jsonObject.toJSONString());
//            System.out.println(user.toString());

			sysUserService.saveUser(user, selectedRoles, selectedDeparts);
			result.success("???????????????");
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			result.error500("????????????");
		}
		return result;
	}

    //@RequiresRoles({"admin"})
    //@RequiresPermissions("user:edit")
	@RequestMapping(value = "/edit", method = {RequestMethod.PUT,RequestMethod.POST})
	public Result<SysUser> edit(@RequestBody JSONObject jsonObject) {
		Result<SysUser> result = new Result<SysUser>();
		try {
			SysUser sysUser = sysUserService.getById(jsonObject.getString("id"));
			baseCommonService.addLog("???????????????id??? " +jsonObject.getString("id") ,CommonConstant.LOG_TYPE_2, 2);
			if(sysUser==null) {
				result.error500("?????????????????????");
			}else {
				SysUser user = JSON.parseObject(jsonObject.toJSONString(), SysUser.class);
				user.setUpdateTime(new Date());
				//String passwordEncode = PasswordUtil.encrypt(user.getUsername(), user.getPassword(), sysUser.getSalt());
				user.setPassword(sysUser.getPassword());
				String roles = jsonObject.getString("selectedroles");
                String departs = jsonObject.getString("selecteddeparts");
                if(oConvertUtils.isEmpty(departs)){
                    //vue3.0??????????????????departIds
                    departs=user.getDepartIds();
                }
                // ?????????????????????service ????????????
				sysUserService.editUser(user, roles, departs);
				result.success("????????????!");
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			result.error500("????????????");
		}
		return result;
	}

	/**
	 * ????????????
	 */
	//@RequiresRoles({"admin"})
	@RequestMapping(value = "/delete", method = RequestMethod.DELETE)
	public Result<?> delete(@RequestParam(name="id",required=true) String id) {
		baseCommonService.addLog("???????????????id??? " +id ,CommonConstant.LOG_TYPE_2, 3);
		this.sysUserService.deleteUser(id);
		return Result.ok("??????????????????");
	}

	/**
	 * ??????????????????
	 */
	//@RequiresRoles({"admin"})
	@RequestMapping(value = "/deleteBatch", method = RequestMethod.DELETE)
	public Result<?> deleteBatch(@RequestParam(name="ids",required=true) String ids) {
		baseCommonService.addLog("????????????????????? ids??? " +ids ,CommonConstant.LOG_TYPE_2, 3);
		this.sysUserService.deleteBatchUsers(ids);
		return Result.ok("????????????????????????");
	}

	/**
	  * ??????&????????????
	 * @param jsonObject
	 * @return
	 */
	//@RequiresRoles({"admin"})
	@RequestMapping(value = "/frozenBatch", method = RequestMethod.PUT)
	public Result<SysUser> frozenBatch(@RequestBody JSONObject jsonObject) {
		Result<SysUser> result = new Result<SysUser>();
		try {
			String ids = jsonObject.getString("ids");
			String status = jsonObject.getString("status");
			String[] arr = ids.split(",");
			for (String id : arr) {
				if(oConvertUtils.isNotEmpty(id)) {
					this.sysUserService.update(new SysUser().setStatus(Integer.parseInt(status)),
							new UpdateWrapper<SysUser>().lambda().eq(SysUser::getId,id));
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			result.error500("????????????"+e.getMessage());
		}
		result.success("????????????!");
		return result;

    }

    @RequestMapping(value = "/queryById", method = RequestMethod.GET)
    public Result<SysUser> queryById(@RequestParam(name = "id", required = true) String id) {
        Result<SysUser> result = new Result<SysUser>();
        SysUser sysUser = sysUserService.getById(id);
        if (sysUser == null) {
            result.error500("?????????????????????");
        } else {
            result.setResult(sysUser);
            result.setSuccess(true);
        }
        return result;
    }

    @RequestMapping(value = "/queryUserRole", method = RequestMethod.GET)
    public Result<List<String>> queryUserRole(@RequestParam(name = "userid", required = true) String userid) {
        Result<List<String>> result = new Result<>();
        List<String> list = new ArrayList<String>();
        List<SysUserRole> userRole = sysUserRoleService.list(new QueryWrapper<SysUserRole>().lambda().eq(SysUserRole::getUserId, userid));
        if (userRole == null || userRole.size() <= 0) {
            result.error500("?????????????????????????????????");
        } else {
            for (SysUserRole sysUserRole : userRole) {
                list.add(sysUserRole.getRoleId());
            }
            result.setSuccess(true);
            result.setResult(list);
        }
        return result;
    }


    /**
	  *  ??????????????????????????????<br>
	  *  ?????????????????? ???????????????????????????????????????
     *
     * @param sysUser
     * @return
     */
    @RequestMapping(value = "/checkOnlyUser", method = RequestMethod.GET)
    public Result<Boolean> checkOnlyUser(SysUser sysUser) {
        Result<Boolean> result = new Result<>();
        //??????????????????false?????????????????????
        result.setResult(true);
        try {
            //??????????????????????????????????????????
            sysUser.setPassword(null);
            SysUser user = sysUserService.getOne(new QueryWrapper<SysUser>(sysUser));
            if (user != null) {
                result.setSuccess(false);
                result.setMessage("?????????????????????");
                return result;
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage(e.getMessage());
            return result;
        }
        result.setSuccess(true);
        return result;
    }

    /**
     * ????????????
     */
    //@RequiresRoles({"admin"})
    @RequestMapping(value = "/changePassword", method = RequestMethod.PUT)
    public Result<?> changePassword(@RequestBody SysUser sysUser) {
        SysUser u = this.sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, sysUser.getUsername()));
        if (u == null) {
            return Result.error("??????????????????");
        }
        sysUser.setId(u.getId());
        return sysUserService.changePassword(sysUser);
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/userDepartList", method = RequestMethod.GET)
    public Result<List<DepartIdModel>> getUserDepartsList(@RequestParam(name = "userId", required = true) String userId) {
        Result<List<DepartIdModel>> result = new Result<>();
        try {
            List<DepartIdModel> depIdModelList = this.sysUserDepartService.queryDepartIdsOfUser(userId);
            if (depIdModelList != null && depIdModelList.size() > 0) {
                result.setSuccess(true);
                result.setMessage("????????????");
                result.setResult(depIdModelList);
            } else {
                result.setSuccess(false);
                result.setMessage("????????????");
            }
            return result;
        } catch (Exception e) {
        	log.error(e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("??????????????????????????????: " + e.getMessage());
            return result;
        }

    }

    /**
     * ???????????????????????????????????????????????????,???????????????,?????????id??????????????????
     *
     * @return
     */
    @RequestMapping(value = "/generateUserId", method = RequestMethod.GET)
    public Result<String> generateUserId() {
        Result<String> result = new Result<>();
        System.out.println("????????????,????????????ID==============================");
        String userId = UUID.randomUUID().toString().replace("-", "");
        result.setSuccess(true);
        result.setResult(userId);
        return result;
    }

    /**
     * ????????????id??????????????????
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/queryUserByDepId", method = RequestMethod.GET)
    public Result<List<SysUser>> queryUserByDepId(@RequestParam(name = "id", required = true) String id,@RequestParam(name="realname",required=false) String realname) {
        Result<List<SysUser>> result = new Result<>();
        //List<SysUser> userList = sysUserDepartService.queryUserByDepId(id);
        SysDepart sysDepart = sysDepartService.getById(id);
        List<SysUser> userList = sysUserDepartService.queryUserByDepCode(sysDepart.getOrgCode(),realname);

        //?????????????????????????????????
        //step.1 ?????????????????? useids
        //step.2 ?????? useids?????????????????????????????????????????????
        List<String> userIds = userList.stream().map(SysUser::getId).collect(Collectors.toList());
        if(userIds!=null && userIds.size()>0){
            Map<String,String>  useDepNames = sysUserService.getDepNamesByUserIds(userIds);
            userList.forEach(item->{
                //TODO ??????????????????????????????????????????
                item.setOrgCodeTxt(useDepNames.get(item.getId()));
            });
        }

        try {
            result.setSuccess(true);
            result.setResult(userList);
            return result;
        } catch (Exception e) {
        	log.error(e.getMessage(), e);
            result.setSuccess(false);
            return result;
        }
    }

    /**
     * ?????????????????? ??????  ???????????????????????????????????????
     * @param departId
     * @param username
     * @return
     */
    @RequestMapping(value = "/queryUserComponentData", method = RequestMethod.GET)
    public Result<IPage<SysUser>> queryUserComponentData(
            @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
            @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
            @RequestParam(name = "departId", required = false) String departId,
            @RequestParam(name="realname",required=false) String realname,
            @RequestParam(name="username",required=false) String username) {
        IPage<SysUser> pageList = sysUserDepartService.queryDepartUserPageList(departId, username, realname, pageSize, pageNo);
        return Result.OK(pageList);
    }

    /**
     * ??????excel
     *
     * @param request
     * @param sysUser
     */
    @RequestMapping(value = "/exportXls")
    public ModelAndView exportXls(SysUser sysUser,HttpServletRequest request) {
        // Step.1 ??????????????????
        QueryWrapper<SysUser> queryWrapper = QueryGenerator.initQueryWrapper(sysUser, request.getParameterMap());
        //Step.2 AutoPoi ??????Excel
        ModelAndView mv = new ModelAndView(new JeecgEntityExcelView());
        //update-begin--Author:kangxiaolin  Date:20180825 for???[03]?????????????????????????????????????????????????????????--------------------
        String selections = request.getParameter("selections");
       if(!oConvertUtils.isEmpty(selections)){
           queryWrapper.in("id",selections.split(","));
       }
        //update-end--Author:kangxiaolin  Date:20180825 for???[03]?????????????????????????????????????????????????????????----------------------
        List<SysUser> pageList = sysUserService.list(queryWrapper);

        //??????????????????
        mv.addObject(NormalExcelConstants.FILE_NAME, "????????????");
        mv.addObject(NormalExcelConstants.CLASS, SysUser.class);
		LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        ExportParams exportParams = new ExportParams("??????????????????", "?????????:"+user.getRealname(), "????????????");
        exportParams.setImageBasePath(upLoadPath);
        mv.addObject(NormalExcelConstants.PARAMS, exportParams);
        mv.addObject(NormalExcelConstants.DATA_LIST, pageList);
        return mv;
    }

    /**
     * ??????excel????????????
     *
     * @param request
     * @param response
     * @return
     */
    //@RequiresRoles({"admin"})
    //@RequiresPermissions("user:import")
    @RequestMapping(value = "/importExcel", method = RequestMethod.POST)
    public Result<?> importExcel(HttpServletRequest request, HttpServletResponse response)throws IOException {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
        // ????????????
        List<String> errorMessage = new ArrayList<>();
        int successLines = 0, errorLines = 0;
        for (Map.Entry<String, MultipartFile> entity : fileMap.entrySet()) {
            MultipartFile file = entity.getValue();// ????????????????????????
            ImportParams params = new ImportParams();
            params.setTitleRows(2);
            params.setHeadRows(1);
            params.setNeedSave(true);
            try {
                List<SysUser> listSysUsers = ExcelImportUtil.importExcel(file.getInputStream(), SysUser.class, params);
                for (int i = 0; i < listSysUsers.size(); i++) {
                    SysUser sysUserExcel = listSysUsers.get(i);
                    if (StringUtils.isBlank(sysUserExcel.getPassword())) {
                        // ??????????????? ???123456???
                        sysUserExcel.setPassword("123456");
                    }
                    // ??????????????????
                    String salt = oConvertUtils.randomGen(8);
                    sysUserExcel.setSalt(salt);
                    String passwordEncode = PasswordUtil.encrypt(sysUserExcel.getUsername(), sysUserExcel.getPassword(), salt);
                    sysUserExcel.setPassword(passwordEncode);
                    try {
                        sysUserService.save(sysUserExcel);
                        successLines++;
                    } catch (Exception e) {
                        errorLines++;
                        String message = e.getMessage().toLowerCase();
                        int lineNumber = i + 1;
                        // ?????????????????????????????????
                        if (message.contains(CommonConstant.SQL_INDEX_UNIQ_SYS_USER_USERNAME)) {
                            errorMessage.add("??? " + lineNumber + " ?????????????????????????????????????????????");
                        } else if (message.contains(CommonConstant.SQL_INDEX_UNIQ_SYS_USER_WORK_NO)) {
                            errorMessage.add("??? " + lineNumber + " ??????????????????????????????????????????");
                        } else if (message.contains(CommonConstant.SQL_INDEX_UNIQ_SYS_USER_PHONE)) {
                            errorMessage.add("??? " + lineNumber + " ?????????????????????????????????????????????");
                        } else if (message.contains(CommonConstant.SQL_INDEX_UNIQ_SYS_USER_EMAIL)) {
                            errorMessage.add("??? " + lineNumber + " ????????????????????????????????????????????????");
                        }  else if (message.contains(CommonConstant.SQL_INDEX_UNIQ_SYS_USER)) {
                            errorMessage.add("??? " + lineNumber + " ?????????????????????????????????");
                        } else {
                            errorMessage.add("??? " + lineNumber + " ?????????????????????????????????");
                            log.error(e.getMessage(), e);
                        }
                    }
                    // ????????????????????????????????????????????????
                    String departIds = sysUserExcel.getDepartIds();
                    if (StringUtils.isNotBlank(departIds)) {
                        String userId = sysUserExcel.getId();
                        String[] departIdArray = departIds.split(",");
                        List<SysUserDepart> userDepartList = new ArrayList<>(departIdArray.length);
                        for (String departId : departIdArray) {
                            userDepartList.add(new SysUserDepart(userId, departId));
                        }
                        sysUserDepartService.saveBatch(userDepartList);
                    }

                }
            } catch (Exception e) {
                errorMessage.add("???????????????" + e.getMessage());
                log.error(e.getMessage(), e);
            } finally {
                try {
                    file.getInputStream().close();
                } catch (IOException e) {
                	log.error(e.getMessage(), e);
                }
            }
        }
        return ImportExcelUtil.imporReturnRes(errorLines,successLines,errorMessage);
    }

    /**
	 * @???????????????id ????????????
	 * @param userIds
	 * @return
	 */
	@RequestMapping(value = "/queryByIds", method = RequestMethod.GET)
	public Result<Collection<SysUser>> queryByIds(@RequestParam String userIds) {
		Result<Collection<SysUser>> result = new Result<>();
		String[] userId = userIds.split(",");
		Collection<String> idList = Arrays.asList(userId);
		Collection<SysUser> userRole = sysUserService.listByIds(idList);
		result.setSuccess(true);
		result.setResult(userRole);
		return result;
	}

	/**
	 * ????????????????????????
	 */
    //@RequiresRoles({"admin"})
    @RequestMapping(value = "/updatePassword", method = RequestMethod.PUT)
	public Result<?> updatePassword(@RequestBody JSONObject json) {
		String username = json.getString("username");
		String oldpassword = json.getString("oldpassword");
		String password = json.getString("password");
		String confirmpassword = json.getString("confirmpassword");
        LoginUser sysUser = (LoginUser)SecurityUtils.getSubject().getPrincipal();
        if(!sysUser.getUsername().equals(username)){
            return Result.error("?????????????????????????????????");
        }
		SysUser user = this.sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
		if(user==null) {
			return Result.error("??????????????????");
		}
		return sysUserService.resetPassword(username,oldpassword,password,confirmpassword);
	}

    @RequestMapping(value = "/userRoleList", method = RequestMethod.GET)
    public Result<IPage<SysUser>> userRoleList(@RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                               @RequestParam(name="pageSize", defaultValue="10") Integer pageSize, HttpServletRequest req) {
        Result<IPage<SysUser>> result = new Result<IPage<SysUser>>();
        Page<SysUser> page = new Page<SysUser>(pageNo, pageSize);
        String roleId = req.getParameter("roleId");
        String username = req.getParameter("username");
        IPage<SysUser> pageList = sysUserService.getUserByRoleId(page,roleId,username);
        result.setSuccess(true);
        result.setResult(pageList);
        return result;
    }

    /**
     * ???????????????????????????
     *
     * @param
     * @return
     */
    //@RequiresRoles({"admin"})
    @RequestMapping(value = "/addSysUserRole", method = RequestMethod.POST)
    public Result<String> addSysUserRole(@RequestBody SysUserRoleVO sysUserRoleVO) {
        Result<String> result = new Result<String>();
        try {
            String sysRoleId = sysUserRoleVO.getRoleId();
            for(String sysUserId:sysUserRoleVO.getUserIdList()) {
                SysUserRole sysUserRole = new SysUserRole(sysUserId,sysRoleId);
                QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<SysUserRole>();
                queryWrapper.eq("role_id", sysRoleId).eq("user_id",sysUserId);
                SysUserRole one = sysUserRoleService.getOne(queryWrapper);
                if(one==null){
                    sysUserRoleService.save(sysUserRole);
                }

            }
            result.setMessage("????????????!");
            result.setSuccess(true);
            return result;
        }catch(Exception e) {
            log.error(e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("?????????: " + e.getMessage());
            return result;
        }
    }
    /**
     *   ?????????????????????????????????
     * @param
     * @return
     */
    //@RequiresRoles({"admin"})
    @RequestMapping(value = "/deleteUserRole", method = RequestMethod.DELETE)
    public Result<SysUserRole> deleteUserRole(@RequestParam(name="roleId") String roleId,
                                                    @RequestParam(name="userId",required=true) String userId
    ) {
        Result<SysUserRole> result = new Result<SysUserRole>();
        try {
            QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<SysUserRole>();
            queryWrapper.eq("role_id", roleId).eq("user_id",userId);
            sysUserRoleService.remove(queryWrapper);
            result.success("????????????!");
        }catch(Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param
     * @return
     */
    //@RequiresRoles({"admin"})
    @RequestMapping(value = "/deleteUserRoleBatch", method = RequestMethod.DELETE)
    public Result<SysUserRole> deleteUserRoleBatch(
            @RequestParam(name="roleId") String roleId,
            @RequestParam(name="userIds",required=true) String userIds) {
        Result<SysUserRole> result = new Result<SysUserRole>();
        try {
            QueryWrapper<SysUserRole> queryWrapper = new QueryWrapper<SysUserRole>();
            queryWrapper.eq("role_id", roleId).in("user_id",Arrays.asList(userIds.split(",")));
            sysUserRoleService.remove(queryWrapper);
            result.success("????????????!");
        }catch(Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }

    /**
     * ??????????????????
     */
    @RequestMapping(value = "/departUserList", method = RequestMethod.GET)
    public Result<IPage<SysUser>> departUserList(@RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                                 @RequestParam(name="pageSize", defaultValue="10") Integer pageSize, HttpServletRequest req) {
        Result<IPage<SysUser>> result = new Result<IPage<SysUser>>();
        Page<SysUser> page = new Page<SysUser>(pageNo, pageSize);
        String depId = req.getParameter("depId");
        String username = req.getParameter("username");
        //????????????ID??????,??????????????????????????????IDS
        List<String> subDepids = new ArrayList<>();
        //??????id?????????????????????????????????????????????
        if(oConvertUtils.isEmpty(depId)){
            LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            int userIdentity = user.getUserIdentity() != null?user.getUserIdentity():CommonConstant.USER_IDENTITY_1;
            if(oConvertUtils.isNotEmpty(userIdentity) && userIdentity == CommonConstant.USER_IDENTITY_2 ){
                subDepids = sysDepartService.getMySubDepIdsByDepId(user.getDepartIds());
            }
        }else{
            subDepids = sysDepartService.getSubDepIdsByDepId(depId);
        }
        if(subDepids != null && subDepids.size()>0){
            IPage<SysUser> pageList = sysUserService.getUserByDepIds(page,subDepids,username);
            //?????????????????????????????????
            //step.1 ?????????????????? useids
            //step.2 ?????? useids?????????????????????????????????????????????
            List<String> userIds = pageList.getRecords().stream().map(SysUser::getId).collect(Collectors.toList());
            if(userIds!=null && userIds.size()>0){
                Map<String, String> useDepNames = sysUserService.getDepNamesByUserIds(userIds);
                pageList.getRecords().forEach(item -> {
                    //?????????????????????????????????
                    item.setOrgCode(useDepNames.get(item.getId()));
                });
            }
            result.setSuccess(true);
            result.setResult(pageList);
        }else{
            result.setSuccess(true);
            result.setResult(null);
        }
        return result;
    }


    /**
     * ?????? orgCode ??????????????????????????????????????????
     * ?????????????????????????????????????????????????????????????????????????????????????????????
     */
    @GetMapping("/queryByOrgCode")
    public Result<?> queryByDepartId(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "orgCode") String orgCode,
            SysUser userParams
    ) {
        IPage<SysUserSysDepartModel> pageList = sysUserService.queryUserByOrgCode(orgCode, userParams, new Page(pageNo, pageSize));
        return Result.ok(pageList);
    }

    /**
     * ?????? orgCode ??????????????????????????????????????????
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    @GetMapping("/queryByOrgCodeForAddressList")
    public Result<?> queryByOrgCodeForAddressList(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "orgCode",required = false) String orgCode,
            SysUser userParams
    ) {
        IPage page = new Page(pageNo, pageSize);
        IPage<SysUserSysDepartModel> pageList = sysUserService.queryUserByOrgCode(orgCode, userParams, page);
        List<SysUserSysDepartModel> list = pageList.getRecords();

        // ???????????????????????? user, key = userId
        Map<String, JSONObject> hasUser = new HashMap<>(list.size());

        JSONArray resultJson = new JSONArray(list.size());

        for (SysUserSysDepartModel item : list) {
            String userId = item.getId();
            // userId
            JSONObject getModel = hasUser.get(userId);
            // ????????????????????????????????????????????????
            if (getModel != null) {
                String departName = getModel.get("departName").toString();
                getModel.put("departName", (departName + " | " + item.getDepartName()));
            } else {
                // ????????????????????????json???????????????????????????????????? json ???
                JSONObject json = JSON.parseObject(JSON.toJSONString(item));
                json.remove("id");
                json.put("userId", userId);
                json.put("departId", item.getDepartId());
                json.put("departName", item.getDepartName());
//                json.put("avatar", item.getSysUser().getAvatar());
                resultJson.add(json);
                hasUser.put(userId, json);
            }
        }

        IPage<JSONObject> result = new Page<>(pageNo, pageSize, pageList.getTotal());
        result.setRecords(resultJson.toJavaList(JSONObject.class));
        return Result.ok(result);
    }

    /**
     * ????????????????????????????????????
     */
    //@RequiresRoles({"admin"})
    @RequestMapping(value = "/editSysDepartWithUser", method = RequestMethod.POST)
    public Result<String> editSysDepartWithUser(@RequestBody SysDepartUsersVO sysDepartUsersVO) {
        Result<String> result = new Result<String>();
        try {
            String sysDepId = sysDepartUsersVO.getDepId();
            for(String sysUserId:sysDepartUsersVO.getUserIdList()) {
                SysUserDepart sysUserDepart = new SysUserDepart(null,sysUserId,sysDepId);
                QueryWrapper<SysUserDepart> queryWrapper = new QueryWrapper<SysUserDepart>();
                queryWrapper.eq("dep_id", sysDepId).eq("user_id",sysUserId);
                SysUserDepart one = sysUserDepartService.getOne(queryWrapper);
                if(one==null){
                    sysUserDepartService.save(sysUserDepart);
                }
            }
            result.setMessage("????????????!");
            result.setSuccess(true);
            return result;
        }catch(Exception e) {
            log.error(e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("?????????: " + e.getMessage());
            return result;
        }
    }

    /**
     *   ?????????????????????????????????
     */
    //@RequiresRoles({"admin"})
    @RequestMapping(value = "/deleteUserInDepart", method = RequestMethod.DELETE)
    public Result<SysUserDepart> deleteUserInDepart(@RequestParam(name="depId") String depId,
                                                    @RequestParam(name="userId",required=true) String userId
    ) {
        Result<SysUserDepart> result = new Result<SysUserDepart>();
        try {
            QueryWrapper<SysUserDepart> queryWrapper = new QueryWrapper<SysUserDepart>();
            queryWrapper.eq("dep_id", depId).eq("user_id",userId);
            boolean b = sysUserDepartService.remove(queryWrapper);
            if(b){
                List<SysDepartRole> sysDepartRoleList = departRoleService.list(new QueryWrapper<SysDepartRole>().eq("depart_id",depId));
                List<String> roleIds = sysDepartRoleList.stream().map(SysDepartRole::getId).collect(Collectors.toList());
                if(roleIds != null && roleIds.size()>0){
                    QueryWrapper<SysDepartRoleUser> query = new QueryWrapper<>();
                    query.eq("user_id",userId).in("drole_id",roleIds);
                    departRoleUserService.remove(query);
                }
                result.success("????????????!");
            }else{
                result.error500("??????????????????????????????????????????!");
            }
        }catch(Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }

    /**
     * ???????????????????????????????????????
     */
    //@RequiresRoles({"admin"})
    @RequestMapping(value = "/deleteUserInDepartBatch", method = RequestMethod.DELETE)
    public Result<SysUserDepart> deleteUserInDepartBatch(
            @RequestParam(name="depId") String depId,
            @RequestParam(name="userIds",required=true) String userIds) {
        Result<SysUserDepart> result = new Result<SysUserDepart>();
        try {
            QueryWrapper<SysUserDepart> queryWrapper = new QueryWrapper<SysUserDepart>();
            queryWrapper.eq("dep_id", depId).in("user_id",Arrays.asList(userIds.split(",")));
            boolean b = sysUserDepartService.remove(queryWrapper);
            if(b){
                departRoleUserService.removeDeptRoleUser(Arrays.asList(userIds.split(",")),depId);
            }
            result.success("????????????!");
        }catch(Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }
    
    /**
         *  ?????????????????????????????????/??????????????????
     * @return
     */
    @RequestMapping(value = "/getCurrentUserDeparts", method = RequestMethod.GET)
    public Result<Map<String,Object>> getCurrentUserDeparts() {
        Result<Map<String,Object>> result = new Result<Map<String,Object>>();
        try {
        	LoginUser sysUser = (LoginUser)SecurityUtils.getSubject().getPrincipal();
            List<SysDepart> list = this.sysDepartService.queryUserDeparts(sysUser.getId());
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("list", list);
            map.put("orgCode", sysUser.getOrgCode());
            result.setSuccess(true);
            result.setResult(map);
        }catch(Exception e) {
            log.error(e.getMessage(), e);
            result.error500("???????????????");
        }
        return result;
    }

    


	/**
	 * ??????????????????
	 * 
	 * @param jsonObject
	 * @param user
	 * @return
	 */
	@PostMapping("/register")
	public Result<JSONObject> userRegister(@RequestBody JSONObject jsonObject, SysUser user) {
		Result<JSONObject> result = new Result<JSONObject>();
		String phone = jsonObject.getString("phone");
		String smscode = jsonObject.getString("smscode");
		Object code = redisUtil.get(phone);
		String username = jsonObject.getString("username");
		//???????????????????????????????????????????????????
		if(oConvertUtils.isEmpty(username)){
            username = phone;
        }
        //?????????????????????????????????????????????
		String password = jsonObject.getString("password");
		if(oConvertUtils.isEmpty(password)){
            password = RandomUtil.randomString(8);
        }
		String email = jsonObject.getString("email");
		SysUser sysUser1 = sysUserService.getUserByName(username);
		if (sysUser1 != null) {
			result.setMessage("??????????????????");
			result.setSuccess(false);
			return result;
		}
		SysUser sysUser2 = sysUserService.getUserByPhone(phone);
		if (sysUser2 != null) {
			result.setMessage("?????????????????????");
			result.setSuccess(false);
			return result;
		}

		if(oConvertUtils.isNotEmpty(email)){
            SysUser sysUser3 = sysUserService.getUserByEmail(email);
            if (sysUser3 != null) {
                result.setMessage("??????????????????");
                result.setSuccess(false);
                return result;
            }
        }
        if(null == code){
            result.setMessage("???????????????????????????????????????");
            result.setSuccess(false);
            return result;
        }
		if (!smscode.equals(code.toString())) {
			result.setMessage("?????????????????????");
			result.setSuccess(false);
			return result;
		}

		try {
			user.setCreateTime(new Date());// ??????????????????
			String salt = oConvertUtils.randomGen(8);
			String passwordEncode = PasswordUtil.encrypt(username, password, salt);
			user.setSalt(salt);
			user.setUsername(username);
			user.setRealname(username);
			user.setPassword(passwordEncode);
			user.setEmail(email);
			user.setPhone(phone);
			user.setStatus(CommonConstant.USER_UNFREEZE);
			user.setDelFlag(CommonConstant.DEL_FLAG_0);
			user.setActivitiSync(CommonConstant.ACT_SYNC_0);
			sysUserService.addUserWithRole(user,"ee8626f80f7c2619917b6236f3a7f02b");//?????????????????? test
			result.success("????????????");
		} catch (Exception e) {
			result.error500("????????????");
		}
		return result;
	}

//	/**
//	 * ?????????????????????????????????????????????
//	 * @param
//	 * @return
//	 */
//	@GetMapping("/querySysUser")
//	public Result<Map<String, Object>> querySysUser(SysUser sysUser) {
//		String phone = sysUser.getPhone();
//		String username = sysUser.getUsername();
//		Result<Map<String, Object>> result = new Result<Map<String, Object>>();
//		Map<String, Object> map = new HashMap<String, Object>();
//		if (oConvertUtils.isNotEmpty(phone)) {
//			SysUser user = sysUserService.getUserByPhone(phone);
//			if(user!=null) {
//				map.put("username",user.getUsername());
//				map.put("phone",user.getPhone());
//				result.setSuccess(true);
//				result.setResult(map);
//				return result;
//			}
//		}
//		if (oConvertUtils.isNotEmpty(username)) {
//			SysUser user = sysUserService.getUserByName(username);
//			if(user!=null) {
//				map.put("username",user.getUsername());
//				map.put("phone",user.getPhone());
//				result.setSuccess(true);
//				result.setResult(map);
//				return result;
//			}
//		}
//		result.setSuccess(false);
//		result.setMessage("????????????");
//		return result;
//	}

	/**
	 * ?????????????????????
	 */
	@PostMapping("/phoneVerification")
	public Result<Map<String,String>> phoneVerification(@RequestBody JSONObject jsonObject) {
		Result<Map<String,String>> result = new Result<Map<String,String>>();
		String phone = jsonObject.getString("phone");
		String smscode = jsonObject.getString("smscode");
		Object code = redisUtil.get(phone);
		if (!smscode.equals(code)) {
			result.setMessage("?????????????????????");
			result.setSuccess(false);
			return result;
		}
		//??????????????????
		redisUtil.set(phone, smscode,600);
		//?????????????????????
		LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
        query.eq(SysUser::getPhone,phone);
        SysUser user = sysUserService.getOne(query);
        Map<String,String> map = new HashMap<>();
        map.put("smscode",smscode);
        map.put("username",user.getUsername());
        result.setResult(map);
		result.setSuccess(true);
		return result;
	}
	
	/**
	 * ??????????????????
	 */
	@GetMapping("/passwordChange")
	public Result<SysUser> passwordChange(@RequestParam(name="username")String username,
										  @RequestParam(name="password")String password,
			                              @RequestParam(name="smscode")String smscode,
			                              @RequestParam(name="phone") String phone) {
        Result<SysUser> result = new Result<SysUser>();
        if(oConvertUtils.isEmpty(username) || oConvertUtils.isEmpty(password) || oConvertUtils.isEmpty(smscode)  || oConvertUtils.isEmpty(phone) ) {
            result.setMessage("?????????????????????");
            result.setSuccess(false);
            return result;
        }

        SysUser sysUser=new SysUser();
        Object object= redisUtil.get(phone);
        if(null==object) {
        	result.setMessage("????????????????????????");
            result.setSuccess(false);
            return result;
        }
        if(!smscode.equals(object.toString())) {
        	result.setMessage("???????????????????????????");
            result.setSuccess(false);
            return result;
        }
        sysUser = this.sysUserService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername,username).eq(SysUser::getPhone,phone));
        if (sysUser == null) {
            result.setMessage("??????????????????");
            result.setSuccess(false);
            return result;
        } else {
            String salt = oConvertUtils.randomGen(8);
            sysUser.setSalt(salt);
            String passwordEncode = PasswordUtil.encrypt(sysUser.getUsername(), password, salt);
            sysUser.setPassword(passwordEncode);
            this.sysUserService.updateById(sysUser);
            result.setSuccess(true);
            result.setMessage("?????????????????????");
            return result;
        }
    }
	

	/**
	 * ??????TOKEN???????????????????????????????????????????????????????????????????????????????????????
	 * 
	 * @return
	 */
	@GetMapping("/getUserSectionInfoByToken")
	public Result<?> getUserSectionInfoByToken(HttpServletRequest request, @RequestParam(name = "token", required = false) String token) {
		try {
			String username = null;
			// ??????????????????token?????????header?????????token?????????????????????
			if (oConvertUtils.isEmpty(token)) {
				 username = JwtUtil.getUserNameByToken(request);
			} else {
				 username = JwtUtil.getUsername(token);				
			}

			log.debug(" ------ ?????????????????????????????????????????????????????? " + username);

			// ?????????????????????????????????
			SysUser sysUser = sysUserService.getUserByName(username);
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("sysUserId", sysUser.getId());
			map.put("sysUserCode", sysUser.getUsername()); // ??????????????????????????????
			map.put("sysUserName", sysUser.getRealname()); // ??????????????????????????????
			map.put("sysOrgCode", sysUser.getOrgCode()); // ??????????????????????????????

			log.debug(" ------ ?????????????????????????????????????????????????????????????????? " + map);

			return Result.ok(map);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return Result.error(500, "????????????:" + e.getMessage());
		}
	}
	
	/**
	 * ???APP??????????????????????????????  ??????????????????????????? ????????????
	 * @param keyword
	 * @param pageNo
	 * @param pageSize
	 * @return
	 */
	@GetMapping("/appUserList")
	public Result<?> appUserList(@RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "username", required = false) String username,
			@RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
			@RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
            @RequestParam(name = "syncFlow", required = false) String syncFlow) {
		try {
			//TODO ??????????????????????????????mp????????????page???????????? ???????????????????????????
			LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>();
			if(oConvertUtils.isNotEmpty(syncFlow)){
                query.eq(SysUser::getActivitiSync, CommonConstant.ACT_SYNC_1);
            }
			query.eq(SysUser::getDelFlag,CommonConstant.DEL_FLAG_0);
			if(oConvertUtils.isNotEmpty(username)){
			    if(username.contains(",")){
                    query.in(SysUser::getUsername,username.split(","));
                }else{
                    query.eq(SysUser::getUsername,username);
                }
            }else{
                query.and(i -> i.like(SysUser::getUsername, keyword).or().like(SysUser::getRealname, keyword));
            }
			Page<SysUser> page = new Page<>(pageNo, pageSize);
			IPage<SysUser> res = this.sysUserService.page(page, query);
			return Result.ok(res);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return Result.error(500, "????????????:" + e.getMessage());
		}
		
	}


    /**
     * ???????????????????????????   ????????????
     * @param
     * @return
     *
     */

    @GetMapping("/showChartsCount")
    public Result<?> showChartsCount(HttpServletRequest request, @RequestParam(name = "token", required = false) String token) {
        Result result=new Result();
        JSONObject json=new JSONObject();
        String username = null;
        // ??????????????????token?????????header?????????token?????????????????????
        if (oConvertUtils.isEmpty(token)) {
            username = JwtUtil.getUserNameByToken(request);
        } else {
            username = JwtUtil.getUsername(token);
        }
        List listDepIds=new ArrayList();
        String depId=sysUserService.getUserByName(username).getDepartIds();
        listDepIds.add(depId);
        int userCount=sysUserService.count();
        List userList=sysUserService.queryByDepIds(listDepIds,username);
        int userDepCount=userList.size();
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<SysUser>();
        int userStuCount=sysUserService.count(queryWrapper.eq("identity", 0));// identity 0??????????????? 1???????????? 2???????????????
        int inCount=sysUserService.count(queryWrapper.isNotNull("city_name").ne("city_name",""));
        //???????????????????????????
        QueryWrapper<SysUser> queryWrapper1 = new QueryWrapper<SysUser>();
        int sexManNum=sysUserService.count(queryWrapper1.eq("sex",1));
        int sexWoNum = userCount-sexManNum; //????????????????????? ???????????????????????? 1????????? 2?????????
        //?????? ????????????
        QueryWrapper<SysUser> queryWrapper2 = new QueryWrapper<SysUser>();
        int sexStuManNum = sysUserService.count(queryWrapper1.eq("identity", 0).eq("sex",1));
        int sexStuWoNum = userStuCount-sexStuManNum;
        QueryWrapper<SysUser> queryWrapper3 = new QueryWrapper<SysUser>();
        queryWrapper3.isNotNull("city_name").ne("city_name","").select("distinct city_name");
        List<SysUser> sysUsersList=sysUserService.list(queryWrapper3);
        Map<String, Integer> cityMap=new TreeMap<>();//???TreeMap??????
       for (SysUser sysUser:sysUsersList){
           QueryWrapper<SysUser> queryWrapper4 = new QueryWrapper<SysUser>();
           if (sysUser.getCityName()!=null){
               cityMap.put(sysUser.getCityName(),sysUserService.count(queryWrapper4.eq("city_name",sysUser.getCityName())));
           }
       }
        List<Map.Entry<String, Integer>> treeMapList = new ArrayList<Map.Entry<String, Integer>>(cityMap.entrySet());
        //??????value????????????
        Collections.sort(treeMapList, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue() - o1.getValue());
            }
        });
        //????????????map  ????????? key values ??????
        Map map=new LinkedHashMap();  // hashmap ??????????????? ?????????9 ???????????????8???????????????
        Map hotMap=new LinkedHashMap();
        List cityList=new LinkedList();
        List<Integer> cityNumList=new LinkedList();
        for (int i = 0; i < treeMapList.size(); i++) {
            //
            if (i>8){
                break;
            }
            if (treeMapList.get(i).getValue()>0){
                cityList.add(treeMapList.get(i).getKey());
//                cityList.add(i);
                cityNumList.add(treeMapList.get(i).getValue());
                map.put(treeMapList.get(i).getKey(),treeMapList.get(i).getValue());
            }
        }
        json.put("userCount",userCount); //????????????
        json.put("userStuCount",userStuCount);//???????????????
        json.put("userDepCount",userDepCount);//???????????????
        json.put("inCount",inCount); // ??????????????????
        json.put("sexManNum",sexManNum);
        json.put("sexWoNum",sexWoNum);
        json.put("sexStuManNum",sexStuManNum);
        json.put("sexStuWoNum",sexStuWoNum);
        String progress=((inCount*100)/userStuCount)+"%";
        json.put("progress",progress); //???????????????
        json.put("hotCity",map);
        json.put("cityMap",cityMap); //????????? ????????????12???????????????  ?????????????????? ?????????????????????

//      ??????app
        Map seriesMap=new LinkedHashMap();
        seriesMap.put("name","??????");
        seriesMap.put("data",cityNumList);
        hotMap.put("categories",cityList);
        List seriesList= new ArrayList<>();
        seriesList.add(seriesMap);
        hotMap.put("series",seriesList);
        json.put("Column",hotMap);

        result.setCode(200);
        result.setSuccess(true);
        result.setMessage("app web ???????????? ??????");
        result.setResult(json);
        return result;
    }


    //??????????????? ????????????????????????????????????
    @GetMapping(value = "/showOrgCount")
    public Result showOrgCount(HttpServletRequest request, @RequestParam(name = "token", required = false) String token) {
        Result result=new Result();
        String username = null;
        // ??????????????????token?????????header?????????token?????????????????????
        if (oConvertUtils.isEmpty(token)) {
            username = JwtUtil.getUserNameByToken(request);
        } else {
            username = JwtUtil.getUsername(token);
        }
        String userId=sysUserService.getUserByName(username).getId();
        //????????????
        QueryWrapper<SysUserDepart> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.isNotNull("user_id").eq("user_id", userId);
        String userDep = sysUserDepartService.getOne(queryWrapper1).getDepId();
        JSONObject json=new JSONObject();
        List listDepIds=new ArrayList();
        listDepIds.add(userDep);
//        QueryWrapper<SysUserDepart> queryWrapper2 = new QueryWrapper<>();
//        queryWrapper2.isNotNull("dep_id").eq("dep_id", userDep);
//        //??????????????????????????????id
//        List<SysUserDepart> userIds=sysUserDepartService.list(queryWrapper2);
        List<SysUser> userList=sysUserService.queryByDepIds(listDepIds,username);
        List<String>  userIdList=new ArrayList<>();
        for (SysUser sysUser:userList){
            if (sysUser.getIdentity()==0){
                userIdList.add(sysUser.getId());
            }
        }

        //??????????????????
        int userCount=userList.size();
        //??????????????????
        QueryWrapper<SysUser> queryWrapper2 = new QueryWrapper<>();
        int inCount=sysUserService.count(queryWrapper2.isNotNull("identity")
                .eq("identity",0).in("id",userIdList));
        //????????????
        int manCount=sysUserService.count(queryWrapper2.isNotNull("sex").eq("sex",1));
        //??????????????????
        int inCount1=sysUserService.count(queryWrapper2.isNotNull("city_name").ne("city_name",""));

        QueryWrapper<SysUser> queryWrapper3 = new QueryWrapper<>();
        queryWrapper3.isNotNull("city_name").ne("city_name","").select("distinct city_name");
        List<SysUser> sysUsersList=sysUserService.list(queryWrapper3);

        Map<String, Integer> cityMap=new TreeMap<>();//???TreeMap??????
        for (SysUser sysUser:sysUsersList){
            QueryWrapper<SysUser> queryWrapper4 = new QueryWrapper<>();
            if (sysUser.getCityName()!=null){
                cityMap.put(sysUser.getCityName(),sysUserService.count(queryWrapper4.in("id",userIdList).eq("city_name",sysUser.getCityName())));
            }
        }
        List<Map.Entry<String, Integer>> treeMapList =
                new ArrayList<Map.Entry<String, Integer>>(cityMap.entrySet());
        //??????value????????????
        Collections.sort(treeMapList, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return (o2.getValue() - o1.getValue());
            }
        });
        //????????????map
        Map map=new LinkedHashMap();  // hashmap ??????????????? ?????????10 ???????????????8???????????????
        for (int i = 0; i < treeMapList.size(); i++) {
            //
            if (i>9){
                break;
            }
            if (treeMapList.get(i).getValue()>0){
                map.put(treeMapList.get(i).getKey(),treeMapList.get(i).getValue());
            }
        }
        json.put("userCount",userCount); //???????????? ????????????  ????????????
        json.put("manCount",manCount);
        json.put("woCount",userCount-manCount);
        json.put("inCount",inCount); // ??????????????????
        json.put("inCount1",inCount1);
        json.put("inCount2",inCount-inCount1);
        String progress=((inCount*100)/userCount)+"%";
        json.put("progress",progress); //???????????????
        json.put("hotCity",map);
        result.setCode(200);
        result.setSuccess(true);
        result.setMessage("app web ?????? ??????");
        result.setResult(json);
        return result;
    }


    @GetMapping(value = "/showLineCount")
    public Result showLineCount(HttpServletRequest request, @RequestParam(name = "token", required = false) String token) {
        Result result=new Result();
        JSONObject json=new JSONObject();
        List<ShowLineCharts> showLineChartsList =sysUserService.showLineCharts();
        List lst=new ArrayList<>();
        for (ShowLineCharts s:showLineChartsList){
            Map map=new HashMap<>();
            if (s.getCityName()!=null && s.getCityName()!=""){
                map.put("type",s.getCityName());
                map.put("??????",s.getMan());
                map.put("??????",s.getWo());
                lst.add(map);
            }
        }
        json.put("line",lst);
        result.setCode(200);
        result.setSuccess(true);
        result.setMessage("app web echarts ?????????");
        result.setResult(json);
        return result;
    }


    /**
     * ???APP??????????????????????????????  ????????????????????????????????????
     * @param key
     * @return
     */
    @GetMapping("/appStuList")
    public Result<?> appStuList(@RequestParam(name = "keyword", required = true) String key,
                                 @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                 @RequestParam(name="pageSize", defaultValue="50") Integer pageSize,
                                 @RequestParam(name = "syncFlow", required = false) String syncFlow) {
        try {
            //TODO ??????????????????????????????mp????????????page???????????? ???????????????????????????
            LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<SysUser>();
            if(oConvertUtils.isNotEmpty(syncFlow)){
                query.eq(SysUser::getActivitiSync, CommonConstant.ACT_SYNC_1);
            }
            query.eq(SysUser::getDelFlag,CommonConstant.DEL_FLAG_0);
            Page<SysUser> page = new Page<>(pageNo, pageSize);
            IPage<SysUser> res = this.sysUserService.page(page, query);
            return Result.ok(res);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Result.error(500, "????????????:" + e.getMessage());
        }

    }


    /**
     * ????????????????????????????????????????????????
     *
     * @return logicDeletedUserList
     */
    @GetMapping("/recycleBin")
    public Result getRecycleBin() {
        List<SysUser> logicDeletedUserList = sysUserService.queryLogicDeleted();
        if (logicDeletedUserList.size() > 0) {
            // ?????????????????????????????????
            // step.1 ?????????????????? userIds
            List<String> userIds = logicDeletedUserList.stream().map(SysUser::getId).collect(Collectors.toList());
            // step.2 ?????? userIds?????????????????????????????????????????????
            Map<String, String> useDepNames = sysUserService.getDepNamesByUserIds(userIds);
            logicDeletedUserList.forEach(item -> item.setOrgCode(useDepNames.get(item.getId())));
        }
        return Result.ok(logicDeletedUserList);
    }

    /**
     * ??????????????????????????????
     *
     * @param jsonObject
     * @return
     */
    @RequestMapping(value = "/putRecycleBin", method = RequestMethod.PUT)
    public Result putRecycleBin(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        String userIds = jsonObject.getString("userIds");
        if (StringUtils.isNotBlank(userIds)) {
            SysUser updateUser = new SysUser();
            updateUser.setUpdateBy(JwtUtil.getUserNameByToken(request));
            updateUser.setUpdateTime(new Date());
            sysUserService.revertLogicDeleted(Arrays.asList(userIds.split(",")), updateUser);
        }
        return Result.ok("????????????");
    }

    /**
     * ??????????????????
     *
     * @param userIds ??????????????????ID?????????id?????????????????????
     * @return
     */
    //@RequiresRoles({"admin"})
    @RequestMapping(value = "/deleteRecycleBin", method = RequestMethod.DELETE)
    public Result deleteRecycleBin(@RequestParam("userIds") String userIds) {
        if (StringUtils.isNotBlank(userIds)) {
            sysUserService.removeLogicDeleted(Arrays.asList(userIds.split(",")));
        }
        return Result.ok("????????????");
    }


    /**
     * ???????????????????????????
     * @param jsonObject
     * runrab ?????? ???????????? ???????????????
     * ????????????????????????????????????
     * @return
     */
    @RequestMapping(value = "/appEdit", method = {RequestMethod.PUT,RequestMethod.POST})
    public Result<SysUser> appEdit(HttpServletRequest request,@RequestBody JSONObject jsonObject) {
        Result<SysUser> result = new Result<SysUser>();
        try {
            //?????????????????????????????????
            String username=null;
            if(null!=jsonObject.getString("role")){
                username = jsonObject.getString("username");
            }else {
                // ?????????????????? ????????????
                if(jsonObject.getString("username")!=null){
                    username = jsonObject.getString("username");
                }else {
                    username = JwtUtil.getUserNameByToken(request);
                }
            }
//            System.out.println(username);
//            String username = JwtUtil.getUserNameByToken(request);
            SysUser sysUser = sysUserService.getUserByName(username);
            baseCommonService.addLog("????????????????????????id??? " +jsonObject.getString("id") ,CommonConstant.LOG_TYPE_2, 2);
            String realname=jsonObject.getString("realname");
            String avatar=jsonObject.getString("avatar");
            String sex=jsonObject.getString("sex");
            String phone=jsonObject.getString("phone");
            String email=jsonObject.getString("email");
            Date birthday=jsonObject.getDate("birthday");

            Integer visible=0; //??????????????????
            if(jsonObject.getString("visible")==null || jsonObject.getString("visible")==""){
                visible = Integer.valueOf(0);
            }else {
                visible=Integer.valueOf(jsonObject.getString("visible"));
            }

            String cityName=jsonObject.getString("cityName"); //runrab ???????????????


            SysUser userPhone = sysUserService.getUserByPhone(phone);
            if(sysUser==null) {
                result.error500("?????????????????????!");
            }else {
                if(userPhone!=null){
                    String userPhonename = userPhone.getUsername();
                    if(!userPhonename.equals(username)){
                        result.error500("??????????????????!");
                        return result;
                    }
                }
                if(StringUtils.isNotBlank(realname)){
                    sysUser.setRealname(realname);
                }
                if(StringUtils.isNotBlank(avatar)){
                    sysUser.setAvatar(avatar);
                }
                if(StringUtils.isNotBlank(sex)){
                    sysUser.setSex(Integer.parseInt(sex));
                }
                if(StringUtils.isNotBlank(phone)){
                    sysUser.setPhone(phone);
                }
                if(StringUtils.isNotBlank(email)){
                    sysUser.setEmail(email);
                }
                if(null != birthday){
                    sysUser.setBirthday(birthday);
                }
                if(null != visible){
                    sysUser.setVisible(visible);
                }
                if(null != cityName){
                    sysUser.setCityName(cityName);
                }
                sysUser.setUpdateTime(new Date());
                sysUserService.updateById(sysUser);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("????????????!");
        }
        return result;
    }

    /**
     * ????????? ???????????? ????????????
     * @param jsonObject
     * @return
     */
    @RequestMapping(value = "/appGoInfo", method = {RequestMethod.PUT,RequestMethod.POST})
    public Result<SysUser> appGoInfo(HttpServletRequest request,@RequestBody JSONObject jsonObject) {
        Result<SysUser> result = new Result<SysUser>();
        try {
            String username = JwtUtil.getUserNameByToken(request);
            SysUser sysUser = sysUserService.getUserByName(username);
//            System.out.println(jsonObject.toJSONString());
            sysUser.setCityName(jsonObject.getString("city_name"));
            sysUserService.updateById(sysUser);
//            Date birthday=jsonObject.getDate("birthday");
//            SysUser userPhone = sysUserService.getUserByPhone(phone);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("????????????!");
        }
        return result;
    }



    /**
     * ???????????????????????????
     * @param jsonObject
     * @return
     */
    @RequestMapping(value = "/restPassword", method = {RequestMethod.PUT,RequestMethod.POST})
    public Result restPassword(HttpServletRequest request,@RequestBody JSONObject jsonObject) {
        Result result = new Result();
        try {
            String username = JwtUtil.getUserNameByToken(request);
//          String username=jsonObject.getString("username");
            SysUser sysUser = sysUserService.getUserByName(username);
            String oldPassword=jsonObject.getString("oldpassword");
            String password=jsonObject.getString("password");
            String confirmPassword=jsonObject.getString("confirmpassword");
            System.out.println(username);
            System.out.println(oldPassword);
            System.out.println(password);
            System.out.println(confirmPassword);
            if(sysUser==null) {
                result.error500("?????????????????????!");
            }else {
                if(password.equals(confirmPassword)){
                    sysUserService.resetPassword(username,oldPassword,password,confirmPassword);
                    result.setMessage("??????????????????");
                }else{
                    result.setMessage("??????????????????,??????????????????????????????");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("??????????????????!");
        }
        return result;
    }


    /**
     * ???????????????????????? ??????????????????
     * @param jsonObject
     * @return
     */
    @RequestMapping(value = "/noLoginPassword", method = {RequestMethod.PUT,RequestMethod.POST})
    public Result noLoginPassword(HttpServletRequest request,@RequestBody JSONObject jsonObject) {
        Result result = new Result();
        try {
//            String username = JwtUtil.getUserNameByToken(request);
            String username=jsonObject.getString("username");
            SysUser sysUser = sysUserService.getUserByName(username);
            String oldPassword=jsonObject.getString("oldpassword");
            String password=jsonObject.getString("password");
            String confirmpassword=jsonObject.getString("confirmpassword");
            if(sysUser==null) {
                result.error500("?????????????????????!");
            }else {
                //(oldPassword.toString()==sysUser.getPassword())&(password==confirmpassword)
                if(oldPassword.toString()==sysUser.getPassword()&(password==confirmpassword)){
                    sysUserService.resetPassword(username,oldPassword,password,confirmpassword);
//                    sysUserService.changePassword(sysUser);
                    result.setMessage("??????????????????");
                }else{
                    result.setMessage("??????????????????,????????????");
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("??????????????????!");
        }
        return result;
    }


    /**
     * ???????????????????????????
     * @param clientId
     * @return
     */
    @RequestMapping(value = "/saveClientId", method = RequestMethod.GET)
    public Result<SysUser> saveClientId(HttpServletRequest request,@RequestParam("clientId")String clientId) {
        Result<SysUser> result = new Result<SysUser>();
        try {
            String username = JwtUtil.getUserNameByToken(request);
            SysUser sysUser = sysUserService.getUserByName(username);
            if(sysUser==null) {
                result.error500("?????????????????????!");
            }else {
                sysUser.setClientId(clientId);
                sysUserService.updateById(sysUser);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            result.error500("????????????!");
        }
        return result;
    }
    /**
     * ??????userid???????????????????????????????????????
     *
     * @return Result
     */
    @GetMapping("/queryChildrenByUsername")
    public Result queryChildrenByUsername(@RequestParam("userId") String userId) {
        //??????????????????
        Map<String,Object> map=new HashMap<String,Object>();
        SysUser sysUser = sysUserService.getById(userId);
        String username = sysUser.getUsername();
        Integer identity = sysUser.getUserIdentity();
        map.put("sysUser",sysUser);
//         && identity==2
        if(identity!=null){
            //????????????????????????
            String departIds = sysUser.getDepartIds();
            if(StringUtils.isNotBlank(departIds)){
                List<String> departIdList = Arrays.asList(departIds.split(","));
                List<SysUser> childrenUser = sysUserService.queryByDepIds(departIdList,username);
                map.put("children",childrenUser);
            }
        }
        return Result.ok(map);
    }
    /**
     * ?????????????????????????????????
     * @param departId
     * @return
     */
    @GetMapping("/appQueryByDepartId")
    public Result<List<SysUser>> appQueryByDepartId(@RequestParam(name="departId", required = false) String departId) {
        Result<List<SysUser>> result = new Result<List<SysUser>>();
        List<String> list=new ArrayList<String> ();
        list.add(departId);
        List<SysUser> childrenUser = sysUserService.queryByDepIds(list,null);
        result.setResult(childrenUser);
        return result;
    }
    /**
     * ?????????????????? ??????id ????????????????????? ??????????????? ??????
     *
     * */
    @GetMapping("/appQueryUserByDepartId")
    public Result<List<SysUser>> appQueryUserByDepartId(@RequestParam(name="departId", required = true) String departId) {
        Result<List<SysUser>> result = new Result<List<SysUser>>();
        List<String> list=new ArrayList<String> ();
//        list.add(departId);
//        sysUserService.queryByDepIds(list,null)
        List<SysUser> childrenUser = sysUserDepartService.queryUserByDepId(departId);
        result.setResult(childrenUser);
        return result;
    }


    /**
     * ???????????????????????????(???????????????????????????)
     * @param keyword
     * @return
     */
    @GetMapping("/appQueryUser")
    public Result<List<SysUser>> appQueryUser(@RequestParam(name = "keyword", required = false) String keyword,
                                              @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
                                              @RequestParam(name="pageSize", defaultValue="10") Integer pageSize) {
        Result<List<SysUser>> result = new Result<List<SysUser>>();
        LambdaQueryWrapper<SysUser> queryWrapper =new LambdaQueryWrapper<SysUser>();
        //TODO ????????????????????????????????????????????????
        queryWrapper.ne(SysUser::getUsername,"_reserve_user_external");
        if(StringUtils.isNotBlank(keyword)){
            queryWrapper.and(i -> i.like(SysUser::getUsername, keyword).or().like(SysUser::getRealname, keyword));
        }
        Page<SysUser> page = new Page<>(pageNo, pageSize);
        IPage<SysUser> pageList = this.sysUserService.page(page, queryWrapper);
        //?????????????????????????????????
        //step.1 ?????????????????? useids
        //step.2 ?????? useids?????????????????????????????????????????????
        List<String> userIds = pageList.getRecords().stream().map(SysUser::getId).collect(Collectors.toList());
        if(userIds!=null && userIds.size()>0){
            Map<String,String>  useDepNames = sysUserService.getDepNamesByUserIds(userIds);
            pageList.getRecords().forEach(item->{
                item.setOrgCodeTxt(useDepNames.get(item.getId()));
            });
        }
        result.setResult(pageList.getRecords());
        return result;
    }

    /**
     * ??????????????????????????????
     * @param json
     * @return
     */
    @RequestMapping(value = "/updateMobile", method = RequestMethod.PUT)
    public Result<?> changMobile(@RequestBody JSONObject json,HttpServletRequest request) {
        String smscode = json.getString("smscode");
        String phone = json.getString("phone");
        Result<SysUser> result = new Result<SysUser>();
        //?????????????????????
        String username = JwtUtil.getUserNameByToken(request);
        if(oConvertUtils.isEmpty(username) || oConvertUtils.isEmpty(smscode) || oConvertUtils.isEmpty(phone)) {
            result.setMessage("????????????????????????");
            result.setSuccess(false);
            return result;
        }
        Object object= redisUtil.get(phone);
        if(null==object) {
            result.setMessage("????????????????????????");
            result.setSuccess(false);
            return result;
        }
        if(!smscode.equals(object.toString())) {
            result.setMessage("???????????????????????????");
            result.setSuccess(false);
            return result;
        }
        SysUser user = sysUserService.getUserByName(username);
        if(user==null) {
            return Result.error("??????????????????");
        }
        user.setPhone(phone);
        sysUserService.updateById(user);
        return Result.ok("?????????????????????!");
    }


    /**
     * ?????????????????????????????????in?????? ?????????????????? ??????????????????
     * @param sysUser
     * @return
     */
    @GetMapping("/getMultiUser")
    public List<SysUser> getMultiUser(SysUser sysUser){
        QueryWrapper<SysUser> queryWrapper = QueryGenerator.initQueryWrapper(sysUser, null);
        //update-begin---author:wangshuai ---date:20220104  for???[JTC-297]???????????????????????????????????????------------
        queryWrapper.eq("status",Integer.parseInt(CommonConstant.STATUS_1));
        //update-end---author:wangshuai ---date:20220104  for???[JTC-297]???????????????????????????????????????------------
        List<SysUser> ls = this.sysUserService.list(queryWrapper);
        for(SysUser user: ls){
            user.setPassword(null);
            user.setSalt(null);
        }
        return ls;
    }

    /**
     * ???????????? ????????????
     *
     * */
    @GetMapping("/setAutoUser")
    public Result setAutoUser(@RequestParam(name="selectedRoles")String key1,
                              @RequestParam(name="selectedDeparts")String key2){
        Result result=new Result<>();
        String selectedRoles=null;
        String selectedDeparts=null;
        if (key1==null){
            selectedRoles = "1260924539346472962";
        }else{
            selectedRoles = key1;
        }
        if (key2==null){
            selectedDeparts = "c6d7cb4deeac411cb3384b1b31278596";
        }else{
            selectedDeparts = key1;
        }
//        String selectedRoles = "1260924539346472962";
//        String selectedDeparts ="c6d7cb4deeac411cb3384b1b31278596";
        try {
            SysUser user = new SysUser();
            user.setCityName(DataGenerator.cityName());//city
            user.setPhone(DataGenerator.genPhone());//phone
            user.setCreateBy("admin");
            user.setVisible(0);
            user.setEmail(DataGenerator.email(false));//email ??????
            user.setIdentity(0); //0 ????????????
            int sex=DataGenerator.sex();
            if(sex!=1){
                sex=2;
            }
//            System.out.println(sex);
            user.setSex(Integer.valueOf(sex));
            user.setRealname(DataGenerator.name());//realename
            user.setUsername(DataGenerator.userName());
            user.setId(DataGenerator.idCard());
            user.setCreateTime(new Date());//??????????????????
            String salt = oConvertUtils.randomGen(8);
            user.setSalt(salt);
            String passwordEncode = PasswordUtil.encrypt(user.getUsername(), "123456", salt);
            user.setPassword(passwordEncode);
            user.setStatus(1);//sate
            user.setDelFlag(CommonConstant.DEL_FLAG_0);
            // ?????????????????????service ????????????
            sysUserService.saveUser(user, selectedRoles, selectedDeparts);
            result.setSuccess(true);
            result.setCode(200);
        } catch (Exception e) {
            e.printStackTrace();
//            System.out.println(e.getMessage());
//            System.out.println("??????????????????");
            result.setSuccess(false);
            result.setMessage("??????????????????");
        }
        return result;
    }



}
