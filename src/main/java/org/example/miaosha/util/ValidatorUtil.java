package org.example.miaosha.util;

import com.alibaba.druid.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidatorUtil {

    private static final Pattern mobile_pattern = Pattern.compile("1\\d{10}");

    public static boolean isMobile(String src) {
        if (StringUtils.isEmpty(src)) {
            return false;
        }
        Matcher m = mobile_pattern.matcher(src);
        return m.matches();
    }

    public static void main(String[] args) {
        System.out.println(isMobile("18434886184"));
        System.out.println(isMobile("12345678"));
    }
}
