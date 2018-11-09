package com.felix.demo.util;


public class StringUtil {


    /**
     * 首字母小写
     * @param str
     * @return
     */
    public static String lowerFirstCase(String str) {
        char[] chars = str.toCharArray();

        if (chars[0] >= 'A' && chars[0] <= 'Z') {
            chars[0] += 32;
        }

        return String.valueOf(chars);
    }
}
