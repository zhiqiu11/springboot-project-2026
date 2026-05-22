package com.example.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import com.example.entity.Account;
import com.example.entity.Recharge;
import com.example.entity.User;
import com.example.exception.CustomException;
import com.example.mapper.RechargeMapper;
import com.example.mapper.UserMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 业务处理
 **/
@Service
public class RechargeService {

    @Resource
    private RechargeMapper rechargeMapper;
    @Resource
    private UserMapper userMapper;

    /**
     * 新增充值记录并更新用户账户余额
     * 此方法处理用户充值业务逻辑，包括更新用户账户余额和插入充值记录
     * 使用事务确保数据一致性，充值金额变更和充值记录插入要么全部成功，要么全部回滚
     */
    @Transactional
    public void add(Recharge recharge) {
        // 获取充值用户的ID
        Integer userId = recharge.getUserId();
        // 根据用户ID查询用户信息
        User user = userMapper.selectById(userId);
        // 将充值金额累加到用户现有账户余额中
        user.setAccount(user.getAccount().add(recharge.getMoney()));
        // 更新用户账户余额到数据库
        userMapper.updateById(user);

        // 设置充值时间为当前时间
        recharge.setTime(DateUtil.now());
        // 插入充值记录到数据库
        rechargeMapper.insert(recharge);
    }

    /**
     * 删除
     */
    public void deleteById(Integer id) {
        rechargeMapper.deleteById(id);
    }

    /**
     * 修改
     */
    public void updateById(Recharge recharge) {
        rechargeMapper.updateById(recharge);
    }

    /**
     * 根据ID查询
     */
    public Recharge selectById(Integer id) {
        return rechargeMapper.selectById(id);
    }

    /**
     * 查询所有
     */
    public List<Recharge> selectAll(Recharge recharge) {
        return rechargeMapper.selectAll(recharge);
    }

    /**
     * 分页查询
     */
    public PageInfo<Recharge> selectPage(Recharge recharge, Integer pageNum, Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Recharge> list = rechargeMapper.selectAll(recharge);
        return PageInfo.of(list);
    }
}