package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    /**
     * 新增套餐
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);
    /**
     * 批量删除套餐
     * @param ids
     * @return
     */
    void deleteBatch(List<Long> ids);
    /**
     * 根据id查询套餐及对应菜品
     * @param id
     * @return
     */
    SetmealVO getByIdWithDish(Long id);
    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    void update(SetmealDTO setmealDTO);
    /**
     * 起售、停售套餐
     * @param status
     * @param id
     * @return
     */
    void startOrStop(Integer status, Long id);
}
