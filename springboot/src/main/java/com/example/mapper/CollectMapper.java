package com.example.mapper;

import com.example.entity.Collect;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作collect相关数据接口
*/
public interface CollectMapper {

    /**
      * 新增
    */
    int insert(Collect collect);

    /**
      * 删除
    */
    int deleteById(Integer id);

    /**
      * 修改
    */
    int updateById(Collect collect);

    /**
      * 根据ID查询
    */
    Collect selectById(Integer id);

    /**
      * 查询所有
    */
    List<Collect> selectAll(Collect collect);

    @Select("select * from collect where goods_id = #{goodsId} and user_id = #{userId}")
    Collect selectByGoodsIdAndUserId(@Param("goodsId") Integer goodsId, @Param("userId") Integer userId);

    @Delete("delete from collect where goods_id = #{goodsId} and user_id = #{userId}")
    void deleteByGoodsIdAndUserId(@Param("goodsId") Integer goodsId, @Param("userId") Integer userId);

    @Delete("delete from collect where user_id = #{userId}")
    void deleteByUserId(@Param("userId") Integer userId);
}