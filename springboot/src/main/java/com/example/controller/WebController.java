package com.example.controller;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.example.common.config.Result;
import com.example.common.enums.ResultCodeEnum;
import com.example.common.enums.RoleEnum;
import com.example.entity.*;
import com.example.mapper.OrderDetailMapper;
import com.example.common.enums.OrderStatusEnum;
import com.example.service.*;
import com.example.utils.SaUtils;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;



@RestController
public class WebController {

    @Resource
    private AdminService adminService;
    @Resource
    private UserService userService;
    @Resource
    private OrdersService ordersService;
    @Resource
    private GoodsService goodsService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private OrderDetailMapper orderDetailMapper;
    /**
     * 默认请求接口
     */
    @GetMapping("/")
    public Result hello() {
        return Result.success();
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result login(@RequestBody Account account) {
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            Admin admin = (Admin) adminService.login(account);
            String token = SaUtils.loginAndGetToken(admin.getId(), admin.getRole());  // 添加这一行
            admin.setToken(token);
            return Result.success(admin);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            User user = (User) userService.login(account);
            String token = SaUtils.loginAndGetToken(user.getId(), user.getRole());
            user.setToken(token);
            return Result.success(user);
        }
        return Result.error(ResultCodeEnum.LOGIN_FAILED);
    }

    /**
     * 验证Token
     */
    @GetMapping("/validateToken")
    public Result validateToken() {
        if(RoleEnum.USER.name().equals(SaUtils.getLoginRole())){
            User user = SaUtils.getLoginUser();
            if (user != null) {
                return Result.success(user);
            }
        } else if(RoleEnum.ADMIN.name().equals(SaUtils.getLoginRole())){
            Admin admin = SaUtils.getLoginAdmin();
            if (admin != null) {
                return Result.success(admin);
            }
        }
        return Result.error(ResultCodeEnum.TOKEN_EXPIRED);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result logout() {
        SaUtils.logout();
        return Result.success("登出成功");
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if(!user.getPassword().equals(user.getNewPassword())) {
            return Result.error(ResultCodeEnum.PASSWORD_NOT_MATCH);
        }
        userService.add(user);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        if (RoleEnum.ADMIN.name().equals(account.getRole())) {
            adminService.updatePassword(account);
        }
        if (RoleEnum.USER.name().equals(account.getRole())) {
            userService.updatePassword(account);
        }
        return Result.success();
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/count")
    public Result count() {
        List<Orders> ordersList = ordersService.selectAll(null).stream().filter(orders -> !orders.getStatus().equals(OrderStatusEnum.CANCEL.getDesc())).toList();
        String todayDate = DateUtil.today();

        BigDecimal total = ordersList.stream().map(Orders::getTotal).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        BigDecimal today = ordersList.stream().filter(orders -> orders.getTime().contains(todayDate)).map(Orders::getTotal).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
        Integer goods = goodsService.selectAll(null).size();
        Integer user = userService.selectByAll(null).size();

        Map<String, Object> map = new HashMap<>();
        map.put("total", total);
        map.put("today", today);
        map.put("goods", goods);
        map.put("user", user);
        return Result.success(map);
    }

    /**
     * 获取折线图数据
     */
    @GetMapping("/selectLine")
    public Result selectLine() {
        Date  date = new Date();
        DateTime start = DateUtil.offsetDay(date, -6);
        List<DateTime> dateTimes = DateUtil.rangeToList(start, date, DateField.DAY_OF_YEAR);
        List<String> dateStringList = dateTimes.stream().map(dateTime -> DateUtil.format(dateTime, "MM-dd")).sorted().toList();

        //获取当前年份
        int year = DateUtil.year(date);

        ArrayList<BigDecimal> countList = new ArrayList<>();
        List<Orders> ordersList = ordersService.selectAll(null).stream().filter(orders -> !orders.getStatus().equals(OrderStatusEnum.CANCEL.getDesc())).toList();
        for (String day : dateStringList) {
            //包含年月日的订单
            BigDecimal total = ordersList.stream().filter(orders -> orders.getTime().contains(String.valueOf(year)) && orders.getTime().contains(day))
                    .map(Orders::getTotal).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
            countList.add(total);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("date", dateStringList);
        map.put("count", countList);
        return Result.success(map);
    }

    @GetMapping("/selectPie")
    public Result selectPie() {
        List<Map<String, Object>> list = new ArrayList<>();
        List<Category> categoryList = categoryService.selectAll(null);
        Map<String, Object> map;
        for(Category category : categoryList){
            map = new HashMap<>();
            map.put("name", category.getName());

            BigDecimal total = BigDecimal.ZERO;
            List<OrderDetail> orderDetailList = orderDetailMapper.selectAll(null);
            for(OrderDetail orderDetail : orderDetailList){
                Integer orderId = orderDetail.getOrderId();
                Orders orders = ordersService.selectById(orderId);
                if(!(OrderStatusEnum.CANCEL.getDesc()).equals(orders.getStatus())){
                    Integer goodsId = orderDetail.getGoodsId();
                    Goods goods = goodsService.selectById(goodsId);
                    if(goods.getCategoryId().equals(category.getId())){
                        total = total.add(orders.getTotal());
                    }
                }
            }
            map.put("value", total);
            //total大于0才添加到list
            if(total.compareTo(BigDecimal.ZERO) > 0){
                list.add(map);
            }
        }
        return Result.success(list);
    }
}