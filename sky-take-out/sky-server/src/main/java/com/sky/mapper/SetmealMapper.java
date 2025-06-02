package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);
    /**
     * 新增套餐, 同时插入数据到 setmeal和 setmeal_dish表
     * @param setmeal
     */
    @AutoFill(value = OperationType.INSERT)
    void insert(Setmeal setmeal);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    /**
     * 根据套餐id获取套餐
     * @param id
     * @return
     */
    @Select("select  * from setmeal where id=#{id}")
    Setmeal getById(Long id);

    /**
     * 批量删除 套餐数据
     * @param ids
     */
    void deleteBatch(List<Long> ids);
    /**
     * 修改套餐
     * @param setmeal
     * @return
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Setmeal setmeal);
}
