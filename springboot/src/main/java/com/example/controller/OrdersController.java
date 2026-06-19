package com.example.controller;

import com.example.common.config.Result;
import com.example.common.enums.RoleEnum;
import com.example.entity.Cart;
import com.example.entity.OrderDetail;
import com.example.entity.Orders;
import com.example.service.GroupService;
import com.example.service.OrdersService;
import com.example.service.SeckillService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端操作接口
 **/
@RestController
@RequestMapping("/orders")
public class OrdersController {

    @Resource
    private OrdersService ordersService;

    @Resource
    private SeckillService seckillService;

    @Resource
    private GroupService groupService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Orders orders) {
        ordersService.add(orders);
        return Result.success();
    }

    /**
     * 新增秒杀订单
     */
    @PostMapping("/addFlashOrder")
    public Result addFlashOrder(@RequestBody Orders orders) {
        seckillService.addFlashOrder(orders);
        return Result.success();
    }

    /**
     * 新增团购订单
     */
    @PostMapping("/addGroupOrder")
    public Result addGroupOrder(@RequestBody Orders orders) {
        groupService.addGroupOrder(orders);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        ordersService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Orders orders) {
        ordersService.updateById(orders);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Orders orders = ordersService.selectById(id);
        return Result.success(orders);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Orders orders) {
        List<Orders> list = ordersService.selectAll(orders);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Orders orders,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                              @RequestParam(defaultValue = "USER") String role) {
        PageInfo<Orders> page = ordersService.selectPage(orders, pageNum, pageSize, role);
        return Result.success(page);
    }

}