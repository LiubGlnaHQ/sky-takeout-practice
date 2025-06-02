package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class SetmealServiceImpl implements SetmealService {
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
     private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    /**
     *  新增套餐
     * @param setmealDTO
     */
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.insert(setmeal);
        // 获取生成的套餐id
        Long setmealId = setmeal.getId();
        // 获取套餐内的菜品集合
        List<SetmealDish>setmealDishes=setmealDTO.getSetmealDishes();
        // 为菜品设置套餐id
        setmealDishes.forEach( setmealDish -> {
             setmealDish.setSetmealId(setmealId);
        });
        //套餐菜品关系表 批量插入
        setmealDishMapper.insertBatch(setmealDishes);
    }
    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageNum = setmealPageQueryDTO.getPage();
        int pageSize= setmealPageQueryDTO.getPageSize();
        //开启分页查询
        PageHelper.startPage(pageNum,pageSize);
        //查询套餐
         Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
         //封装pageResult 返回
        return new PageResult(
                page.getTotal(),
                page.getResult());
    }
    /**
     * 批量删除套餐
     * @param ids
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        // 1. 套餐是否启售
        for(Long id:ids){
            Setmeal setmeal=setmealMapper.getById(id);
            if(Objects.equals(setmeal.getStatus(), StatusConstant.ENABLE)){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        // 2. 删除套餐表中的数据
        setmealMapper.deleteBatch(ids);
        // 3. 删除套餐和菜品的关联数据
        setmealDishMapper.deleteBySetmealId(ids);
    }
    /**
     * 根据id查询套餐及对应菜品 用于修改时数据回显
     * @param id
     * @return
     */
    @Override
    public SetmealVO getByIdWithDish(Long id) {
        // 1. 查询套餐数据
        Setmeal setmeal=setmealMapper.getById(id);
        // 2. 查询套餐对应的菜品数据
        List<SetmealDish> setmealDishes=setmealDishMapper.getBySetmealId(id);
        SetmealVO setmealVO=new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        // 设置套餐对应的菜品数据
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }
    /**
     * 修改套餐
     * @param setmealDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(SetmealDTO setmealDTO) {
        // 1. 修改套餐的基本信息
        Setmeal setmeal=new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);
        // 2. 删除套餐原有 对应的菜品数据
        Long setmealId=setmealDTO.getId();
        setmealDishMapper.deleteBySetmealId(Collections.singletonList(setmealId));
        // 3. 添加新的菜品数据 到 套餐菜品关系表
        List<SetmealDish> setmealDishes=setmealDTO.getSetmealDishes();
        //为菜品设置套餐id
        setmealDishes.forEach( setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        setmealDishMapper.insertBatch(setmealDishes);
    }
    /**
     * 起售、停售套餐
     * @param status
     * @param id
     * @return
     */
    @Override
    public void startOrStop(Integer status, Long id) {

        // 1. 启售套餐时，获取套餐内的菜品，判断是否停售
        if(Objects.equals(status, StatusConstant.ENABLE)){
            List<SetmealDish>setmealDishes=setmealDishMapper.getBySetmealId(id);
            if(setmealDishes != null && !setmealDishes.isEmpty()){
                // 套餐内的菜品存在停售菜品，则不能启售套餐
                setmealDishes.forEach(setmealDish -> {
                    Long dishId = setmealDish.getDishId();
                    Integer dishStatus = dishMapper.getById(dishId).getStatus();
                    if(Objects.equals(dishStatus, StatusConstant.DISABLE)){
                        throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        //  2. 套餐状态修改
        Setmeal setmeal=setmealMapper.getById(id);
        setmeal.setStatus(status);
        setmealMapper.update(setmeal);
    }
}
