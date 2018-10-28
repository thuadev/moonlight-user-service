package com.thuadev.core.pms.biz;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import wusc.edu.pay.common.page.PageBean;
import wusc.edu.pay.common.page.PageParam;
import wusc.edu.pay.core.pms.dao.PmsOperatorDao;
import wusc.edu.pay.core.pms.dao.PmsRoleOperatorDao;
import wusc.edu.pay.facade.pms.entity.PmsOperator;
import wusc.edu.pay.facade.pms.entity.PmsRoleOperator;
import wusc.edu.pay.facade.pms.enums.PmsOperatorTypeEnum;


/**
 * 
 * @描述: 操作员表--服务层接口 .
 * @作者: WuShuicheng .
 * @创建时间: 2013-7-25,下午10:41:04 .
 * @版本: 1.0 .
 */
@Service("pmsOperatorBiz")
public class PmsOperatorBiz {

	@Autowired
	private PmsOperatorDao pmsOperatorDao;

	@Autowired
	private PmsRoleOperatorDao pmsRoleOperatorDao;

	/**
	 * 根据登录名取得操作员对象
	 */
	public PmsOperator getOperatorByLoginName(String loginName) {
		return pmsOperatorDao.getByLoginName(loginName);
	}

	/**
	 * 根据ID删除一个操作员，同时删除与该操作员关联的角色关联信息（注：超级管理员和默认操作员不能删除）.
	 * @param operatorId 操作员ID.
	 */
	public void deleteOperatorById(long operatorId) {
		PmsOperator pmsOperator = pmsOperatorDao.getById(operatorId);
		if (pmsOperator != null) {
			if (PmsOperatorTypeEnum.SUPER_ADMIN.getValue().equals(pmsOperator.getType()) || PmsOperatorTypeEnum.DEFAULT_USER.getValue().equals(pmsOperator.getType())){
				throw new RuntimeException("操作员[" + pmsOperator.getLoginName() + "]不能删除");
			}
			// 删除原来的角色与操作员关联
			pmsRoleOperatorDao.deleteByOperatorId(operatorId);
			// 删除操作员信息
			pmsOperatorDao.deleteById(pmsOperator.getId());
		}
	}

	/**
	 * 根据角色ID查询关联到此角色的操作员信息.
	 * 
	 * @param roleId 角色ID.
	 * @return operatorList.
	 */
	public List<PmsOperator> listOperatorByRoleId(long roleId) {
		return pmsOperatorDao.listByRoleId(roleId);
	}
	
	/**
	 * 更新操作员信息.
	 * @param operator
	 */
	public void update(PmsOperator operator) {
		pmsOperatorDao.update(operator);
		
	}
	
	/**
	 * 根据操作员ID更新操作员密码.
	 * @param operatorId 操作员ID.
	 * @param newPwd 新密码(已进行SHA1加密).
	 * @param isChangedPwd 密码是否已修改.
	 */
	public void updateOperatorPwd(Long operatorId, String newPwd, boolean isChangedPwd) {
		PmsOperator pmsOperator = pmsOperatorDao.getById(operatorId);
		pmsOperator.setLoginPwd(newPwd);
		pmsOperator.setPwdErrorCount(0); // 密码错误次数重置为0
		pmsOperator.setIsChangedPwd(isChangedPwd); // 设置密码为已修改过
		pmsOperatorDao.update(pmsOperator);
	}

	/**
	 * 根据ID获取操作员信息.
	 * @param operatorId
	 * @return
	 */
	public PmsOperator getById(Long operatorId) {
		return pmsOperatorDao.getById(operatorId);
	}

	/**
	 * 查询并分页列出操作员信息.
	 * @param pageParam
	 * @param paramMap
	 * @return
	 */
	public PageBean listPage(PageParam pageParam, Map<String, Object> paramMap) {
		return pmsOperatorDao.listPage(pageParam, paramMap);
	}

	/**
	 * 保存操作员信息.
	 * @param pmsOperator
	 */
	public void create(PmsOperator pmsOperator) {
		pmsOperatorDao.insert(pmsOperator);
		
	}
	
	/**
	 * 保存操作員信息及其关联的角色.
	 * 
	 * @param pmsOperator
	 *            .
	 * @param roleOperatorStr
	 *            .
	 */
	public void saveOperator(PmsOperator pmsOperator, String roleOperatorStr) {
		// 保存操作员信息
		pmsOperatorDao.insert(pmsOperator);
		// 保存角色关联信息
		if (StringUtils.isNotBlank(roleOperatorStr) && roleOperatorStr.length() > 0) {
			saveOrUpdateRoleOperator(pmsOperator.getId(), roleOperatorStr);
		}
	}
	
	/**
	 * 保存用户和角色之间的关联关系
	 */
	private void saveOrUpdateRoleOperator(long operatorId, String roleIdsStr) {
		// 删除原来的角色与操作员关联
		List<PmsRoleOperator> listPmsRoleOperators = pmsRoleOperatorDao.listByOperatorId(operatorId);
		Map<Long, PmsRoleOperator> delMap = new HashMap<Long, PmsRoleOperator>();
		for (PmsRoleOperator pmsRoleOperator : listPmsRoleOperators) {
			delMap.put(pmsRoleOperator.getRoleId(), pmsRoleOperator);
		}
		if (StringUtils.isNotBlank(roleIdsStr)) {
			// 创建新的关联
			String[] roleIds = roleIdsStr.split(",");
			for (int i = 0; i < roleIds.length; i++) {
				long roleId = Long.parseLong(roleIds[i]);
				if (delMap.get(roleId) == null) {
					PmsRoleOperator pmsRoleOperator = new PmsRoleOperator();
					pmsRoleOperator.setOperatorId(operatorId);
					pmsRoleOperator.setRoleId(roleId);
					pmsRoleOperatorDao.insert(pmsRoleOperator);
				} else {
					delMap.remove(roleId);
				}
			}
		}

		Iterator<Long> iterator = delMap.keySet().iterator();
		while (iterator.hasNext()) {
			long roleId = iterator.next();
			pmsRoleOperatorDao.deleteByRoleIdAndOperatorId(roleId, operatorId);
		}
	}

	
	/**
	 * 修改操作員信息及其关联的角色.
	 * 
	 * @param pmsOperator
	 *            .
	 * @param roleOperatorStr
	 *            .
	 */
	public void updateOperator(PmsOperator pmsOperator, String roleOperatorStr) {
		pmsOperatorDao.update(pmsOperator);
		// 更新角色信息
		saveOrUpdateRoleOperator(pmsOperator.getId(), roleOperatorStr);
	}
	
	/**
	 * 根据角色ID统计有多少个操作员关联到此角色.
	 * 
	 * @param roleId
	 *            .
	 * @return count.
	 */
	public int countOperatorByRoleId(Long roleId) {
		List<PmsRoleOperator> operatorList = pmsRoleOperatorDao.listByRoleId(roleId);
		if (operatorList == null || operatorList.isEmpty()) {
			return 0;
		} else {
			return operatorList.size();
		}
	}

}