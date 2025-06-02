package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增菜品和对应口味
     *
     * @param dishDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithFlavor(DishDTO dishDTO) {
        // 新建一个Dish对象
        Dish dish = new Dish();
        // 属性拷贝
        BeanUtils.copyProperties(dishDTO, dish);
        // 向菜品表插入1条数据
        dishMapper.insert(dish);
        // 获取insert生成的主键值
        Long dishId = dish.getId();
        // 拿到口味数组
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            // 遍历 flavors
            flavors.forEach(
                    (flavor) -> {
                        // 设置口味的菜品id
                        flavor.setDishId(dishId);
                    }
            );
            // 向口味表批量插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }
    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO
     * @return
     */
     public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
         PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
         Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
          return new PageResult(
                 page.getTotal(),
                 page.getResult());
     }
    /**
     * 批量删除菜品
     * @param ids
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        //1. 判断当前菜品是否能够删除-是否存在启售中
        for (Long id : ids) {
            Dish dish=dishMapper.getById(id);
            if(Objects.equals(dish.getStatus(), StatusConstant.ENABLE)){
                // 处于启售中，不能删除
                throw  new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //2. 是否被套餐关联
        List<Long>setmealIds=setmealDishMapper.getSetmealIdsByDishIds(ids);
         if(setmealIds != null && !setmealIds.isEmpty()){
            // 处于启售中，不能删除
            throw  new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //3. 删除菜品表中的数据
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            //4. 删除菜品关联的口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }

        // 根据菜品id批量删除菜品数据
        dishMapper.deleteByIds(ids);
        // 根据菜品id批量删除关联的口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }
    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        // 根据菜品id查询菜品数据
        Dish dish = dishMapper.getById(id);
        // 根据菜品id查询口味数据
        List<DishFlavor>dishFlavors=dishFlavorMapper.getByDishId(id);
        // 将查询到的数据封装到dishVO中
        DishVO dishVO=new DishVO();
        //属性拷贝
        BeanUtils.copyProperties(dish,dishVO);
        // 设置口味数据
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 根据id修改菜品的基本信息和口味信息
     * @param dishDTO
     * @return
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改基本信息
        Dish dish=new Dish();
        BeanUtils.copyProperties(dishDTO,dish);
        dishMapper.update(dish);
        //删除原有的口味信息
        Long dishId=dishDTO.getId();
        dishFlavorMapper.deleteByDishId(dishId);
        // 拿到口味数组
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            // 遍历 flavors
            flavors.forEach(
                    (flavor) -> {
                        // 设置口味的菜品id
                        flavor.setDishId(dishId);
                    }
            );
            // 向口味表批量插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }
    }
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<Dish> getDishByCategoryId(Long categoryId) {
        Dish dish= Dish.builder().categoryId( categoryId)
                .status( StatusConstant.ENABLE).build();
         return dishMapper.list(dish);
    }
    /**
     * 起售、停售菜品
     * @param status
     * @param id
     * @return
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Dish dish=dishMapper.getById(id);
        if(Objects.equals(dish.getStatus(),status)){
            //如果菜品状态没有发生变化，则直接返回
            return;
        }
        dish.setStatus(status);
        dishMapper.update(dish);
    }
}
