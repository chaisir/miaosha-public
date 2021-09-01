package org.example.miaosha.controller;


import org.example.miaosha.domain.User;
import org.example.miaosha.rabbitmq.MQSender;
import org.example.miaosha.redis.RedisService;
import org.example.miaosha.redis.UserKey;
import org.example.miaosha.result.CodeMsg;
import org.example.miaosha.result.Result;
import org.example.miaosha.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


@Controller
public class SampleController {

    @Autowired
    UserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    MQSender sender;

    @RequestMapping("/hello")
    @ResponseBody
    public Result<String> hello() {
        return Result.success("helhaha,chaisir");
    }

    @RequestMapping("/helloerror")
    @ResponseBody
    public Result<String> helloerror() {
        return Result.error(CodeMsg.SERVER_ERROR);
    }

    @RequestMapping("/db/get")
    @ResponseBody
    public Result<User> dbGet() {
        User user = userService.getById(1);
        return Result.success(user);
    }


    //事务
//    @RequestMapping("/db/tx")
//    @ResponseBody
//    public Result <Boolean> dbTx(){
//        userService.tx();
//        return Result.success(Boolean.TRUE);
//    }

//    @RequestMapping("/redis/get")
//    @ResponseBody
//    public Result <String> redisGet(){
//        String str1=redisService.get("chai10080",String.class);
//        return Result.success(str1);
//
//    }

    @RequestMapping("/redis/get")
    @ResponseBody
    public Result<User> redisGet() {
        User user = redisService.get(UserKey.getById, "" + 1, User.class);
        return Result.success(user);

    }

//    @RequestMapping("/redis/set")
//    @ResponseBody
//    public Result <String> redisSet(){
//        boolean res=redisService.set("key2","hello,chaisir!nicesafkalsjflk;fjds;ljejflk;jiofjdslkfjlsk;dfjdfkd kfkljfl;kdsf;osdaf;asdjfkl;sj");
//        String str=redisService.get("key2",String.class);
//        return Result.success(str);
//
//    }

    @RequestMapping("/redis/set")
    @ResponseBody
    public Result<Boolean> redisSet() {
        User user = new User();
        user.setId(1);
        user.setName("111111");
        redisService.set(UserKey.getById, "" + 1, user);
        return Result.success(true);

    }

//    @RequestMapping("/db")
//    @ResponseBody
//    public User dbGet(){
//        User user=userService.getById(1);
//        return user;
//    }


//    @RequestMapping("/mq/header")
//    @ResponseBody
//    public Result<String> header() {
//		sender.sendHeader("hello,imooc");
//        return Result.success("Hello，world");
//    }
//
//	@RequestMapping("/mq/fanout")
//    @ResponseBody
//    public Result<String> fanout() {
//		sender.sendFanout("hello,imooc");
//        return Result.success("Hello，world");
//    }
//
//	@RequestMapping("/mq/topic")
//    @ResponseBody
//    public Result<String> topic() {
//		sender.sendTopic("hello,imooc");
//        return Result.success("Hello，world");
//    }
//
//	@RequestMapping("/mq")
//    @ResponseBody
//    public Result<String> mq() {
//		sender.send("hello,imooc");
//        return Result.success("Hello,world");
//    }
}
