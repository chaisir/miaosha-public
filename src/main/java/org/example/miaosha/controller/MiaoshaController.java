package org.example.miaosha.controller;

import org.example.miaosha.access.AccessLimit;
import org.example.miaosha.domain.MiaoshaOrder;
import org.example.miaosha.domain.MiaoshaUser;
import org.example.miaosha.domain.OrderInfo;
import org.example.miaosha.rabbitmq.MQSender;
import org.example.miaosha.rabbitmq.MiaoshaMessage;
import org.example.miaosha.redis.*;
import org.example.miaosha.result.CodeMsg;
import org.example.miaosha.result.Result;
import org.example.miaosha.service.GoodsService;
import org.example.miaosha.service.MiaoshaService;
import org.example.miaosha.service.MiaoshaUserService;
import org.example.miaosha.service.OrderService;
import org.example.miaosha.vo.GoodsVo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;


@Controller
@RequestMapping("/miaosha")
public class MiaoshaController implements InitializingBean {

    @Autowired
    MiaoshaUserService userService;

    @Autowired
    RedisService redisService;

    @Autowired
    GoodsService goodsService;

    @Autowired
    OrderService orderService;

    @Autowired
    MiaoshaService miaoshaService;

    @Autowired
    MQSender sender;

    // 秒杀结束标志---default：false表示秒杀没有结束
    private HashMap<Long, Boolean> localOverMap = new HashMap<Long, Boolean>();


    /**
     * 系统初始化
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        List<GoodsVo> goodsList = goodsService.listGoodsVo();
        if (goodsList == null) {
            return;
        }
        for (GoodsVo goods : goodsList) {
            redisService.set(GoodsKey.getMiaoshaGoodsStock, "" + goods.getId(), goods.getStockCount());
            localOverMap.put(goods.getId(), false);
        }
    }

    /**
     * 生成验证码
     * 图片和答案，图片返回给前端，答案放入redis
     *
     * @param response
     * @param user
     * @param goodsId
     * @return
     */
    @RequestMapping(value = "/verifyCode", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaVerifyCode(HttpServletResponse response, MiaoshaUser user,
                                               @RequestParam("goodsId") long goodsId) {
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        try {
            BufferedImage image = miaoshaService.createVerifyCode(user, goodsId);
            OutputStream out = response.getOutputStream();
            ImageIO.write(image, "JPEG", out);
            out.flush();
            out.close();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(CodeMsg.MIAOSHA_FAIL);
        }
    }

    /**
     * 获取秒杀路径
     * 路径放入redis
     *
     * @param request
     * @param user
     * @param goodsId
     * @param verifyCode
     * @return
     */
    @AccessLimit(seconds = 10, maxCount = 5, needLogin = true)
    @RequestMapping(value = "/path", method = RequestMethod.GET)
    @ResponseBody
    public Result<String> getMiaoshaPath(HttpServletRequest request, MiaoshaUser user,
                                         @RequestParam("goodsId") long goodsId,
                                         @RequestParam(value = "verifyCode", defaultValue = "0") int verifyCode) {

        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }

        // 查询访问次数
//		String uri = request.getRequestURI();
//		String key = uri + "_" + user.getId();
//		Integer count = redisService.get(AccessKey.access, key , Integer.class);
//		if(count == null) {
//			redisService.set(AccessKey.access, key, 1);
//		}else if(count < 3) {
//			redisService.incr(AccessKey.access, key);
//		}else {
//			return Result.error(CodeMsg.ACCESS_LIMIT_REACHED);
//		}


        boolean check = miaoshaService.checkVerifyCode(user, goodsId, verifyCode);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }

        String path = miaoshaService.createMiaoshaPath(user, goodsId);
        return Result.success(path);
    }


    /**
     * 执行秒杀
     * 根据上一步生成的秒杀路径来执行秒杀
     *
     * @param model
     * @param user
     * @param goodsId
     * @param path
     * @return
     */
    @RequestMapping(value = "/{path}/do_miaosha", method = RequestMethod.POST)
    @ResponseBody
    public Result<Integer> miaosha(Model model, MiaoshaUser user,
                                   @RequestParam("goodsId") long goodsId,
                                   @PathVariable("path") String path) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        // 验证path
        boolean check = miaoshaService.checkPath(user, goodsId, path);
        if (!check) {
            return Result.error(CodeMsg.REQUEST_ILLEGAL);
        }
        // 内存标记（秒杀是否结束---true表示已经结束），减少redis访问
        boolean over = localOverMap.get(goodsId);
        if (over) {
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        // 预减库存
        long stock = redisService.decr(GoodsKey.getMiaoshaGoodsStock, "" + goodsId);
        if (stock < 0) {
            localOverMap.put(goodsId, true);
            return Result.error(CodeMsg.MIAO_SHA_OVER);
        }
        // 判断是否已经秒杀到了（是否重复秒杀）
        MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
        if (order != null) {
            return Result.error(CodeMsg.REPEAT_MIAOSHA);
        }
        // 入队
        MiaoshaMessage mm = new MiaoshaMessage();
        mm.setUser(user);
        mm.setGoodsId(goodsId);
        sender.sendMiaoshaMessage(mm);
        // 排队中
        return Result.success(0);

		/*
		//判断库存
		GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);//10个商品，req1 req2
		int stock = goods.getStockCount();
		if(stock <= 0) {
			return Result.error(CodeMsg.MIAO_SHA_OVER);
		}
		//判断是否已经秒杀到了
		MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
		if(order != null) {
			return Result.error(CodeMsg.REPEAT_MIAOSHA);
		}
		//减库存 下订单 写入秒杀订单
		OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
		return Result.success(orderInfo);
		*/
    }

    /**
     * orderId：成功
     * -1：秒杀失败
     * 0： 排队中
     */
    /**
     * @param model
     * @param user
     * @param goodsId
     * @return orderId:成功  -1:秒杀失败  0:排队中
     */
    @RequestMapping(value = "/result", method = RequestMethod.GET)
    @ResponseBody
    public Result<Long> miaoshaResult(Model model, MiaoshaUser user,
                                      @RequestParam("goodsId") long goodsId) {
        model.addAttribute("user", user);
        if (user == null) {
            return Result.error(CodeMsg.SESSION_ERROR);
        }
        long result = miaoshaService.getMiaoshaResult(user.getId(), goodsId);
        return Result.success(result);
    }


//	@RequestMapping(value="/reset", method=RequestMethod.GET)
//	@ResponseBody
//	public Result<Boolean> reset(Model model) {
//		List<GoodsVo> goodsList = goodsService.listGoodsVo();
//		for(GoodsVo goods : goodsList) {
//			goods.setStockCount(10);
//			redisService.set(GoodsKey.getMiaoshaGoodsStock, ""+goods.getId(), 10);
//			localOverMap.put(goods.getId(), false);
//		}
//		redisService.delete(OrderKey.getMiaoshaOrderByUidGid);
//		redisService.delete(MiaoshaKey.isGoodsOver);
//		miaoshaService.reset(goodsList);
//		return Result.success(true);
//	}


//	@RequestMapping("/do_miaosha")
//    public String list(Model model, MiaoshaUser user,
//					   @RequestParam("goodsId")long goodsId) {
//    	model.addAttribute("user", user);
//    	if(user == null) {
//    		return "login";
//    	}
//    	// 判断库存
//    	GoodsVo goods = goodsService.getGoodsVoByGoodsId(goodsId);
//    	int stock = goods.getStockCount();
//    	if(stock <= 0) {
//    		model.addAttribute("errmsg", CodeMsg.MIAO_SHA_OVER.getMsg());
//    		return "miaosha_fail";
//    	}
//    	// 判断是否已经秒杀到了
//    	MiaoshaOrder order = orderService.getMiaoshaOrderByUserIdGoodsId(user.getId(), goodsId);
//    	if(order != null) {
//    		model.addAttribute("errmsg", CodeMsg.REPEAT_MIAOSHA.getMsg());
//    		return "miaosha_fail";
//    	}
//    	// 减库存 下订单 写入秒杀订单
//    	OrderInfo orderInfo = miaoshaService.miaosha(user, goods);
//    	model.addAttribute("orderInfo", orderInfo);
//    	model.addAttribute("goods", goods);
//        return "order_detail";
//    }
}
