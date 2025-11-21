package com.eq.autopops;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * create time 2024/11/12 15:00
 * 文件说明
 *
 * @author xuejiaming
 */
public class MyStrUtil {
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isBlank(String str) {
        if (isEmpty(str)) {
            return true;
        }
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if ((!Character.isWhitespace(str.charAt(i)))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }


    public static List<String> splitAndRemoveEmptyElements(String input, String delimiter) {
        String[] inputArray = input.split(delimiter);
        List<String> result = new ArrayList<>(inputArray.length);
        for (int i = 0; i < inputArray.length; i++) {
            String item = inputArray[i];
            if(isNotBlank(item)){
                result.add(item);
            }
        }
        return result;
    }
}
