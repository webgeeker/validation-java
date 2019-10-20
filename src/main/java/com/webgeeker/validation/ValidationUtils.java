package com.webgeeker.validation;

import java.util.ArrayList;

class ValidationUtils {

    static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    static String replace(String text, String search, String replacement) {
        return replace(text, search, replacement, Integer.MAX_VALUE);
    }

    static String replace(String text, String search, String replacement, int max) {
        if (text == null || text.length() == 0 ||
            search == null || search.length() == 0 ||
            replacement == null ||
            max <= 0)
            return text;

        int start = 0;
        int end = indexOf(text, search, start);
        if (end == -1) {
            return text;
        } else {
            int searchLen = search.length();
            int increase = replacement.length() - searchLen;
            increase = increase < 0 ? 0 : increase;
            increase *= max < 0 ? 16 : (max > 64 ? 64 : max);

            StringBuilder buf = new StringBuilder(text.length() + increase);
            for (; end != -1; end = indexOf(text, search, start)) {
                buf.append(text, start, end).append(replacement);
                start = end + searchLen;
                max--;
                if (max == 0)
                    break;
            }

            buf.append(text.substring(start));
            return buf.toString();
        }
    }

    static String[] split(String text, char separator) {
        if (text == null || text.length() == 0)
            return new String[0];
        int i = text.indexOf(separator);
        if (i == -1) // 没有出现 separator
            return new String[]{text};

        ArrayList<String> strings = new ArrayList<String>();

        if (i == 0)
            strings.add("");
        else { // i > 0
            String string = text.substring(0, i);
            strings.add(string);
        }

        int s = i + 1;
        while (s < text.length()){
            i = text.indexOf(separator, s);
            if (i == -1) {
                break;
            }
            String string = text.substring(s, i);
            strings.add(string);

            s = i + 1;
        }
        if (s <= text.length()) {
            strings.add(text.substring(s));
        }

        String[] params = new String[strings.size()];
        return strings.toArray(params);
    }

    static int indexOf(String text, String search) {
        return indexOf(text, search, 0);
    }

    static int indexOf(String text, String search, int startIndex) {
        if (text == null || search == null || startIndex < 0)
            return -1;

        int textLen = text.length();
        int searchLen = search.length();
        if (text.length() == 0 || search.length() == 0 || textLen < searchLen)
            return -1;

        for (int i = startIndex, len = textLen - searchLen + 1; i < len; i++) {
            int j = 0;
            for (; j < searchLen; j++) {
                char c1 = text.charAt(i + j);
                char c2 = search.charAt(j);
                if (c1 != c2)
                    break;
            }
            if (j == searchLen)
                return i;
        }
        return -1;
    }

    // 是否是整数, 不检测数值溢出的情况
    static boolean isIntString(String string) {
        if (string == null || string.length() == 0)
            return false;
        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                if (i != 0 || (c != '+' && c != '-'))
                    return false;
            }
        }
        return true;
    }

    // 是否是非负整数(大于等于0), 不检测数值溢出的情况
    static boolean isNonNegativeInt(String string) {
        if (string == null || string.length() == 0)
            return false;
        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9')
                return false;
        }
        return true;
    }

    static String implode(String[] strings, String separator) {
        if (strings == null || strings.length == 0)
            return "";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            String v = strings[i];
            if (i > 0)
                builder.append(separator);
            builder.append(v);
        }
        return builder.toString();
    }

}
