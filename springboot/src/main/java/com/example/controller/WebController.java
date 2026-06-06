package com.example.controller;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.example.common.Result;
import com.example.entity.*;
import com.example.mapper.OrderDetailMapper;
import com.example.service.*;
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
    private TokenService tokenService;
    @Resource
    OrdersService ordersService;
    @Resource
    GoodsService goodsService;
    @Resource
    CategoryService categoryService;
    @Resource
    OrderDetailMapper orderDetailMapper;
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
        if ("管理员".equals(account.getRole())) {
            Admin admin = (Admin) adminService.login(account);
            return Result.success(admin);
        }
        if ("普通用户".equals(account.getRole())) {
            User user = (User) userService.login(account);
            String token = tokenService.generateToken(user);
            user.setToken(token);
            return Result.success(user);
        }
        return Result.error("登录失败，用户不存在");
    }

    /**
     * 验证Token
     */
    @GetMapping("/validateToken")
    public Result validateToken(@RequestHeader(value = "token", required = false) String token) {
        User user = tokenService.validateToken(token);
        if (user != null) {
            return Result.success(user);
        }
        return Result.error("Token无效或已过期");
    }

    /**
     * 登出（支持 header 和 body 两种方式传 token）
     */
    @PostMapping("/logout")
    public Result logout(
            @RequestHeader(value = "token", required = false) String headerToken,
            @RequestBody(required = false) Map<String, String> body) {
        
        String token = headerToken;
        if (token == null || token.isEmpty()) {
            if (body != null && body.containsKey("token")) {
                token = body.get("token");
            }
        }
        
        if (token == null || token.isEmpty()) {
            return Result.error("请先登录");
        }
        tokenService.invalidateToken(token);
        return Result.success("登出成功");
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        if(!user.getPassword().equals(user.getNewPassword())) {
            return Result.error("两次输入的密码不一致！请重新输入");
        }
        userService.add(user);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/updatePassword")
    public Result updatePassword(@RequestBody Account account) {
        if ("管理员".equals(account.getRole())) {
            adminService.updatePassword(account);
        }
        if ("普通用户".equals(account.getRole())) {
            userService.updatePassword(account);
        }
        return Result.success();
    }

    /**
     * 获取统计数据
     */
    @GetMapping("/count")
    public Result count() {
        List<Orders> ordersList = ordersService.selectAll(null).stream().filter(orders -> !orders.getStatus().equals("已取消")).toList();
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
        List<Orders> ordersList = ordersService.selectAll(null).stream().filter(orders -> !orders.getStatus().equals("已取消")).toList();
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
                if(!("已取消").equals(orders.getStatus())){
                    Integer goodsId = orderDetail.getGoodsId();
                    Goods goods = goodsService.selectById(goodsId);
                    if(goods.getCategoryId().equals(category.getId())){
                        total = total.add(orders.getTotal());
                    }
                };
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