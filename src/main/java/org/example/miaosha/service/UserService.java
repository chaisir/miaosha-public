package org.example.miaosha.service;


import org.example.miaosha.dao.UserMapper;
import org.example.miaosha.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UserService {

    @Autowired
    UserMapper userMapper;

    public User getById(int id) {
        return userMapper.getById(id);
    }


    public boolean tx() {
        User u1 = new User();
        u1.setId(7);
        u1.setName("photo wall");
        u1.setAlexa("100");
        u1.setUrl("www.chaiwensong.top");
        u1.setCountry("china");
        userMapper.insert(u1);

        return true;
    }
}
