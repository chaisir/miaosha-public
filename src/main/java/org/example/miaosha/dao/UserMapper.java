package org.example.miaosha.dao;


import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.miaosha.domain.User;

@Mapper
public interface UserMapper {
    //    @Select("select * from websites where id=#{id}")
    public User getById(@Param("id") int id);

    //    @Insert("insert into websites(id,name,url,alexa,country)values(#{id},#{name},#{url},#{alexa},#{country})")
    public int insert(User user);
}
