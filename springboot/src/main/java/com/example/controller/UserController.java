package com.example.controller;

import com.example.common.Result;
import com.example.entity.Admin;
import com.example.entity.User;
import com.example.service.UserService;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false)String name,
                             @RequestParam(required = false)Integer id){
        PageInfo<User> pageInfo = userService.selectPage(pageNum, pageSize, name, id);
        return Result.success(pageInfo);
    };

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id){
        userService.deleteById(id);
        return Result.success();
    }


    @PostMapping("/add")
    public Result add(@RequestBody User user){
        userService.add(user);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody User user){
        userService.update(user);
        return Result.success();
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById/{id}")
    public Result selectById(@PathVariable Integer id) {
        User user = userService.selectById(id);
        return Result.success(user);
    }
}
