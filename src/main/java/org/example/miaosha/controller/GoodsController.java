package org.example.miaosha.controller;

import com.alibaba.druid.util.StringUtils;
import org.example.miaosha.access.AccessLimit;
import org.example.miaosha.domain.MiaoshaUser;
import org.example.miaosha.redis.GoodsKey;
import org.example.miaosha.redis.RedisService;
import org.example.miaosha.result.Result;
import org.example.miaosha.service.GoodsService;
import org.example.miaosha.service.MiaoshaUserService;
import org.example.miaosha.vo.GoodsDetailVo;
import org.example.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    ThymeleafViewResolver thymeleafViewResolver;

//    @Autowired
//    GoodsService goodsService;

//    @RequestMapping("/to_list")
//    public String list(HttpServletResponse response, Model model,
//                       @CookieValue(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false)String cookieToken,
//                       @RequestParam(value = MiaoshaUserService.COOKIE_NAME_TOKEN, required = false)String paramToken) {
//
//        if(StringUtils.isEmpty(cookieToken) && StringUtils.isEmpty(paramToken)){
//            return "login";
//        }
//        String token = StringUtils.isEmpty(paramToken) ? cookieToken : paramToken;
//        MiaoshaUser user = userService.getByToken(response, token);
//
//        // 向前端传数据
//        model.addAttribute("user", user);
//        return "goods_list";
//    }

    @AccessLimit(seconds = 10, maxCount = 5, needLogin = true)
    @RequestMapping(value = "/to_list", produces = "text/html")
    @ResponseBody
    public String list(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user) {

        //      页面缓存
        // 取缓存
        String html = redisService.get(GoodsKey.getGoodsList, "", String.class);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }

        // 向前端传数据--User
        model.addAttribute("user", user);
        // 查询商品列表
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        // 向前端传数据--GoodsList
        model.addAttribute("goodsList", goodsList);
        IWebContext ctx = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        // 手动渲染
        html = thymeleafViewResolver.getTemplateEngine().process("goods_list", ctx);
        if (!StringUtils.isEmpty(html)) {
            redisService.set(GoodsKey.getGoodsList, "", html);
        }
        return html;
    }

    @AccessLimit(seconds = 10, maxCount = 5, needLogin = true)
    @RequestMapping(value = "/detail/{goodsId}")
    @ResponseBody
    public Result<GoodsDetailVo> detail(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user,
                                        @PathVariable("goodsId") long goodsId) {
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();
        int miaoshaStatus = 0;
        int remainSeconds = 0;
        if (now < startAt) {//秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int) ((startAt - now) / 1000);
        } else if (now > endAt) {//秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        } else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        GoodsDetailVo vo = new GoodsDetailVo();
        vo.setGoods(goods);
        vo.setUser(user);
        vo.setRemainSeconds(remainSeconds);
        vo.setMiaoshaStatus(miaoshaStatus);
        return Result.success(vo);
    }


    @RequestMapping(value = "/to_detail2/{goodsId}", produces = "text/html")
    @ResponseBody
    public String detail2(HttpServletRequest request, HttpServletResponse response, Model model, MiaoshaUser user,
                          @PathVariable("goodsId") long goodsId) {

        //    URl缓存 --与页面缓存相似
        // 取缓存
        String html = redisService.get(GoodsKey.getGoodsDetail, "" + goodsId, String.class);
        if (!StringUtils.isEmpty(html)) {
            return html;
        }
        model.addAttribute("user", user);
        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
        model.addAttribute("goods", goods);

        long startAt = goods.getStartDate().getTime();
        long endAt = goods.getEndDate().getTime();
        long now = System.currentTimeMillis();

        int miaoshaStatus = 0;
        int remainSeconds = 0;

        if (now < startAt) {        // 秒杀还没开始，倒计时
            miaoshaStatus = 0;
            remainSeconds = (int) ((startAt - now) / 1000);
        } else if (now > endAt) {     //秒杀已经结束
            miaoshaStatus = 2;
            remainSeconds = -1;
        } else {//秒杀进行中
            miaoshaStatus = 1;
            remainSeconds = 0;
        }
        model.addAttribute("miaoshaStatus", miaoshaStatus);
        model.addAttribute("remainSeconds", remainSeconds);
        IWebContext ctx = new WebContext(request, response, request.getServletContext(), request.getLocale(), model.asMap());
        // 手动渲染
        html = thymeleafViewResolver.getTemplateEngine().process("goods_detail", ctx);
        if (!StringUtils.isEmpty(html)) {
            redisService.set(GoodsKey.getGoodsDetail, "" + goodsId, html);
        }
        return html;
    }


//    @RequestMapping("/to_detail/{goodsId}")
//    public String detail(Model model,MiaoshaUser user,
//                         @PathVariable("goodsId")long goodsId) {
//        model.addAttribute("user", user);
//
//        GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
//        model.addAttribute("goods", goods);
//
//        long startAt = goods.getStartDate().getTime();
//        long endAt = goods.getEndDate().getTime();
//        long now = System.currentTimeMillis();
//
//        int miaoshaStatus = 0;
//        int remainSeconds = 0;
//
//        if(now < startAt ) {        // 秒杀还没开始，倒计时
//            miaoshaStatus = 0;
//            remainSeconds = (int)((startAt - now )/1000);
//        }else  if(now > endAt){     //秒杀已经结束
//            miaoshaStatus = 2;
//            remainSeconds = -1;
//        }else {//秒杀进行中
//            miaoshaStatus = 1;
//            remainSeconds = 0;
//        }
//        model.addAttribute("miaoshaStatus", miaoshaStatus);
//        model.addAttribute("remainSeconds", remainSeconds);
//        return "goods_detail";
//    }

}

