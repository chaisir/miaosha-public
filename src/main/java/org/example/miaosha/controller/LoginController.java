package org.example.miaosha.controller;


import com.alibaba.druid.util.StringUtils;
import org.example.miaosha.domain.User;
import org.example.miaosha.redis.RedisService;
import org.example.miaosha.redis.UserKey;
import org.example.miaosha.result.CodeMsg;
import org.example.miaosha.result.Result;
import org.example.miaosha.service.MiaoshaUserService;
import org.example.miaosha.service.UserService;
import org.example.miaosha.util.ValidatorUtil;
import org.example.miaosha.vo.LoginVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;


@Controller
@RequestMapping("/login")
public class LoginController {

    private static Logger log = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @RequestMapping("/to_login")
    public String toLogin() {
        return "login";
    }

    @RequestMapping("/do_login")
    @ResponseBody
    public Result<String> doLogin(HttpServletResponse response, @Valid LoginVo loginVo) {

        log.info(loginVo.toString());

        // 参数校验 --是否为空、手机号格式
//        String passInput =  loginVo.getPassword();
//        String mobile = loginVo.getMobile();
//        if(StringUtils.isEmpty(passInput)){
//            return Result.error(CodeMsg.PASSWORD_EMPTY);
//        }
//        if(StringUtils.isEmpty(mobile)){
//            return Result.error(CodeMsg.MOBILE_EMPTY);
//        }
//        if(!ValidatorUtil.isMobile(mobile)){
//            return Result.error(CodeMsg.MOBILE_ERROR);
//        }

        // login
//        CodeMsg cm = userService.login(loginVo);
//        if(cm.getCode() == 0) {
//            return Result.success(true);
//        }else{
//            return Result.error(cm);
//        }

        // login
        String token = userService.login(response, loginVo);
        return Result.success(token);
    }

}

