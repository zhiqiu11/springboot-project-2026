package com.example.controller;

import com.example.common.Result;
import com.example.entity.Cart;
import com.example.service.CartService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 前端操作接口
 **/
@RestController
@RequestMapping("/cart")
public class CartController {

    @Resource
    private CartService cartService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Cart cart) {
        cartService.add(cart);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete/{id}")
    public Result deleteById(@PathVariable Integer id) {
        cartService.deleteById(id);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result updateById(@RequestBody Cart cart) {
        cartService.updateById(cart);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        Cart cart = cartService.selectById(id);
        return Result.success(cart);
    }

    /**
     * 查询所有
     */
    @GetMapping("/selectAll")
    public Result selectAll(Cart cart) {
        List<Cart> list = cartService.selectAll(cart);
        return Result.success(list);
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(Cart cart,
                             @RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize) {
        PageInfo<Cart> page = cartService.selectPage(cart, pageNum, pageSize);
        return Result.success(page);
    }

    // ========== Redis优化接口 ==========

    /**
     * 根据用户ID查询购物车（Redis优先）
     */
    @GetMapping("/selectByUserId")
    public Result selectByUserId(@RequestParam Integer userId) {
        List<Cart> list = cartService.selectByUserId(userId);
        return Result.success(list);
    }

    /**
     * 根据用户ID和商品ID删除
     */
    @DeleteMapping("/delete")
    public Result deleteByGoodsId(@RequestParam Integer userId, @RequestParam Integer goodsId) {
        cartService.deleteByGoodsId(userId, goodsId);
        return Result.success();
    }

    /**
     * 更新数量
     */
    @PutMapping("/updateNum")
    public Result updateNum(@RequestParam Integer userId, @RequestParam Integer goodsId, @RequestParam Integer num) {
        cartService.updateNum(userId, goodsId, num);
        return Result.success();
    }

    /**
     * 删除用户所有购物车
     */
    @DeleteMapping("/deleteByUserId/{userId}")
    public Result deleteByUserId(@PathVariable Integer userId) {
        cartService.deleteByUserId(userId);
        return Result.success();
    }

}