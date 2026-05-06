package com.test.mall.mapper;

import com.test.mall.model.Employees;
import com.test.mall.model.EmployeesExample;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface EmployeesMapper {
    long countByExample(EmployeesExample example);

    int deleteByExample(EmployeesExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(Employees row);

    int insertSelective(Employees row);

    List<Employees> selectByExample(EmployeesExample example);

    Employees selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("row") Employees row, @Param("example") EmployeesExample example);

    int updateByExample(@Param("row") Employees row, @Param("example") EmployeesExample example);

    int updateByPrimaryKeySelective(Employees row);

    int updateByPrimaryKey(Employees row);
}