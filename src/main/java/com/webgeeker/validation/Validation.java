package com.webgeeker.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validation {
    /**
     * 从 request 中提取参数, 生成 Map 返回
     * <p>
     * 因为 request.getParameterMap() 返回的 Map 中的数据结构与客户端发送的结构不一致, 所以需要调用本方法,
     * 重新生成一个 Map, 使其包含的数据结构与客户端发送时的数据结构一致.
     * <p>
     * 即使客户端发送时的参数包含嵌套的数据结构(List, Map), 返回的 Map 中的也会包含同样结构的数据.
     * <p>
     * 原始 ServletRequest 的问题:
     * 1. getParameterMap() 返回的结构与客户端发送时不一致
     * 1. getParameterMap() 既获取GET参数, 也获取POST参数
     * 1. 如果query string为"?count=+10", 调用getParameter()得到的是" 10", '+'号变为了空格' '
     *
     * @param request 请求
     * @return 返回包含请求中的参数的Map. 这些参数既包含 GET 参数, 也包含 POST 参数, 如果同名, POST参数会覆盖掉同名的GET参数
     *         永远不会返回null。
     * @throws ValidationException 如果出现名称重复的参数, 会使用最后出现的那个值, 不会抛异常; 如果参数名称或参数路径不合法, 会抛出异常
     */
    public static HashMap<String, Object> getParameters(HttpServletRequest request) throws ValidationException {

        if (request == null)
            return new HashMap<String, Object>();

        if ("POST".equals(request.getMethod())) {

            String contentType = request.getContentType();
            if (contentType != null && "application/json".equalsIgnoreCase(contentType.substring(0, 16))) {

                @SuppressWarnings("unchecked")
                HashMap<String, Object> map = (HashMap<String, Object>) request.getAttribute("cachedMapOfBodyJson20191202163708");
                if (map != null)
                    return map;

                TypeFactory factory = TypeFactory.defaultInstance();
                MapType type = factory.constructMapType(HashMap.class, String.class, Object.class);

                BufferedReader reader;
                try {
                    reader = request.getReader();
                } catch (IOException e) {
                    throw new ValidationException("读取请求body失败");
                }

                try {
                    map = new ObjectMapper().readValue(reader, type);
                    request.setAttribute("cachedMapOfBodyJson20191202163708", map); // 在当前request中缓存json解析结果
                    reader.close();
                } catch (Exception e) {
                    try {
                        reader.close();
                    } catch (Exception e1) {
                        // do nothing
                    }
                    throw new ValidationException("解析 Json body 失败");
                }
                return map;
            } // end content-type == json
        } // end method == post

        /*
         * 根据Servlet规范，如果同时满足下列条件，则请求体(Entity)中的表单数据，将被填充到request的parameter集合中（request.getParameter系列方法可以读取相关数据）：
         * 1 这是一个HTTP/HTTPS请求
         * 2 请求方法是POST（querystring无论是否POST都将被设置到parameter中）
         * 3 请求的类型（Content-Type头）是application/x-www-form-urlencoded
         * 4 Servlet调用了getParameter系列方法
         */
        Map<String, String[]> originalMap = request.getParameterMap(); //

        if (originalMap == null)
            return new HashMap<String, Object>();

        HashMap<String, Object> map = new HashMap<String, Object>();

        for (String key : originalMap.keySet()) {
            String[] values = originalMap.get(key);
            String value = values[values.length - 1]; // 如果请求中有重复的参数，则values.length会大于1，这里我们只取最后一个值

            int i1 = key.indexOf('[');
            int i2 = key.indexOf(']');

            if (i1 == -1 && i2 == -1) { // 非嵌套的参数
                map.put(key, value);
            } else { // 嵌套参数

                if (i1 == -1) // 没有左方括号[
                    throw new ValidationException("请求中包含非法的参数: " + key);
                else if (i1 == 0) // 左方括号[前没有字符，如: [123]
                    throw new ValidationException("请求中包含非法的参数: " + key);
                if (i2 == -1) // 没有右方括号]
                    throw new ValidationException("请求中包含非法的参数: " + key);
                if (i2 <= i1) // 右方括号]在左方括号[前面，如abc]i[；或者左右方括号之间没有字符，如 abc[]
                    throw new ValidationException("请求中包含非法的参数: " + key);

                String prevKey = key.substring(0, i1);
                int prevIdx = -1;

                int start = i1;
                Object fatherCollection = map;
                boolean isArray = false;
                while (start < key.length()) {
                    i1 = key.indexOf('[', start);
                    i2 = key.indexOf(']', start);
                    if (i1 != start) // 非法的key，形如: abc[i]def[j]
                        throw new ValidationException("请求中包含非法的参数: " + key);
                    if (i1 == -1 || i2 == -1) // 非法的key，形如: abc[i]def   abc[i]def]   abc[i][def
                        throw new ValidationException("请求中包含非法的参数: " + key);

                    int len = i2 - i1 - 1;
                    if (len < 1) // 右方括号]在左方括号[前面，如abc[0]]i[；或者左右方括号之间没有字符，如 abc[][]
                        throw new ValidationException("请求中包含非法的参数: " + key);

                    String subKey = key.substring(i1 + 1, i2);
                    int idx = -1;
                    try {
                        idx = Integer.parseInt(subKey);
                        // subKey 应该是数组的索引。不考虑索引值是否乱序的问题
                        isArray = true;
                    } catch (Exception e) {
                        // subKey 应该是Map的键
                        char firstChar = subKey.charAt(0);
                        if ((firstChar >= 'a' && firstChar <= 'z') ||
                            (firstChar >= 'A' && firstChar <= 'Z') ||
                            firstChar == '_') { // 合法的键
                            isArray = false;
                        } else {
                            throw new ValidationException("请求中包含非法的参数: " + key);
                        }
                    }

                    if (isArray) { // 是list结构
                        if (idx < 0)
                            throw new ValidationException("请求中包含非法索引值的嵌套参数: " + key);
                        ArrayList<Object> list;
                        if (fatherCollection instanceof HashMap) {
                            HashMap<String, Object> fatherFatherMap = (HashMap<String, Object>) fatherCollection;
                            Object collection = fatherFatherMap.get(prevKey);
                            if (collection == null) {
                                list = new ArrayList<Object>();
                                fatherFatherMap.put(prevKey, list);
                            } else if (collection instanceof ArrayList == false)
                                throw new ValidationException("请求中包含的嵌套参数(" + key + ")与其它参数发生了冲突");
                            else
                                list = (ArrayList<Object>) collection;
                        } else {
                            ArrayList<Object> fatherList = (ArrayList<Object>) fatherCollection;
                            Object collection = prevIdx < fatherList.size() ? fatherList.get(prevIdx) : null;
                            if (collection == null) {
                                list = new ArrayList<Object>();
                                fatherList.add(list);
                            } else if (collection instanceof ArrayList == false)
                                throw new ValidationException("请求中包含的嵌套参数(" + key + ")与其它参数发生了冲突");
                            else
                                list = (ArrayList<Object>) collection;
                        }

                        if (idx > list.size())
                            throw new ValidationException("请求中包含数组下标不连续的嵌套参数: " + key);

                        fatherCollection = list;
                        prevIdx = idx;
                    } else { // 是map结构
                        HashMap<String, Object> m;
                        if (fatherCollection instanceof HashMap) {
                            HashMap<String, Object> fatherFatherMap = (HashMap<String, Object>) fatherCollection;
                            Object collection = fatherFatherMap.get(prevKey);
                            if (collection == null) {
                                m = new HashMap<String, Object>();
                                fatherFatherMap.put(prevKey, m);
                            } else if (collection instanceof HashMap == false)
                                throw new ValidationException("请求中包含的嵌套参数(" + key + ")与其它参数发生了冲突");
                            else
                                m = (HashMap<String, Object>) collection;
                        } else {
                            ArrayList<Object> fatherList = (ArrayList<Object>) fatherCollection;
                            Object collection = prevIdx < fatherList.size() ? fatherList.get(prevIdx) : null;
                            if (collection == null) {
                                m = new HashMap<String, Object>();
                                fatherList.add(m);
                            } else if (collection instanceof HashMap == false)
                                throw new ValidationException("请求中包含的嵌套参数(" + key + ")与其它参数发生了冲突");
                            else
                                m = (HashMap<String, Object>) collection;
                        }
                        fatherCollection = m;
                        prevKey = subKey;
                    }

                    start = i2 + 1;
                } // end while(start < key.length())

                if (isArray) {
                    ((ArrayList<Object>) fatherCollection).add(value);
                } else {
                    ((Map<String, Object>) fatherCollection).put(prevKey, value);
                }
            }
        }

        return map;
    }

    public static HashMap<String, Object> validate(HashMap<String, Object> params, String[] validations) throws ValidationException {
        return validate(params, validations, false);
    }

    public static HashMap<String, Object> validate(HashMap<String, Object> params, String[] validations, boolean ignoreRequired) throws ValidationException {

        ArrayList<String> validation = new ArrayList<String>();
        for (int i = 0, len = validations.length; i < len; i++) {
            String str = validations[i];
            if (str != null) {
                validation.add(str);
                if (i < len - 1) // 最后一个null可以省略
                    continue;
            }

            if (validation.size() < 2)
                throw new ValidationException("参数validations格式错误");

            String keyPath = validation.get(0);

            ArrayList<Object> keys = compileKeyPath(keyPath);

            String[] ruleStrings = new String[validation.size() - 1];
            for (int j = 1; j < validation.size(); j++) {
                ruleStrings[j-1] = validation.get(j);
            }
            validation.clear();

            _validate(params, keys, ruleStrings, "", ignoreRequired);
        }
        return null;
    }

    private static HashMap<String, Object> _validate(Object params, List<Object> keys, String[] ruleStrings, String keyPrefix, boolean ignoreRequired) throws ValidationException {

        String keyPath = keyPrefix;
        Object siblings = params;
        Object value = params;

        int keysCount = keys.size();

        int n = 0;
        for (; n < keysCount; n++) {
            siblings = value;
            keyPrefix = keyPath;

            Object key = keys.get(n);
            if (key.equals("*")) {
                List<Object> siblingsList = validateArr(siblings, null, keyPrefix);
                int c = siblingsList.size();
                if (c > 0) {
                    List<Object> subKeys = keys.subList(n + 1, keysCount);
                    int subKeysCount = keysCount - n - 1;
                    for (int i = 0; i < c; i++) {
                        Object element = siblingsList.get(i);
                        keyPath = keyPrefix + "[" + i + "]";
                        if (subKeysCount > 0) {
                            _validate(element, subKeys, ruleStrings, keyPath, ignoreRequired);
                        } else {
                            _validateValue(element, ruleStrings, keyPath, ignoreRequired, params, siblingsList);
                        }
                    }
                    return null;
                } else { // 'items[*]' => 'Required' 要求items至少有1个元素, 但上面的循环不检测items==[]的情况
                    value = null; // 这里是针对$value==[]这种情况的特殊处理
                }
            } else {
                if (key instanceof Integer) {
                    siblings = validateArr(siblings, null, keyPrefix);
                    int idx = (Integer)key;
                    if (idx < 0 || idx >= ((List<Object>) siblings).size()) // idx<0 不可能出现, 因为compileKeyPath()已经过滤掉了这种情况
                        value = null;
                    else
                        value = ((List<Object>) siblings).get(idx);
                } else {
                    validateMap(siblings, null, keyPrefix);
                    value = ((Map<String, Object>) siblings).get(key);
                }
            } // end if key != "*"

            if (keyPrefix.length() == 0)
                keyPath = (String)key;
            else if (key instanceof Integer || key.equals("*"))
                keyPath = keyPrefix + "[" + key + "]";
            else
                keyPath = keyPrefix + "." + key;

            if (value == null) {
                n++;
                break;
            }
        } // end for keys

        // 到这里n表示当前的value是第几层
        if (n == keysCount) {
            _validateValue(value, ruleStrings, keyPath, ignoreRequired, params, siblings);
        }

        return null;
    }

    /**
     * 根据路径从参数数组中取值. 可以用于IfXxx中参数的取值
     *
     * 本函数里的代码与 _validate() 中的相似, 但是不可能合并成一个函数.
     * 因为针对"comments[*]"这样的参数路径, _validate() 方法内部必须枚举数组
     * 的每个元素, 一个个检测; 而本函数根本就不会遇到参数路径中带*号的情况, 因
     * 为本函数只需要返回一个值, 带*号的话就不知道要返回哪个值了.
     *
     * @param params 原始参数
     * @param keys 条件参数的路径中不能有 * 号, 否则就不知道取哪个值了
     * @return null|Object
     * @throws ValidationException 出错抛出异常
     */
    private static Object getParamValueForIf(Object params, List<Object> keys) throws ValidationException {

        int keysCount = keys.size();

        Object value = params;

        String keyPath = "";
        Object siblings = params;
        int n = 0;
        for (; n < keysCount; n++) {

            Object key = keys.get(n);
            if (key instanceof Integer) {
                siblings = validateArr(siblings, null, keyPath);
                int idx = (Integer) key;
                if (idx < 0 || idx >= ((List<Object>) siblings).size())
                    value = null;
                else
                    value = ((List<Object>) siblings).get(idx);
            } else {
                validateMap(siblings, null, keyPath);
                value = ((Map<String, Object>) siblings).get(key);
            }

            if (keyPath.length() == 0)
                keyPath = (String) key;
            else if (key instanceof Integer || key.equals("*"))
                keyPath = keyPath + "[" + key + "]";
            else
                keyPath = keyPath + "." + key;

            if (value == null) {
                n++;
                break;
            }
            siblings = value;
        } // end for keys

        // 到这里n表示当前的value是第几层
//        if (n == keysCount) {
//        }

        return value;
    }

    private static ArrayList<Object> compileKeyPath(String keyPath) throws ValidationException {
        if (keyPath.length() == 0)
            throw new ValidationException("参数validations中包含空的参数名称");

        if (!keyPath.matches("^[a-zA-Z0-9_.\\[\\]*]+$"))
            throw new ValidationException("非法的参数名称“" + keyPath + "”");

        String[] keys = ValidationUtils.split(keyPath, '.'); // 不可能返回空数组. $keys中的数组还没有解析

        ArrayList<Object> filteredKeys = new ArrayList<Object>();
        for (String key : keys) {
            if (key.length() == 0)
                throw new ValidationException("“" + keyPath + "”中包含空的参数名称");

            int i = key.indexOf('[');
            if (i == -1) { // 普通的key
                if (key.indexOf('*') >= 0)
                    throw new ValidationException("“" + keyPath + "”中'*'号只能处于方括号[]中");
                if (key.indexOf(']') >= 0)
                    throw new ValidationException("“" + key + "”中包含了非法的']'号");
                char firstChar = key.charAt(0);
                if (firstChar >= '0' && firstChar <= '9') {
                    if (keys.length == 1)
                        throw new ValidationException("参数名称“" + keyPath + "”不得以数字开头");
                    else
                        throw new ValidationException("“" + keyPath + "”中包含了以数字开头的参数名称“" + key + "”");
                }
                filteredKeys.add(key);
            } else if (i == 0) {
                throw new ValidationException("“" + keyPath + "”中'['号前面没有参数名称");
            } else { // 嵌套的key
                int j = key.indexOf(']');
                if (j == -1)
                    throw new ValidationException("“" + key + "”中的'['号之后缺少']'");
                if (i > j)
                    throw new ValidationException("“" + key + "”中'[', ']'顺序颠倒了");

                // 识别普通数组的变量名（'[*]'之前的部分）
                String varName = key.substring(0, i);
                if (varName.indexOf('*') >= 0)
                    throw new ValidationException("“" + key + "”中包含了非法的'*'号");
                char firstChar = varName.charAt(0);
                if (firstChar >= '0' && firstChar <= '9')
                    throw new ValidationException("“" + keyPath + "”中包含了以数字开头的参数名称“" + varName + "”");
                filteredKeys.add(varName);

                // 识别普通数组的索引值
                String index = key.substring(i + 1, j);
                if (index.equals("*")) {
                    filteredKeys.add(index);
                } else if (ValidationUtils.isNonNegativeInt(index)) {
                    filteredKeys.add(Integer.parseInt(index));
                } else
                    throw new ValidationException("“" + key + "”中的方括号[]之间只能包含'*'号或数字");

                // 尝试识别多维数组
                int len = key.length();
                while (j < len - 1) {
                    j++;
                    i = key.indexOf('[', j);
                    if (i != j)
                        throw new ValidationException("“" + key + "”中的“[" + index + "]”之后包含非法字符");
                    j = key.indexOf(']', i);
                    if (j == -1)
                        throw new ValidationException("“" + key + "”中的'['号之后缺少']'");

                    index = key.substring(i + 1, j);
                    if (index.equals("*")) {
                        filteredKeys.add(index);
                    } else if (ValidationUtils.isNonNegativeInt(index)) {
                        filteredKeys.add(Integer.parseInt(index));
                    } else
                        throw new ValidationException("“" + key + "”中的方括号[]之间只能包含*号或数字");
                }
            }
        }

        return filteredKeys;
    }

    public static void validateValue(Object value, String[] ruleStrings) throws ValidationException {
        _validateValue(value, ruleStrings, "Parameter", false, null, null);
    }

    public static void validateValue(Object value, String[] ruleStrings, String alias, boolean ignoreRequired) throws ValidationException {
        if (alias == null || alias.length() == 0)
            alias = "Parameter";
        _validateValue(value, ruleStrings, alias, ignoreRequired, null, null);
    }

    private static Object _validateValue(Object value, String[] ruleStrings, String alias, boolean ignoreRequired, Object originParams, Object siblings) throws ValidationException {
        if (ruleStrings == null || ruleStrings.length == 0)
            throw new ValidationException("没有提供验证规则");

        String[] validators = ruleStrings;

        /*
         * 一个参数可以有一条或多条validator, 检测是否通过的规则如下:
         * 1. 如果有一条validator检测成功, 则该参数检测通过
         * 2. 如果即没有成功的也没有失败的（全部validator都被忽略或者有0条validator）, 也算参数检测通过
         * 3. 上面两条都不满足, 则参数检测失败
         */
        int success = 0;
        int failed = 0;
        ValidationException lastValidationException = null;
        for (String strValidator : validators) {
            if (strValidator.length() == 0) {
                success++;
                continue;
            }
            Validator validator = compileValidator(strValidator);
            List<Object[]> validatorUnits = validator.units;

            try {
                int countOfIfs = validator.countOfIfs;
                int countOfUnits = validatorUnits.size();

                String aAlias = validator.alias != null ? validator.alias : alias;

                int i = 0;
                for (; i < countOfIfs; i++) {
                    Object[] validatorUnit = validatorUnits.get(i);
                    Integer ifValidatorType = (Integer) validatorUnit[0];
                    String varKeyPath = (String) validatorUnit[1]; // 条件参数的路径

                    Object ifParamValue;
                    if (varKeyPath.startsWith(".")) { // 以.开头, 是相对路径
                        String key = varKeyPath.substring(1); // 去掉开头的.号
                        validateVarName(key, "IfXxx中的条件参数“" + key + "”不是合法的变量名", null);
                        ifParamValue = ((HashMap<String, Object>)siblings).get(key);
                    } else { // 绝对路径
                        if (varKeyPath.contains("*"))
                            throw new ValidationException("IfXxx中的条件参数“" + varKeyPath + "”中不得包含*号");
                        ArrayList<Object> keys = compileKeyPath(varKeyPath);
                        ifParamValue = getParamValueForIf(originParams, keys);
                    }

                    // 处理条件参数不存在的情况
                    if (ignoreRequired) { // 忽略所有 Required 验证器
                        if (value != null) { // 如果参数存在，则其依赖的条件参数也必须存在
                            if (ifParamValue == null && // 依赖的条件参数不存在
                                ifValidatorType != Type.IfExist &&
                                ifValidatorType != Type.IfNotExist)
                                throw new ValidationException("必须提供条件参数“" + varKeyPath + "”，因为“" + aAlias + "”的验证依赖它");
                        } else { // 如果参数不存在，则该参数不检测
                            return value;
                        }
                    } else { // 没有忽略 Required 验证器
                        // 无论参数是否存在，则其依赖的条件参数都必须存在
                        if (ifParamValue == null && // 依赖的条件参数不存在
                            ifValidatorType != Type.IfExist &&
                            ifValidatorType != Type.IfNotExist)
                            throw new ValidationException("必须提供条件参数“" + varKeyPath + "”，因为“" + aAlias + "”的验证依赖它");
                    }

                    boolean ifResult;
                    switch (ifValidatorType) {
                        case Type.If:
                            ifResult = checkIf(ifParamValue);
                            break;
                        case Type.IfNot:
                            ifResult = checkIfNot(ifParamValue);
                            break;
                        case Type.IfTrue:
                            ifResult = checkIfTrue(ifParamValue);
                            break;
                        case Type.IfFalse:
                            ifResult = checkIfFalse(ifParamValue);
                            break;
                        case Type.IfExist:
                            ifResult = checkIfExist(ifParamValue);
                            break;
                        case Type.IfNotExist:
                            ifResult = checkIfNotExist(ifParamValue);
                            break;
                        case Type.IfIntEq:
                            ifResult = checkIfIntEq(ifParamValue, (Integer) validatorUnit[2]);
                            break;
                        case Type.IfIntNe:
                            ifResult = checkIfIntNe(ifParamValue, (Integer) validatorUnit[2]);
                            break;
                        case Type.IfIntGt:
                            ifResult = checkIfIntGt(ifParamValue, (Integer) validatorUnit[2]);
                            break;
                        case Type.IfIntGe:
                            ifResult = checkIfIntGe(ifParamValue, (Integer) validatorUnit[2]);
                            break;
                        case Type.IfIntLt:
                            ifResult = checkIfIntLt(ifParamValue, (Integer) validatorUnit[2]);
                            break;
                        case Type.IfIntLe:
                            ifResult = checkIfIntLe(ifParamValue, (Integer) validatorUnit[2]);
                            break;
                        case Type.IfIntIn:
                            ifResult = checkIfIntIn(ifParamValue, (List<Integer>) validatorUnit[2]);
                            break;
                        case Type.IfIntNotIn:
                            ifResult = checkIfIntNotIn(ifParamValue, (List<Integer>) validatorUnit[2]);
                            break;
                        case Type.IfLongEq:
                            ifResult = checkIfLongEq(ifParamValue, (Long) validatorUnit[2]);
                            break;
                        case Type.IfLongNe:
                            ifResult = checkIfLongNe(ifParamValue, (Long) validatorUnit[2]);
                            break;
                        case Type.IfLongGt:
                            ifResult = checkIfLongGt(ifParamValue, (Long) validatorUnit[2]);
                            break;
                        case Type.IfLongGe:
                            ifResult = checkIfLongGe(ifParamValue, (Long) validatorUnit[2]);
                            break;
                        case Type.IfLongLt:
                            ifResult = checkIfLongLt(ifParamValue, (Long) validatorUnit[2]);
                            break;
                        case Type.IfLongLe:
                            ifResult = checkIfLongLe(ifParamValue, (Long) validatorUnit[2]);
                            break;
                        case Type.IfLongIn:
                            ifResult = checkIfLongIn(ifParamValue, (List<Long>) validatorUnit[2]);
                            break;
                        case Type.IfLongNotIn:
                            ifResult = checkIfLongNotIn(ifParamValue, (List<Long>) validatorUnit[2]);
                            break;
                        case Type.IfStrEq:
                            ifResult = checkIfStrEq(ifParamValue, (String) validatorUnit[2]);
                            break;
                        case Type.IfStrNe:
                            ifResult = checkIfStrNe(ifParamValue, (String) validatorUnit[2]);
                            break;
                        case Type.IfStrGt:
                            ifResult = checkIfStrGt(ifParamValue, (String) validatorUnit[2]);
                            break;
                        case Type.IfStrGe:
                            ifResult = checkIfStrGe(ifParamValue, (String) validatorUnit[2]);
                            break;
                        case Type.IfStrLt:
                            ifResult = checkIfStrLt(ifParamValue, (String) validatorUnit[2]);
                            break;
                        case Type.IfStrLe:
                            ifResult = checkIfStrLe(ifParamValue, (String) validatorUnit[2]);
                            break;
                        case Type.IfStrIn:
                            ifResult = checkIfStrIn(ifParamValue, (List<String>) validatorUnit[2]);
                            break;
                        case Type.IfStrNotIn:
                            ifResult = checkIfStrNotIn(ifParamValue, (List<String>) validatorUnit[2]);
                            break;
                        default:
                            throw new ValidationException("无法识别的条件验证器类型“" + ifValidatorType + "”"); // 不应该出现这种情况
                    }

                    if (!ifResult) // If条件不满足
                        break; // 跳出
                }

                if (i < countOfIfs) // 有If条件不满足, 忽略本条验证规则
                    continue;

                if (value == null) { // 没有提供参数
                    if (!validator.required || ignoreRequired)
                        continue; // 忽略本条验证规则

                    throwIfHasReason(validator.reason);

                    aAlias = finalAlias(aAlias);
                    throwWithErrorTemplate("Required",
                        "{{param}}", aAlias);
                }

                for (i = countOfIfs; i < countOfUnits; i++) {
                    Object[] validatorUnit = validatorUnits.get(i);
                    Integer validatorType = (Integer) validatorUnit[0];

                    switch (validatorType) {
                        case Type.Int:
                            validateInt(value, validator.reason, aAlias);
                            break;
                        case Type.IntEq:
                            validateIntEq(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.IntNe:
                            validateIntNe(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.IntGt:
                            validateIntGt(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.IntGe:
                            validateIntGe(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.IntLt:
                            validateIntLt(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.IntLe:
                            validateIntLe(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.IntGtLt:
                            validateIntGtLt(value, (Integer) validatorUnit[1], (Integer) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.IntGeLe:
                            validateIntGeLe(value, (Integer) validatorUnit[1], (Integer) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.IntGtLe:
                            validateIntGtLe(value, (Integer) validatorUnit[1], (Integer) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.IntGeLt:
                            validateIntGeLt(value, (Integer) validatorUnit[1], (Integer) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.IntIn:
                            validateIntIn(value, (Integer[]) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.IntNotIn:
                            validateIntNotIn(value, (Integer[]) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.Long:
                            validateLong(value, validator.reason, aAlias);
                            break;
                        case Type.LongEq:
                            validateLongEq(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.LongNe:
                            validateLongNe(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.LongGt:
                            validateLongGt(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.LongGe:
                            validateLongGe(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.LongLt:
                            validateLongLt(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.LongLe:
                            validateLongLe(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.LongGtLt:
                            validateLongGtLt(value, (Long) validatorUnit[1], (Long) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.LongGeLe:
                            validateLongGeLe(value, (Long) validatorUnit[1], (Long) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.LongGtLe:
                            validateLongGtLe(value, (Long) validatorUnit[1], (Long) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.LongGeLt:
                            validateLongGeLt(value, (Long) validatorUnit[1], (Long) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.LongIn:
                            validateLongIn(value, (Long[]) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.LongNotIn:
                            validateLongNotIn(value, (Long[]) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.Str:
                            validateStr(value, validator.reason, aAlias);
                            break;
                        case Type.StrEq:
                            validateStrEq(value, (String) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrEqI:
                            validateStrEqI(value, (String) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrNe:
                            validateStrNe(value, (String) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrNeI:
                            validateStrNeI(value, (String) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrIn:
                            validateStrIn(value, (String[]) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrInI:
                            validateStrInI(value, (String[]) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrNotIn:
                            validateStrNotIn(value, (String[]) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrNotInI:
                            validateStrNotInI(value, (String[]) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrLen:
                            validateStrLen(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrLenGe:
                            validateStrLenGe(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrLenLe:
                            validateStrLenLe(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.StrLenGeLe:
                            validateStrLenGeLe(value, (Integer) validatorUnit[1], (Integer) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.Bool:
                            validateBool(value, validator.reason, aAlias);
                            break;
                        case Type.BoolTrue:
                            validateBoolTrue(value, validator.reason, aAlias);
                            break;
                        case Type.BoolFalse:
                            validateBoolFalse(value, validator.reason, aAlias);
                            break;
                        case Type.BoolSmart:
                            validateBoolSmart(value, validator.reason, aAlias);
                            break;
                        case Type.BoolSmartTrue:
                            validateBoolSmartTrue(value, validator.reason, aAlias);
                            break;
                        case Type.BoolSmartFalse:
                            validateBoolSmartFalse(value, validator.reason, aAlias);
                            break;
                        case Type.Letters:
                            validateLetters(value, validator.reason, aAlias);
                            break;
                        case Type.Alphabet:
                            validateAlphabet(value, validator.reason, aAlias);
                            break;
                        case Type.Numbers:
                            validateNumbers(value, validator.reason, aAlias);
                            break;
                        case Type.Digits:
                            validateDigits(value, validator.reason, aAlias);
                            break;
                        case Type.LettersNumbers:
                            validateLettersNumbers(value, validator.reason, aAlias);
                            break;
                        case Type.Numeric:
                            validateNumeric(value, validator.reason, aAlias);
                            break;
                        case Type.VarName:
                            validateVarName(value, validator.reason, aAlias);
                            break;
                        case Type.Email:
                            validateEmail(value, validator.reason, aAlias);
                            break;
                        case Type.Url:
                            validateUrl(value, validator.reason, aAlias);
                            break;
                        case Type.HttpUrl:
                            validateHttpUrl(value, validator.reason, aAlias);
                            break;
                        case Type.Ip:
                            validateIp(value, validator.reason, aAlias);
                            break;
                        case Type.Ipv4:
                            validateIpv4(value, validator.reason, aAlias);
                            break;
                        case Type.Ipv6:
                            validateIpv6(value, validator.reason, aAlias);
                            break;
                        case Type.Mac:
                            validateMac(value, validator.reason, aAlias);
                            break;
                        case Type.Regexp:
                            validateRegexp(value, (Pattern) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.Float:
                            validateFloat(value, validator.reason, aAlias);
                            break;
                        case Type.FloatGt:
                            validateFloatGt(value, (Double) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.FloatGe:
                            validateFloatGe(value, (Double) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.FloatLt:
                            validateFloatLt(value, (Double) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.FloatLe:
                            validateFloatLe(value, (Double) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.FloatGtLt:
                            validateFloatGtLt(value, (Double) validatorUnit[1], (Double) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.FloatGeLe:
                            validateFloatGeLe(value, (Double) validatorUnit[1], (Double) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.FloatGtLe:
                            validateFloatGtLe(value, (Double) validatorUnit[1], (Double) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.FloatGeLt:
                            validateFloatGeLt(value, (Double) validatorUnit[1], (Double) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.List:
                        case Type.Arr:
                            validateArr(value, validator.reason, aAlias);
                            break;
                        case Type.ListLen:
                        case Type.ArrLen:
                            validateArrLen(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.ListLenGe:
                        case Type.ArrLenGe:
                            validateArrLenGe(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.ListLenLe:
                        case Type.ArrLenLe:
                            validateArrLenLe(value, (Integer) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.ListLenGeLe:
                        case Type.ArrLenGeLe:
                            validateArrLenGeLe(value, (Integer) validatorUnit[1], (Integer) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.Map:
                            validateMap(value, validator.reason, aAlias);
                            break;
                        case Type.Date:
                            validateDate(value, validator.reason, aAlias);
                            break;
                        case Type.DateFrom:
                            validateDateFrom(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.DateTo:
                            validateDateTo(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.DateFromTo:
                            validateDateFromTo(value, (Long) validatorUnit[1], (Long) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.DateTime:
                            validateDateTime(value, validator.reason, aAlias);
                            break;
                        case Type.DateTimeFrom:
                            validateDateTimeFrom(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.DateTimeTo:
                            validateDateTimeTo(value, (Long) validatorUnit[1], validator.reason, aAlias);
                            break;
                        case Type.DateTimeFromTo:
                            validateDateTimeFromTo(value, (Long) validatorUnit[1], (Long) validatorUnit[2], validator.reason, aAlias);
                            break;
                        case Type.ByteLen:
                        case Type.ByteLenGe:
                        case Type.ByteLenLe:
                        case Type.ByteLenGeLe:
                        default:
                            throw new ValidationException("无法识别的验证器类型“" + validatorType + "”"); // 不应该出现这种情况
                    }

                }

                success++;
                break; // 多个validator只需要一条验证成功即可
            } catch (ValidationException e) {
                lastValidationException = e;
                failed++;
            }
        }

        if (success > 0 || failed == 0)
            return value;

        throw lastValidationException; // 此时 success == 0 && failed > 0
    }

    /**
     * 将字符串验证器编译为Validator对象
     * <p>
     * 示例1:
     * 输入: $validator = 'StrLen:6,16|regex:/^[a-zA-Z0-9]+$/'
     * 输出: [
     * 'countOfIfs' => 0,
     * 'required' => false,
     * 'reason' => null,
     * 'alias' => $alias,
     * 'units' => [
     * ['StrLen', 6, 16],
     * ['regex', '/^[a-zA-Z0-9]+$/'],
     * ],
     * ]
     * <p>
     * 示例2（自定义验证失败的提示）:
     * 输入: validator = 'StrLen:6,16|regex:/^[a-zA-Z0-9]+$/|>>>:参数验证失败了'
     * 输出: [
     * 'countOfIfs' => 0,
     * 'required' => false,
     * 'reason' => $reason,
     * 'alias' => $alias,
     * 'units' => [
     * ['StrLen', 6, 16],
     * ['regex', '/^[a-zA-Z0-9]+$/'],
     * ],
     * ]
     *
     * @param strValidator string 一条验证字符串
     * @return 返回Validator
     */
    private static Validator compileValidator(String strValidator) throws ValidationException {

        Validator validator = cachedRules.get(strValidator);
        if (validator == null)
            validator = new Validator();

//        if (strValidator.length() == 0); // 外部函数已经检测过了, 不可能出现strValidator为空串

        int countOfIfs = 0;
        boolean required = false;
        String customReason = null;
        String alias = null;
        List<Object[]> units = new ArrayList<Object[]>();

        String[] segments = strValidator.split("\\|");
        int segCount = segments.length;
        for (int i = 0; i < segCount; ) {
            String segment = segments[i];
            i++;

            if (segment.startsWith("Regexp:")) { // 是正则表达式
                if (segment.indexOf('/', 7) != 7) // 非法的正则表达. 合法的必须首尾加/
                    throw new ValidationException("正则表达式验证器Regexp格式错误. 正确的格式是 Regexp:/xxxx/");

                int pos = 8;
                int len = segment.length();

                boolean finish = false;
                do {
                    int pos2 = segment.lastIndexOf('/'); //反向查找字符/
                    if (pos2 != len - 1 || // 不是以/结尾, 说明正则表达式中包含了|分隔符, 正则表达式被explode拆成了多段
                        pos2 == 7) { // 第1个/后面就没字符了, 说明正则表达式中包含了|分隔符, 正则表达式被explode拆成了多段
                        //
                    } else { // 以/结尾, 可能是完整的正则表达式, 也可能是不完整的正则表达式
                        do {
                            pos = segment.indexOf('\\', pos); // 从前往后扫描转义符\
                            if (pos == -1) { // 结尾的/前面没有转义符\, 正则表达式扫描完毕
                                finish = true;
                                break;
                            } else if (pos == len - 1) { // 不可能, $len-1这个位置是字符/
                                //
                            } else if (pos == len - 2) { // 结尾的/前面有转义符\, 说明/只是正则表达式内容的一部分, 正则表达式尚未结束
                                pos += 3; // 跳过“\/|”三个字符
                                break;
                            } else {
                                pos += 2;
                            }
                        } while (true);

                        if (finish)
                            break;
                    }

                    if (i >= segCount)
                        throw new ValidationException("正则表达式验证器Regexp格式错误. 正确的格式是 Regexp:/xxxx/");

                    segment = segment + '|';
                    segment = segment + segments[i]; // 拼接后面一个segment
                    len = segment.length();
                    i++;
                } while (true);
                String regexp = segment.substring(8, segment.length() - 1);
                Pattern p = Pattern.compile(regexp);
                units.add(new Object[]{Type.Regexp, p}); // 正则表达式要去掉首尾的斜杠/
            } // end if (segment.startsWith("Regexp:")) { // 是正则表达式
            else { // 非正则表达式验证器
                int pos = segment.indexOf(':');
                if (pos == -1) { // 不带参数的验证器
                    if (segment.equals("Required")) {
                        if (units.size() > countOfIfs)
                            throw new ValidationException("Required只能出现在验证规则的开头（IfXxx后面）");
                        required = true;
                    } else {
                        int validatorType = Type.fromNameOrThrow(segment);
                        units.add(new Object[]{validatorType});
                    }
                } else { // 有冒号:, 是带参数的验证器
                    if (pos == 0)
                        throw new ValidationException("“" + segment + "”中的':'号前面没有验证器");
                    String validatorName = segment.substring(0, pos);
                    int validatorType = Type.fromNameOrThrow(validatorName);
                    String p;
                    if (pos + 1 == segment.length())
                        p = "";
                    else
                        p = segment.substring(pos + 1);
                    Object[] validatorUnit;
                    switch (validatorType) {
                        case Type.IntEq:
                        case Type.IntNe:
                        case Type.IntGt:
                        case Type.IntGe:
                        case Type.IntLt:
                        case Type.IntLe: {
                            int v = parseParamIntOrThrow(p, validatorName);
                            validatorUnit = new Object[]{validatorType, v};
                            break;
                        }
                        case Type.LongEq:
                        case Type.LongNe:
                        case Type.LongGt:
                        case Type.LongGe:
                        case Type.LongLt:
                        case Type.LongLe: {
                            long v = parseParamLongOrThrow(p, validatorName);
                            validatorUnit = new Object[]{validatorType, v};
                            break;
                        }
                        case Type.StrLen:
                        case Type.StrLenGe:
                        case Type.StrLenLe:
                        case Type.ByteLen:
                        case Type.ByteLenGe:
                        case Type.ByteLenLe:
                        case Type.ListLen:
                        case Type.ListLenGe:
                        case Type.ListLenLe:
                        case Type.ArrLen:
                        case Type.ArrLenGe:
                        case Type.ArrLenLe:{
                            int v = parseParamIntNonNegativeOrThrow(p, validatorName);
                            validatorUnit = new Object[]{validatorType, v};
                            break;
                        }
                        case Type.IntGtLt:
                        case Type.IntGeLe:
                        case Type.IntGtLe:
                        case Type.IntGeLt:{
                            String[] vals = p.split(",");
                            if (vals.length != 2)
                                throwFormatError(validatorName);
                            int v1 = parseParamIntOrThrow(vals[0], validatorName);
                            int v2 = parseParamIntOrThrow(vals[1], validatorName);
                            validatorUnit = new Object[]{validatorType, v1, v2};
                            break;
                        }
                        case Type.LongGtLt:
                        case Type.LongGeLe:
                        case Type.LongGtLe:
                        case Type.LongGeLt:{
                            String[] vals = p.split(",");
                            if (vals.length != 2)
                                throwFormatError(validatorName);
                            long v1 = parseParamLongOrThrow(vals[0], validatorName);
                            long v2 = parseParamLongOrThrow(vals[1], validatorName);
                            validatorUnit = new Object[]{validatorType, v1, v2};
                            break;
                        }
                        case Type.StrLenGeLe:
                        case Type.ByteLenGeLe:
                        case Type.ListLenGeLe:
                        case Type.ArrLenGeLe: {
                            String[] vals = p.split(",");
                            if (vals.length != 2)
                                throwFormatError(validatorName);
                            int v1 = parseParamIntNonNegativeOrThrow(vals[0], validatorName);
                            int v2 = parseParamIntNonNegativeOrThrow(vals[1], validatorName);
                            validatorUnit = new Object[]{validatorType, v1, v2};
                            break;
                        }
                        case Type.IntIn:
                        case Type.IntNotIn: {
                            Integer[] ints = parseParamIntArray(p);
                            if (ints == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, ints};
                            break;
                        }
                        case Type.LongIn:
                        case Type.LongNotIn: {
                            Long[] longs = parseParamLongArray(p);
                            if (longs == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, longs};
                            break;
                        }
                        case Type.StrEq:
                        case Type.StrNe:
                        case Type.StrEqI:
                        case Type.StrNeI: {
                            validatorUnit = new Object[]{validatorType, p};
                            break;
                        }
                        case Type.StrIn:
                        case Type.StrNotIn:
                        case Type.StrInI:
                        case Type.StrNotInI: {
                            String[] strings = parseParamStrArray(p);
                            validatorUnit = new Object[]{validatorType, strings};
                            break;
                        }
                        case Type.IfIntEq:
                        case Type.IfIntNe:
                        case Type.IfIntGt:
                        case Type.IfIntLt:
                        case Type.IfIntGe:
                        case Type.IfIntLe: {
                            if (units.size() > countOfIfs)
                                throw new ValidationException("条件验证器 IfXxx 只能出现在验证规则的开头");
                            Object[] params = parseIfXxxWith1Param1Int(p, validatorName);
                            if (params == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, params[0], params[1]};
                            countOfIfs++;
                            break;
                        }
                        case Type.IfIntIn:
                        case Type.IfIntNotIn: {
                            if (units.size() > countOfIfs)
                                throw new ValidationException("条件验证器 IfXxx 只能出现在验证规则的开头");
                            Object[] params = parseIfXxxWith1ParamNInts(p, validatorName);
                            if (params == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, params[0], params[1]};
                            countOfIfs++;
                            break;
                        }
                        case Type.IfLongEq:
                        case Type.IfLongNe:
                        case Type.IfLongGt:
                        case Type.IfLongLt:
                        case Type.IfLongGe:
                        case Type.IfLongLe: {
                            if (units.size() > countOfIfs)
                                throw new ValidationException("条件验证器 IfXxx 只能出现在验证规则的开头");
                            Object[] params = parseIfXxxWith1Param1Long(p, validatorName);
                            if (params == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, params[0], params[1]};
                            countOfIfs++;
                            break;
                        }
                        case Type.IfLongIn:
                        case Type.IfLongNotIn: {
                            if (units.size() > countOfIfs)
                                throw new ValidationException("条件验证器 IfXxx 只能出现在验证规则的开头");
                            Object[] params = parseIfXxxWith1ParamNLongs(p, validatorName);
                            if (params == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, params[0], params[1]};
                            countOfIfs++;
                            break;
                        }
                        case Type.IfStrEq:
                        case Type.IfStrNe:
                        case Type.IfStrGt:
                        case Type.IfStrLt:
                        case Type.IfStrGe:
                        case Type.IfStrLe: {
                            if (units.size() > countOfIfs)
                                throw new ValidationException("条件验证器 IfXxx 只能出现在验证规则的开头");
                            Object[] params = parseIfXxxWith1Param1Str(p, validatorName);
                            if (params == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, params[0], params[1]};
                            countOfIfs++;
                            break;
                        }
                        case Type.IfStrIn:
                        case Type.IfStrNotIn: {
                            if (units.size() > countOfIfs)
                                throw new ValidationException("条件验证器 IfXxx 只能出现在验证规则的开头");
                            Object[] params = parseIfXxxWith1ParamNStrs(p, validatorName);
                            if (params == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, params[0], params[1]};
                            countOfIfs++;
                            break;
                        }
                        case Type.If:
                        case Type.IfNot:
                        case Type.IfExist:
                        case Type.IfNotExist:
                        case Type.IfTrue:
                        case Type.IfFalse: {
                            if (units.size() > countOfIfs)
                                throw new ValidationException("条件验证器 IfXxx 只能出现在验证规则的开头");
                            Object[] params = ValidationUtils.split(p, ',');
                            if (params.length != 1)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, params[0]};
                            countOfIfs++;
                            break;
                        }
                        case Type.FloatGt:
                        case Type.FloatGe:
                        case Type.FloatLt:
                        case Type.FloatLe: {
                            Double v = parseParamDouble(p);
                            if (v == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, v};
                            break;
                        }
                        case Type.FloatGtLt:
                        case Type.FloatGeLe:
                        case Type.FloatGtLe:
                        case Type.FloatGeLt: {
                            String[] strs = ValidationUtils.split(p, ',');
                            if (strs.length != 2)
                                throwFormatError(validatorName);
                            Double d1 = parseParamDouble(strs[0]);
                            Double d2 = parseParamDouble(strs[1]);
                            if (d1 == null || d2 == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, d1, d2};
                            break;
                        }
                        case Type.DateFrom:
                        case Type.DateTo: {
                            Long timestamp = parseParamDateToTimestamp(p);
                            if (timestamp == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, timestamp};
                            break;
                        }
                        case Type.DateFromTo: {
                            String[] strs = ValidationUtils.split(p, ',');
                            if (strs.length != 2)
                                throwFormatError(validatorName);
                            Long timestamp1 = parseParamDateToTimestamp(strs[0]);
                            Long timestamp2 = parseParamDateToTimestamp(strs[1]);
                            if (timestamp1 == null || timestamp2 == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, timestamp1, timestamp2};
                            break;
                        }
                        case Type.DateTimeFrom:
                        case Type.DateTimeTo: {
                            Long timestamp = parseParamDateTimeToTimestamp(p);
                            if (timestamp == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, timestamp};
                            break;
                        }
                        case Type.DateTimeFromTo: {
                            String[] strs = ValidationUtils.split(p, ',');
                            if (strs.length != 2)
                                throwFormatError(validatorName);
                            Long timestamp1 = parseParamDateTimeToTimestamp(strs[0]);
                            Long timestamp2 = parseParamDateTimeToTimestamp(strs[1]);
                            if (timestamp1 == null || timestamp2 == null)
                                throwFormatError(validatorName);
                            validatorUnit = new Object[]{validatorType, timestamp1, timestamp2};
                            break;
                        }
                        case Type.FileMimes: {
                            throw new ValidationException("暂不支付验证器 " + validatorName);
                        }
                        case Type.FileMaxSize:
                        case Type.FileMinSize: {
                            throw new ValidationException("暂不支付验证器 " + validatorName);
                        }
                        case Type.Hint: {
                            customReason = p;
                            // >>>:之后的所有字符都属于错误提示字符串, 即使其中包含'|'
                            for (; i < segCount; i++) {
                                customReason = customReason + '|' + segments[i];
                            }
                            validatorUnit = null;
                            break;
                        }
                        case Type.Alias: {
                            if (p.length() == 0)
                                throw new ValidationException("\"Alias:\"后面必须有字符串");
                            alias = p;
                            validatorUnit = null;
                            break;
                        }
                        default:
                            throw new ValidationException("无法识别的验证器“" + segment + "”");
                    }
                    if (validatorUnit != null) // 如果是Alias或>>>验证器, validatorUnit==null
                        units.add(validatorUnit);
                } // end if 有冒号:, 是带参数的验证器
            } // end else 不是Regexp
        } // end for segments

        validator.countOfIfs = countOfIfs;
        validator.required = required;
        validator.reason = customReason;
        validator.alias = alias;
        validator.units = units;

        cachedRules.put(strValidator, validator);
        return validator;
    }

    private static ConcurrentHashMap<String, Validator> cachedRules = new ConcurrentHashMap<String, Validator>(1024);

    // region 错误提示

    private static void throwWithErrorTemplate(String validatorName, Object... replaces) throws ValidationException {
        String template = getErrorTemplate(validatorName);
        for (int i = 0; i < replaces.length; i += 2) {
            template = ValidationUtils.replace(template, (String) replaces[i], (String) replaces[i + 1]);
        }
        throw new ValidationException(template);
    }

    private static String getErrorTemplate(String validatorName) {

        // 从"错误提示信息模版"翻译表中查找
        if (langCodeToErrorTemplates != null) {
            Map<String, String> templates = langCodeToErrorTemplates.get(langCode);
            if (templates != null) {
                String template = templates.get(validatorName);
                if (template != null && template.length() > 0)
                    return template;
            }
        }

        // 从默认的"错误提示信息模版"中获取
        return errorTemplates.get(validatorName);
    }

    private static HashMap<String, String> errorTemplates = new HashMap<String, String>(256) {{
        // 整型（不提供length检测,因为负数的符号位会让人混乱, 可以用大于小于比较来做到这一点）
        put("Int", "“{{param}}”必须是整数");
        put("IntEq", "“{{param}}”必须等于 {{value}}");
        put("IntNe", "“{{param}}”不能等于 {{value}}");
        put("IntGt", "“{{param}}”必须大于 {{min}}");
        put("IntGe", "“{{param}}”必须大于等于 {{min}}");
        put("IntLt", "“{{param}}”必须小于 {{max}}");
        put("IntLe", "“{{param}}”必须小于等于 {{max}}");
        put("IntGtLt", "“{{param}}”必须大于 {{min}} 小于 {{max}}");
        put("IntGeLe", "“{{param}}”必须大于等于 {{min}} 小于等于 {{max}}");
        put("IntGtLe", "“{{param}}”必须大于 {{min}} 小于等于 {{max}}");
        put("IntGeLt", "“{{param}}”必须大于等于 {{min}} 小于 {{max}}");
        put("IntIn", "“{{param}}”只能取这些值: {{valueList}}");
        put("IntNotIn", "“{{param}}”不能取这些值: {{valueList}}");

        // 长整型（不提供length检测,因为负数的符号位会让人混乱, 可以用大于小于比较来做到这一点）
        put("Long", "“{{param}}”必须是长整数");
        put("LongEq", "“{{param}}”必须等于 {{value}}");
        put("LongNe", "“{{param}}”不能等于 {{value}}");
        put("LongGt", "“{{param}}”必须大于 {{min}}");
        put("LongGe", "“{{param}}”必须大于等于 {{min}}");
        put("LongLt", "“{{param}}”必须小于 {{max}}");
        put("LongLe", "“{{param}}”必须小于等于 {{max}}");
        put("LongGtLt", "“{{param}}”必须大于 {{min}} 小于 {{max}}");
        put("LongGeLe", "“{{param}}”必须大于等于 {{min}} 小于等于 {{max}}");
        put("LongGtLe", "“{{param}}”必须大于 {{min}} 小于等于 {{max}}");
        put("LongGeLt", "“{{param}}”必须大于等于 {{min}} 小于 {{max}}");
        put("LongIn", "“{{param}}”只能取这些值: {{valueList}}");
        put("LongNotIn", "“{{param}}”不能取这些值: {{valueList}}");

        // 浮点型（内部一律使用double来处理）
        put("Float", "“{{param}}”必须是浮点数");
        put("FloatGt", "“{{param}}”必须大于 {{min}}");
        put("FloatGe", "“{{param}}”必须大于等于 {{min}}");
        put("FloatLt", "“{{param}}”必须小于 {{max}}");
        put("FloatLe", "“{{param}}”必须小于等于 {{max}}");
        put("FloatGtLt", "“{{param}}”必须大于 {{min}} 小于 {{max}}");
        put("FloatGeLe", "“{{param}}”必须大于等于 {{min}} 小于等于 {{max}}");
        put("FloatGtLe", "“{{param}}”必须大于 {{min}} 小于等于 {{max}}");
        put("FloatGeLt", "“{{param}}”必须大于等于 {{min}} 小于 {{max}}");

        // bool型
        put("Bool", "“{{param}}”必须是bool型(true or false)"); // 忽略大小写
        put("BoolSmart", "“{{param}}”只能取这些值: true, false, 1, 0, yes, no, y, n（忽略大小写）");
        put("BoolTrue", "“{{param}}”必须为true");
        put("BoolFalse", "“{{param}}”必须为false");
        put("BoolSmartTrue", "“{{param}}”只能取这些值: true, 1, yes, y（忽略大小写）");
        put("BoolSmartFalse", "“{{param}}”只能取这些值: false, 0, no, n（忽略大小写）");

        // 字符串
        put("Str", "“{{param}}”必须是字符串");
        put("StrEq", "“{{param}}”必须等于\"{{value}}\"");
        put("StrEqI", "“{{param}}”必须等于\"{{value}}\"（忽略大小写）");
        put("StrNe", "“{{param}}”不能等于\"{{value}}\"");
        put("StrNeI", "“{{param}}”不能等于\"{{value}}\"（忽略大小写）");
        put("StrIn", "“{{param}}”只能取这些值: {{valueList}}");
        put("StrInI", "“{{param}}”只能取这些值: {{valueList}}（忽略大小写）");
        put("StrNotIn", "“{{param}}”不能取这些值: {{valueList}}");
        put("StrNotInI", "“{{param}}”不能取这些值: {{valueList}}（忽略大小写）");
        // todo StrSame:var 检测某个参数是否等于另一个参数, 比如password2要等于password
        put("StrLen", "“{{param}}”长度必须等于 {{length}}"); // 字符串长度
        put("StrLenGe", "“{{param}}”长度必须大于等于 {{min}}");
        put("StrLenLe", "“{{param}}”长度必须小于等于 {{max}}");
        put("StrLenGeLe", "“{{param}}”长度必须在 {{min}} - {{max}} 之间"); // 字符串长度
        put("ByteLen", "“{{param}}”长度（字节）必须等于 {{length}}"); // 字符串长度
        put("ByteLenGe", "“{{param}}”长度（字节）必须大于等于 {{min}}");
        put("ByteLenLe", "“{{param}}”长度（字节）必须小于等于 {{max}}");
        put("ByteLenGeLe", "“{{param}}”长度（字节）必须在 {{min}} - {{max}} 之间"); // 字符串长度
        put("Letters", "“{{param}}”只能包含字母");
        put("Alphabet", "“{{param}}”只能包含字母"); // 同Letters
        put("Numbers", "“{{param}}”只能是纯数字");
        put("Digits", "“{{param}}”只能是纯数字"); // 同Numbers
        put("LettersNumbers", "“{{param}}”只能包含字母和数字");
        put("Numeric", "“{{param}}”必须是数值"); // 一般用于大数处理（超过double表示范围的数,一般会用字符串来表示）, 如果是正常范围内的数, 可以使用'Int'或'Float'来检测
        put("VarName", "“{{param}}”只能包含字母、数字和下划线，并且以字母或下划线开头");
        put("Email", "“{{param}}”不是合法的email");
        put("Url", "“{{param}}”不是合法的Url地址");
        put("HttpUrl", "“{{param}}”不是合法的Http地址");
        put("Ip", "“{{param}}”不是合法的IP地址");
        put("Ipv4", "“{{param}}”不是合法的IPv4地址");
        put("Ipv6", "“{{param}}”不是合法的IPv6地址");
        put("Mac", "“{{param}}”不是合法的MAC地址");
        put("Regexp", "“{{param}}”不匹配正则表达式“{{regexp}}”"); // Perl正则表达式匹配. 目前不支持modifiers. http://www.rexegg.com/regex-modifiers.html

        // 数组
        put("List", "“{{param}}”必须是数组或List");
        put("ListLen", "“{{param}}”长度必须等于 {{length}}");
        put("ListLenGe", "“{{param}}”长度必须大于等于 {{min}}");
        put("ListLenLe", "“{{param}}”长度必须小于等于 {{max}}");
        put("ListLenGeLe", "“{{param}}”长度必须在 {{min}} ~ {{max}} 之间");
        put("Arr", "“{{param}}”必须是数组或List");
        put("ArrLen", "“{{param}}”长度必须等于 {{length}}");
        put("ArrLenGe", "“{{param}}”长度必须大于等于 {{min}}");
        put("ArrLenLe", "“{{param}}”长度必须小于等于 {{max}}");
        put("ArrLenGeLe", "“{{param}}”长度必须在 {{min}} ~ {{max}} 之间");

        // 对象
        put("Map", "“{{param}}”必须是 Map<String, Object>");

        // 文件
        put("File", "“{{param}}”必须是文件");
        put("FileMaxSize", "“{{param}}”必须是文件, 且文件大小不超过{{size}}");
        put("FileMinSize", "“{{param}}”必须是文件, 且文件大小不小于{{size}}");
        put("FileImage", "“{{param}}”必须是图片");
        put("FileVideo", "“{{param}}”必须是视频文件");
        put("FileAudio", "“{{param}}”必须是音频文件");
        put("FileMimes", "“{{param}}”必须是这些MIME类型的文件:{{mimes}}");

        // Date & Time
        put("Date", "“{{param}}”必须符合日期格式YYYY-MM-DD");
        put("DateFrom", "“{{param}}”不得早于 {{from}}");
        put("DateTo", "“{{param}}”不得晚于 {{to}}");
        put("DateFromTo", "“{{param}}”必须在 {{from}} ~ {{to}} 之间");
        put("DateTime", "“{{param}}”必须符合日期时间格式YYYY-MM-DD HH:mm:ss");
        put("DateTimeFrom", "“{{param}}”不得早于 {{from}}");
        put("DateTimeTo", "“{{param}}”必须早于 {{to}}");
        put("DateTimeFromTo", "“{{param}}”必须在 {{from}} ~ {{to}} 之间");
//        put("Time", "“{{param}}”必须符合时间格式HH:mm:ss或HH:mm");
//        put("TimeZone", "TimeZone:timezone_identifiers_list()");

        // 其它
        put("Required", "必须提供“{{param}}”");

//        // 预处理（只处理字符串类型, 如果是其它类型, 则原值返回）
//        put("Trim", ""); // 对要检测的值先作一个trim操作, 后续的检测是针对trim后的值进行检测
//        put("Lowercase", ""); // 将要检测的值转为小写, 后续的检测是针对转换后的值进行检测
//        put("Uppercase", ""); // 将要检测的值转为大写, 后续的检测是针对转换后的值进行检测
//        put("ToInt", ""); // 预处理为int型
//        put("ToString", ""); // 预处理为string型（这个一般用不到）
    }};

    // endregion

    // region 国际化

    private static String translateText(String text)
    {
        HashMap<String, String> translations = langCodeToTranslations.get(langCode);
        if (translations != null) {
            String newText = translations.get(text);
            if (newText != null && newText.length() > 0)
                return newText;
        }
        return text;
    }

    /**
     * 设置当前语言代码。默认lang code是空串""（无效的）
     *
     * @param langCode 语言代码
     */
    public static void setLangCode(String langCode) {
        if (langCode != null)
            Validation.langCode = langCode;
    }

    protected static String langCode = "";

    /**
     * “错误提示信息模版”翻译对照表。
     * 完整的“错误提示信息模版”可在成员变量 errorTemplates 中找到
     */
    protected static Map<String, Map<String, String>> langCodeToErrorTemplates = null;
//    new HashMap<>() {{
//        put("zh-tw", new HashMap<>() {{
//            put("Int", "“{{param}}”必須是整數");
//            put("IntGt", "“{{param}}”必須大於 {{min}}");
//            put("Str", "“{{param}}”必須是字符串");
//        }});
//        put("en-us", new HashMap<>() {{
//            put("Int", "{{param}} must be an integer");
//            put("IntGt", "{{param}} must be greater than {{min}}");
//            put("Str", "{{param}} must be a string");
//        }});
//    }};

    private static HashMap<String, HashMap<String, String>> langCodeToTranslations = new HashMap<String, HashMap<String, String>>() {{
        put("en-us", new HashMap<String, String>(64) {{
//            put("错误信息1", "error message 1");
//            put("姓名", "name");
        }});
    }};

    // endregion

    // region 参数解析 parsers

    // 解析Int型验证器的参数值. 如果参数值不合法, 抛出异常
    private static int parseParamIntOrThrow(String string, String validatorName) throws ValidationException {
        if (string == null || string.length() == 0)
            throw new ValidationException("验证器 " + validatorName + " 的参数必须是一个整数");
        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                if (i != 0 || (c != '+' && c != '-'))
                    throw new ValidationException("验证器 " + validatorName + " 的参数必须是一个整数");
            }
        }
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            throw new ValidationException("验证器 " + validatorName + " 的参数的取值超过整数的取值范围");
        }
    }

    // 解析Long型验证器的参数值. 如果参数值不合法, 抛出异常
    private static long parseParamLongOrThrow(String string, String validatorName) throws ValidationException {
        if (string == null || string.length() == 0)
            throw new ValidationException("验证器 " + validatorName + " 的参数必须是一个长整数");
        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                if (i != 0 || (c != '+' && c != '-'))
                    throw new ValidationException("验证器 " + validatorName + " 的参数必须是一个长整数");
            }
        }
        try {
            return Long.parseLong(string);
        } catch (Exception e) {
            throw new ValidationException("验证器 " + validatorName + " 的参数的取值超过长整数的取值范围");
        }
    }

    // 解析(非负)Int型验证器的参数值. 如果参数值不合法, 抛出异常
    private static int parseParamIntNonNegativeOrThrow(String string, String validatorName) throws ValidationException {
        if (string == null || string.length() == 0)
            throw new ValidationException("验证器 " + validatorName + " 的参数必须是一个非负整数");
        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9')
                throw new ValidationException("验证器 " + validatorName + " 的参数必须是一个非负整数");
        }
        try {
            return Integer.parseInt(string);
        } catch (Exception e) {
            throw new ValidationException("验证器 " + validatorName + " 的参数的取值超过整数的取值范围");
        }
    }

    // 解析浮点型验证器的参数值. 如果参数值不合法, 返回null
    // todo 数值溢出的处理
    private static Double parseParamDouble(String string) {
        if (string.length() == 0)
            return null;
        if (string.indexOf(' ') >= 0) // 不能有空格
            return null;

        try {
            return Double.parseDouble(string);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将包含int数组的字符串转为int数组
     *
     * @param string 待解析的字符串, 如: 1,2,3,123
     * @return 如果是合法的int数组, 并且至少有1个int, 返回Integer数组; 否则返回null
     */
    private static Integer[] parseParamIntArray(String string) throws ValidationException {
        String[] strs = ValidationUtils.split(string, ',');
        List<Integer> ints = new ArrayList<Integer>();
        for (String str : strs) {
            if (ValidationUtils.isIntString(str)) {
                try {
                    int v = Integer.parseInt(str);
                    ints.add(v);
                } catch (NumberFormatException e) { // 应该是数值溢出, 暂时视为数据类型错误
                    return null;
                }
            } else {
                return null;
            }
        }
        if (ints.size() == 0)
            return null;
        Integer[] arr = new Integer[ints.size()];
        return ints.toArray(arr);
    }

    /**
     * 将包含long数组的字符串转为Long数组
     *
     * @param string 待解析的字符串, 如: 1,2,3,123
     * @return 如果是合法的long数组, 并且至少有1个long, 返回Long数组; 否则返回null
     */
    private static Long[] parseParamLongArray(String string) throws ValidationException {
        String[] strs = ValidationUtils.split(string, ',');
        List<Long> longs = new ArrayList<Long>();
        for (String str : strs) {
            if (ValidationUtils.isIntString(str)) {
                try {
                    long v = Long.parseLong(str);
                    longs.add(v);
                } catch (NumberFormatException e) { // 应该是数值溢出, 暂时视为数据类型错误
                    return null;
                }
            } else {
                return null;
            }
        }
        if (longs.size() == 0)
            return null;
        Long[] arr = new Long[longs.size()];
        return longs.toArray(arr);
    }

    /**
     * 将（逗号分隔的）字符串转为字符串数组.
     *
     * 不去重, 因为string是程序员提供的, 可以认为他们不会写错; 即使出现重复, 也不影响最终的验证结果.
     *
     * @param string 待解析的字符串, 如: abc,d,efg,123
     * @return 返回字符串数组; 如果string是空串, 返回 [""]
     */
    private static String[] parseParamStrArray(String string) throws ValidationException {
        if (string.length() == 0)
            return new String[]{""};
        return ValidationUtils.split(string, ',');
    }

    /**
     * 解析 IfIntXx:varname,123 中的冒号后面的部分（1个条件参数后面带1个Int值）
     * @param string 待解析的字符串, 如: "count,10"
     * @param validatorName 验证器名称 "IfIntXx"
     * @return 出错返回null, 否则返回 {"varname", 123}
     */
    private static Object[] parseIfXxxWith1Param1Int(String string, String validatorName) throws ValidationException {
        if (string.length() == 0)
            return null;

        String[] strs = ValidationUtils.split(string, ',');
        if (strs.length != 2)
            return null;

        String varName = strs[0];
        if (varName.length() == 0)
            return null;

        Integer value = validateInt(strs[1], "“" + validatorName + ":" + string + "”中“" + varName + "”后面必须是整数", null);

        return new Object[]{varName, value};
    }

    /**
     * 解析 IfLongXx:varname,123 中的冒号后面的部分（1个条件参数后面带1个Long值）
     * @param string 待解析的字符串, 如: "count,10"
     * @param validatorName 验证器名称 "IfLongXx"
     * @return 出错返回null, 否则返回 {"varname", 123}
     */
    private static Object[] parseIfXxxWith1Param1Long(String string, String validatorName) throws ValidationException {
        if (string.length() == 0)
            return null;

        String[] strs = ValidationUtils.split(string, ',');
        if (strs.length != 2)
            return null;

        String varName = strs[0];
        if (varName.length() == 0)
            return null;

        Long value = validateLong(strs[1], "“" + validatorName + ":" + string + "”中“" + varName + "”后面必须是长整数", null);

        return new Object[]{varName, value};
    }

    /**
     * 解析 IfStrXx:varname,abc 中的冒号后面的部分（1个条件参数后面带1个String值）
     * @param string 待解析的字符串, 如: "sex,male"
     * @param validatorName 验证器名称 "IfStrXx"
     * @return 出错返回null, 否则返回 {"varname", "abc"}
     */
    private static Object[] parseIfXxxWith1Param1Str(String string, String validatorName) throws ValidationException {
        if (string.length() == 0)
            return null;

        String[] strs = ValidationUtils.split(string, ',');
        if (strs.length != 2)
            return null;

        String varName = strs[0];
        if (varName.length() == 0)
            return null;

        return new Object[]{varName, strs[1]};
    }

    /**
     * 解析 IfIntInXxx:varname,1,2,3 中的冒号后面的部分（1个条件参数后面带多个整数）
     * @param string 待解析的字符串, 如: "states,1,2,3"
     * @param validatorName 验证器名称 "IfIntInXxx"
     * @return 出错返回null, 否则返回 {"varname", List{1,2,3}}
     */
    private static Object[] parseIfXxxWith1ParamNInts(String string, String validatorName) throws ValidationException {
        if (string.length() == 0)
            return null;

        String[] strs = ValidationUtils.split(string, ',');
        if (strs.length < 2)
            return null;

        String varName = strs[0];
        if (varName.length() == 0)
            return null;

        List<Integer> params = new ArrayList<>(strs.length - 1);
        for (int i = 1; i < strs.length; i++) {
            String str = strs[i];
            Integer value = validateInt(strs[i], "“" + validatorName + ":" + string + "”中“" + varName + "”后面必须全部是整数，实际上却包含了\"" + str + "\"", null);
            params.add(value);
        }
        return new Object[]{varName, params};
    }

    /**
     * 解析 IfLongInXxx:varname,1,2,3 中的冒号后面的部分（1个条件参数后面带多个整数）
     * @param string 待解析的字符串, 如: "states,1,2,3"
     * @param validatorName 验证器名称 "IfLongInXxx"
     * @return 出错返回null, 否则返回 {"varname", List{1L,2L,3L}}
     */
    private static Object[] parseIfXxxWith1ParamNLongs(String string, String validatorName) throws ValidationException {
        if (string.length() == 0)
            return null;

        String[] strs = ValidationUtils.split(string, ',');
        if (strs.length < 2)
            return null;

        String varName = strs[0];
        if (varName.length() == 0)
            return null;

        List<Long> params = new ArrayList<>(strs.length - 1);
        for (int i = 1; i < strs.length; i++) {
            String str = strs[i];
            Long value = validateLong(strs[i], "“" + validatorName + ":" + string + "”中“" + varName + "”后面必须全部是整数，实际上却包含了\"" + str + "\"", null);
            params.add(value);
        }
        return new Object[]{varName, params};
    }

    /**
     * 解析 IfStrXxx:varname,a,b,abc 中的冒号后面的部分（1个条件参数后面带多个字符串）
     * @param string 待解析的字符串, 如: "state,pending,started"
     * @param validatorName 验证器名称 "IfStrXxx"
     * @return 出错返回null, 否则返回 ["varname", ["a","b","abc"]]
     */
    private static Object[] parseIfXxxWith1ParamNStrs(String string, String validatorName) throws ValidationException {
        if (string.length() == 0)
            return null;

        String[] strs = ValidationUtils.split(string, ',');
        if (strs.length < 2)
            return null;

        String varName = strs[0];
        if (varName.length() == 0)
            return null;

        List<String> params = new ArrayList<>(strs.length - 1);
        for (int i = 1; i < strs.length; i++) {
            params.add(strs[i]);
        }
        return new Object[]{varName, params};
    }

    private static ThreadLocal<SimpleDateFormat> localDateFormatter = new ThreadLocal<SimpleDateFormat>();

    // 解析DateXx验证器的参数值. 如果参数值不合法, 返回null
    private static Long parseParamDateToTimestamp(String string) {

        if (string.indexOf('-') != 4) // 19-01-01这种格式SimpleDateFormat也是能识别的，要排除这种情况
            return null;

        SimpleDateFormat fmt = localDateFormatter.get();
        if (fmt == null) {
            fmt = new SimpleDateFormat("yyyy-MM-dd");
            localDateFormatter.set(fmt);
        }
        try {
            Date date = fmt.parse(string);
            return date.getTime() / 1000L;
        } catch (Exception e) {
            return null;
        }
    }

    private static ThreadLocal<SimpleDateFormat> localDateTimeFormatter = new ThreadLocal<SimpleDateFormat>();

    // 解析DateTimeXx验证器的参数值. 如果参数值不合法, 返回null
    private static Long parseParamDateTimeToTimestamp(String string) {

        if (string.indexOf('-') != 4) // "19-01-01 10:30:33"这种格式SimpleDateFormat也是能识别的，要排除这种情况
            return null;

        SimpleDateFormat fmt = localDateTimeFormatter.get();
        if (fmt == null) {
            fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            localDateTimeFormatter.set(fmt);
        }
        try {
            Date date = fmt.parse(string);
            return date.getTime() / 1000L;
        } catch (Exception e) {
            return null;
        }
    }

    // endregion

    // region 工具

    // 生成最终的(翻译后的)Alias
    private static String finalAlias(String alias) {
        if (alias == null || alias.length() == 0)
            return "Parameter";
        else
            return translateText(alias);
    }

    // 如果提供了 reason, 则先翻译, 再抛出异常; 否则什么也不做
    private static void throwIfHasReason(String reason) throws ValidationException {
        if (reason != null && reason.length() > 0) {
            reason = translateText(reason);
            throw new ValidationException(reason);
        }
    }

    // 抛出异常提示验证器格式错误
    private static void throwFormatError(String validatorName) throws ValidationException {
        String sampleFormat = sampleFormats.get(validatorName);
        if (sampleFormat == null)
            throw new ValidationException("验证器 " + validatorName + " 格式错误");
        throw new ValidationException("验证器 " + validatorName + " 格式错误. 正确的格式示例: "+ sampleFormat);
    }

    // 所有验证器格式示例
    private static Map<String, String> sampleFormats = new HashMap<String, String>(256) {{
        // 整型（不提供length检测,因为负数的符号位会让人混乱, 可以用大于小于比较来做到这一点）
        put("Int", "Int");
        put("IntEq", "IntEq:100");
        put("IntNe", "IntNe:100");
        put("IntGt", "IntGt:100");
        put("IntGe", "IntGe:100");
        put("IntLt", "IntLt:100");
        put("IntLe", "IntLe:100");
        put("IntGtLt", "IntGtLt:1,100");
        put("IntGeLe", "IntGeLe:1,100");
        put("IntGtLe", "IntGtLe:1,100");
        put("IntGeLt", "IntGeLt:1,100");
        put("IntIn", "IntIn:2,3,5,7,11");
        put("IntNotIn", "IntNotIn:2,3,5,7,11");

        // 长整型
        put("Long", "Long");
        put("LongEq", "LongEq:100");
        put("LongNe", "LongNe:100");
        put("LongGt", "LongGt:100");
        put("LongGe", "LongGe:100");
        put("LongLt", "LongLt:100");
        put("LongLe", "LongLe:100");
        put("LongGtLt", "LongGtLt:1,100");
        put("LongGeLe", "LongGeLe:1,100");
        put("LongGtLe", "LongGtLe:1,100");
        put("LongGeLt", "LongGeLt:1,100");
        put("LongIn", "LongIn:2,3,5,7,11");
        put("LongNotIn", "LongNotIn:2,3,5,7,11");

        // 浮点型（内部一律使用double来处理）
        put("Float", "Float");
        put("FloatGt", "FloatGt:1.0");
        put("FloatGe", "FloatGe:1.0");
        put("FloatLt", "FloatLt:1.0");
        put("FloatLe", "FloatLe:1.0");
        put("FloatGtLt", "FloatGtLt:0,1.0");
        put("FloatGeLe", "FloatGeLe:0,1.0");
        put("FloatGtLe", "FloatGtLe:0,1.0");
        put("FloatGeLt", "FloatGeLt:0,1.0");

        // bool型
        put("Bool", "Bool");
        put("BoolSmart", "BoolSmart");
        put("BoolTrue", "BoolTrue");
        put("BoolFalse", "BoolFalse");
        put("BoolSmartTrue", "BoolSmartTrue");
        put("BoolSmartFalse", "BoolSmartFalse");

        // 字符串
        put("Str", "Str");
        put("StrEq", "StrEq:abc");
        put("StrEqI", "StrEqI:abc");
        put("StrNe", "StrNe:abc");
        put("StrNeI", "StrNeI:abc");
        put("StrIn", "StrIn:abc,def,g");
        put("StrInI", "StrInI:abc,def,g");
        put("StrNotIn", "StrNotIn:abc,def,g");
        put("StrNotInI", "StrNotInI:abc,def,g");
        put("StrLen", "StrLen:8");
        put("StrLenGe", "StrLenGe:8");
        put("StrLenLe", "StrLenLe:8");
        put("StrLenGeLe", "StrLenGeLe:6,8");
        put("ByteLen", "ByteLen:8");
        put("ByteLenGe", "ByteLenGe:8");
        put("ByteLenLe", "ByteLenLe:8");
        put("ByteLenGeLe", "ByteLenGeLe:6,8");
        put("Letters", "Letters");
        put("Alphabet", "Alphabet"); // 同Letters
        put("Numbers", "Numbers");
        put("Digits", "Digits"); // 同Numbers
        put("LettersNumbers", "LettersNumbers");
        put("Numeric", "Numeric");
        put("VarName", "VarName");
        put("Email", "Email");
        put("Url", "Url");
        put("HttpUrl", "HttpUrl");
        put("Ip", "Ip");
        put("Ipv4", "Ipv4");
        put("Ipv6", "Ipv6");
        put("Mac", "Mac");
        put("Regexp", "Regexp:/^abc$/"); // Perl正则表达式匹配

        // 数组. 如何检测数组长度为0
        put("List", "List");
        put("ListLen", "ListLen:5");
        put("ListLenGe", "ListLenGe:1");
        put("ListLenLe", "ListLenLe:9");
        put("ListLenGeLe", "ListLenGeLe:1,9");
        put("Arr", "Arr");
        put("ArrLen", "ArrLen:5");
        put("ArrLenGe", "ArrLenGe:1");
        put("ArrLenLe", "ArrLenLe:9");
        put("ArrLenGeLe", "ArrLenGeLe:1,9");

        // 对象
        put("Map", "Map");

        // 文件
        put("File", "File");
        put("FileMaxSize", "FileMaxSize:10mb");
        put("FileMinSize", "FileMinSize:100kb");
        put("FileImage", "FileImage");
        put("FileVideo", "FileVideo");
        put("FileAudio", "FileAudio");
        put("FileMimes", "FileMimes:mpeg,jpeg,png");

        // Date & Time
        put("Date", "Date");
        put("DateFrom", "DateFrom:2017-04-13");
        put("DateTo", "DateTo:2017-04-13");
        put("DateFromTo", "DateFromTo:2017-04-13,2017-04-13");
        put("DateTime", "DateTime");
        put("DateTimeFrom", "DateTimeFrom:2017-04-13 12:00:00");
        put("DateTimeTo", "DateTimeTo:2017-04-13 12:00:00");
        put("DateTimeFromTo", "DateTimeFromTo:2017-04-13 12:00:00,2017-04-13 12:00:00");
//        put("Time", "Time");
//        put("TimeZone", "TimeZone:timezone_identifiers_list()");

        // 其它
        put("Required", "Required");

        // 条件判断
        put("If", "If:selected"); // 如果参数"selected"值等于 1, true, "1", "true", "yes"或 "y"(字符串忽略大小写)
        put("IfNot", "IfNot:selected"); // 如果参数"selected"值等于 0, false, "0", "false", "no"或"n"(字符串忽略大小写)
        put("IfTrue", "IfTrue:selected"); // 如果参数"selected"值等于 true 或 "true"(忽略大小写)
        put("IfFalse", "IfFalse:selected"); // 如果参数"selected"值等于 false 或 "false"(忽略大小写)
        put("IfExist", "IfExist:var"); // 如果参数"var"存在
        put("IfNotExist", "IfNotExist:var"); // 如果参数"var"不存在
        put("IfIntEq", "IfIntEq:var,1"); // if (type == 1)
        put("IfIntNe", "IfIntNe:var,2"); // if (state != 2). 特别要注意的是如果条件参数var的数据类型不匹配, 那么If条件是成立的; 而其它几个IfIntXx当条件参数var的数据类型不匹配时, If条件不成立
        put("IfIntGt", "IfIntGt:var,0"); // if (var > 0)
        put("IfIntLt", "IfIntLt:var,1"); // if (var < 1)
        put("IfIntGe", "IfIntGe:var,6"); // if (var >= 6)
        put("IfIntLe", "IfIntLe:var,8"); // if (var <= 8)
        put("IfIntIn", "IfIntIn:var,2,3,5,7"); // 如果var的值等于2,3,5,7中的某一个
        put("IfIntNotIn", "IfIntNotIn:var,2,3,5,7"); // 如果var的值不等于2,3,5,7中的任何一个
        put("IfLongEq", "IfLongEq:var,1"); // if (type == 1L)
        put("IfLongNe", "IfLongNe:var,2"); // if (state != 2L). 特别要注意的是如果条件参数var的数据类型不匹配, 那么If条件是成立的; 而其它几个IfLongXx当条件参数var的数据类型不匹配时, If条件不成立
        put("IfLongGt", "IfLongGt:var,0"); // if (var > 0L)
        put("IfLongLt", "IfLongLt:var,1"); // if (var < 1L)
        put("IfLongGe", "IfLongGe:var,6"); // if (var >= 6L)
        put("IfLongLe", "IfLongLe:var,8"); // if (var <= 8L)
        put("IfLongIn", "IfLongIn:var,2,3,5,7"); // 如果var的值等于2L,3L,5L,7L中的某一个
        put("IfLongNotIn", "IfLongNotIn:var,2,3,5,7"); // 如果var的值不等于2L,3L,5L,7L中的任何一个
        put("IfStrEq", "IfStrEq:var,waiting"); // if ("waiting".equals(var))
        put("IfStrNe", "IfStrNe:var,editing"); // if (!"editing".equals(var)). 特别要注意的是如果条件参数var的数据类型不匹配, 那么If条件是成立的; 而其它几个IfStrXx当条件参数var的数据类型不匹配时, If条件不成立
        put("IfStrGt", "IfStrGt:var,a"); // if (var.compareTo("a") > 0)
        put("IfStrLt", "IfStrLt:var,z"); // if (var.compareTo("z") < 0)
        put("IfStrGe", "IfStrGe:var,A"); // if (var.compareTo("A") >= 0)
        put("IfStrLe", "IfStrLe:var,Z"); // if (var.compareTo("Z") <= 0)
        put("IfStrIn", "IfStrIn:var,normal,warning,error"); // 如果var的值等于"normal", "warning", "error"中的某一个
        put("IfStrNotIn", "IfStrNotIn:var,warning,error"); // 如果var的值不等于"normal", "warning", "error"中的任何一个
//        put("IfSame", "IfSame:AnotherParameter");
//        put("IfNotSame", "IfNotSame:AnotherParameter");
//        put("IfAny", "IfAny:type,1,type,2"); //待定

//        // 预处理（只处理字符串类型, 如果是其它类型, 则原值返回）
//        put("Trim", "Trim");
//        put("Lowercase", "Lowercase");
//        put("Uppercase", "Uppercase");
//        put("ToInt", "ToInt");
//        put("ToString", "ToString");
    }};

    // endregion

    // region Int

    public static Integer validateInt(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    return Integer.parseInt((String) value);
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return (Integer) value;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("Int", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntEq(Object value, Integer equalVal, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val == equalVal)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            if (equalVal.intValue() == (Integer) value)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntEq", "{{param}}", alias, "{{value}}", equalVal.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntNe(Object value, Integer equalVal, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val != equalVal)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            if (equalVal.intValue() != (Integer) value)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntNe", "{{param}}", alias, "{{value}}", equalVal.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntGt(Object value, Integer min, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val > min)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            if (((Integer) value).intValue() > min)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntGt", "{{param}}", alias, "{{min}}", min.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntGe(Object value, Integer min, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val >= min)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            if (((Integer) value).intValue() >= min)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntGe", "{{param}}", alias, "{{min}}", min.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntLt(Object value, Integer max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val < max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            if (((Integer) value).intValue() < max)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntLt", "{{param}}", alias, "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntLe(Object value, Integer max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val <= max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            if (((Integer) value).intValue() <= max)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntLe", "{{param}}", alias, "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntGtLt(Object value, Integer min, Integer max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val > min && val < max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            Integer valInteger = (Integer) value;
            if (valInteger > min && valInteger < max)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntGtLt", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntGeLe(Object value, Integer min, Integer max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val >= min && val <= max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            Integer valInteger = (Integer) value;
            if (valInteger >= min && valInteger <= max)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntGeLe", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntGtLe(Object value, Integer min, Integer max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val > min && val <= max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            Integer valInteger = (Integer) value;
            if (valInteger > min && valInteger <= max)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntGtLe", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntGeLt(Object value, Integer min, Integer max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    if (val >= min && val < max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            Integer valInteger = (Integer) value;
            if (valInteger >= min && valInteger < max)
                return (Integer) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else
            throwWithErrorTemplate("IntGeLt", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntIn(Object value, Integer[] inValues, String reason, String alias) throws ValidationException {

        if (inValues == null || inValues.length == 0)
            throw new ValidationException("必须提供可取值的列表");

        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    for (Integer v : inValues) {
                        if (v == val)
                            return v;
                    }
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            int val = (Integer) value;
            for (Integer v : inValues) {
                if (v == val)
                    return v;
            }
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < inValues.length; i++) {
                Integer v = inValues[i];
                if (i > 0)
                    builder.append(", ");
                builder.append(v);
            }
            throwWithErrorTemplate("IntIn", "{{param}}", alias, "{{valueList}}", builder.toString());
        }
        return null; // 永远不会执行这一句
    }

    public static Integer validateIntNotIn(Object value, Integer[] notInValues, String reason, String alias) throws ValidationException {

        if (notInValues == null || notInValues.length == 0)
            throw new ValidationException("必须提供不可取值的列表");

        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    boolean in = false;
                    for (Integer v : notInValues) {
                        if (v == val) {
                            in = true;
                            break;
                        }
                    }
                    if (!in)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            int val = (Integer) value;
            boolean in = false;
            for (Integer v : notInValues) {
                if (v == val) {
                    in = true;
                    break;
                }
            }
            if (!in)
                return val;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Int", "{{param}}", alias);
        else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < notInValues.length; i++) {
                Integer v = notInValues[i];
                if (i > 0)
                    builder.append(", ");
                builder.append(v);
            }
            throwWithErrorTemplate("IntNotIn", "{{param}}", alias, "{{valueList}}", builder.toString());
        }
        return null; // 永远不会执行这一句
    }

    // endregion

    // region Long

    public static Long validateLong(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    return Long.parseLong((String) value);
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return (Long) value;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("Long", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static Long validateLongEq(Object value, Long equalVal, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val == equalVal)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (equalVal == valLong)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            if (equalVal.longValue() == (Long) value)
                return (Long) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongEq", "{{param}}", alias, "{{value}}", equalVal.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongNe(Object value, Long equalVal, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val != equalVal)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (equalVal != valLong)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            if (equalVal.longValue() != (Long) value)
                return (Long) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongNe", "{{param}}", alias, "{{value}}", equalVal.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongGt(Object value, Long min, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val > min)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (valLong > min)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            if (((Long) value) > min)
                return (Long) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongGt", "{{param}}", alias, "{{min}}", min.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongGe(Object value, Long min, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val >= min)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (valLong >= min)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            if (((Long) value) >= min)
                return (Long) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongGe", "{{param}}", alias, "{{min}}", min.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongLt(Object value, Long max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val < max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (valLong < max)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            if (((Long) value) < max)
                return (Long) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongLt", "{{param}}", alias, "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongLe(Object value, Long max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val <= max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (valLong <= max)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            if (((Long) value) <= max)
                return (Long) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongLe", "{{param}}", alias, "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongGtLt(Object value, Long min, Long max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val > min && val < max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (valLong > min && valLong < max)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            Long valLong = (Long) value;
            if (valLong > min && valLong < max)
                return valLong;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongGtLt", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongGeLe(Object value, Long min, Long max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val >= min && val <= max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (valLong >= min && valLong <= max)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            Long valLong = (Long) value;
            if (valLong >= min && valLong <= max)
                return valLong;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongGeLe", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongGtLe(Object value, Long min, Long max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val > min && val <= max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (valLong > min && valLong <= max)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            Long valLong = (Long) value;
            if (valLong > min && valLong <= max)
                return valLong;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongGtLe", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongGeLt(Object value, Long min, Long max, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    if (val >= min && val < max)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long valLong = ((Integer) value).longValue();
            if (valLong >= min && valLong < max)
                return valLong;
            isTypeError = false;
        } else if (value instanceof Long) {
            Long valLong = (Long) value;
            if (valLong >= min && valLong < max)
                return valLong;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else
            throwWithErrorTemplate("LongGeLt", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Long validateLongIn(Object value, Long[] inValues, String reason, String alias) throws ValidationException {

        if (inValues == null || inValues.length == 0)
            throw new ValidationException("必须提供可取值的列表");

        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    for (Long v : inValues) {
                        if (v == val)
                            return v;
                    }
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long val = ((Integer) value).longValue();
            for (Long v : inValues) {
                if (v == val)
                    return v;
            }
            isTypeError = false;
        } else if (value instanceof Long) {
            long val = (Long) value;
            for (Long v : inValues) {
                if (v == val)
                    return v;
            }
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < inValues.length; i++) {
                Long v = inValues[i];
                if (i > 0)
                    builder.append(", ");
                builder.append(v);
            }
            throwWithErrorTemplate("LongIn", "{{param}}", alias, "{{valueList}}", builder.toString());
        }
        return null; // 永远不会执行这一句
    }

    public static Long validateLongNotIn(Object value, Long[] notInValues, String reason, String alias) throws ValidationException {

        if (notInValues == null || notInValues.length == 0)
            throw new ValidationException("必须提供不可取值的列表");

        boolean isTypeError;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    boolean in = false;
                    for (Long v : notInValues) {
                        if (v == val) {
                            in = true;
                            break;
                        }
                    }
                    if (!in)
                        return val;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                isTypeError = true;
        } else if (value instanceof Integer) {
            long val = ((Integer) value).longValue();
            boolean in = false;
            for (Long v : notInValues) {
                if (v == val) {
                    in = true;
                    break;
                }
            }
            if (!in)
                return val;
            isTypeError = false;
        } else if (value instanceof Long) {
            long val = (Long) value;
            boolean in = false;
            for (Long v : notInValues) {
                if (v == val) {
                    in = true;
                    break;
                }
            }
            if (!in)
                return val;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Long", "{{param}}", alias);
        else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < notInValues.length; i++) {
                Long v = notInValues[i];
                if (i > 0)
                    builder.append(", ");
                builder.append(v);
            }
            throwWithErrorTemplate("LongNotIn", "{{param}}", alias, "{{valueList}}", builder.toString());
        }
        return null; // 永远不会执行这一句
    }

    // endregion

    // region String

    public static String validateStr(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            return (String) value;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("Str", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static String validateStrEq(Object value, String equalsValue, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (equalsValue.equals(value))
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrEq", "{{param}}", alias, "{{value}}", equalsValue);
        return null; // 永远不会执行这一句
    }

    public static String validateStrNe(Object value, String equalsValue, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (!equalsValue.equals(value))
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrNe", "{{param}}", alias, "{{value}}", equalsValue);
        return null; // 永远不会执行这一句
    }

    public static String validateStrEqI(Object value, String equalsValue, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (equalsValue.equalsIgnoreCase((String)value))
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrEqI", "{{param}}", alias, "{{value}}", equalsValue);
        return null; // 永远不会执行这一句
    }

    public static String validateStrNeI(Object value, String equalsValue, String reason, String alias) throws ValidationException {
        boolean isTypeError;
        if (value instanceof String) {
            if (!equalsValue.equalsIgnoreCase((String)value))
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrNeI", "{{param}}", alias, "{{value}}", equalsValue);
        return null; // 永远不会执行这一句
    }

    public static String validateStrIn(Object value, String[] inStrings, String reason, String alias) throws ValidationException {

        if (inStrings == null || inStrings.length == 0)
            throw new ValidationException("必须提供可取值的列表");

        boolean isTypeError;
        if (value instanceof String) {
            String val = (String) value;
            for (String str : inStrings) {
                if (str.equals(val))
                    return val;
            }
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrIn", "{{param}}", alias,
            "{{valueList}}", "\"" + ValidationUtils.implode(inStrings, "\", \"") + "\"");
        return null; // 永远不会执行这一句
    }

    public static String validateStrInI(Object value, String[] inStrings, String reason, String alias) throws ValidationException {

        if (inStrings == null || inStrings.length == 0)
            throw new ValidationException("必须提供可取值的列表");

        boolean isTypeError;
        if (value instanceof String) {
            String val = (String) value;
            for (String str : inStrings) {
                if (str.equalsIgnoreCase(val))
                    return val;
            }
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrInI", "{{param}}", alias,
            "{{valueList}}", "\"" + ValidationUtils.implode(inStrings, "\", \"") + "\"");
        return null; // 永远不会执行这一句
    }

    public static String validateStrNotIn(Object value, String[] notInStrings, String reason, String alias) throws ValidationException {

        if (notInStrings == null || notInStrings.length == 0)
            throw new ValidationException("必须提供不可取值的列表");

        boolean isTypeError;
        if (value instanceof String) {
            String val = (String) value;
            boolean in = false;
            for (String str : notInStrings) {
                if (str.equals(val)) {
                    in = true;
                    break;
                }
            }
            if (!in)
                return val;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrNotIn", "{{param}}", alias,
            "{{valueList}}", "\"" + ValidationUtils.implode(notInStrings, "\", \"") + "\"");
        return null; // 永远不会执行这一句
    }

    public static String validateStrNotInI(Object value, String[] notInStrings, String reason, String alias) throws ValidationException {

        if (notInStrings == null || notInStrings.length == 0)
            throw new ValidationException("必须提供不可取值的列表");

        boolean isTypeError;
        if (value instanceof String) {
            String val = (String) value;
            boolean in = false;
            for (String str : notInStrings) {
                if (str.equalsIgnoreCase(val)) {
                    in = true;
                    break;
                }
            }
            if (!in)
                return val;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrNotInI", "{{param}}", alias,
            "{{valueList}}", "\"" + ValidationUtils.implode(notInStrings, "\", \"") + "\"");
        return null; // 永远不会执行这一句
    }

    public static String validateStrLen(Object value, Integer length, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (((String) value).length() == length)
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrLen", "{{param}}", alias, "{{length}}", length.toString());
        return null; // 永远不会执行这一句
    }

    public static String validateStrLenGe(Object value, Integer minLen, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (((String) value).length() >= minLen)
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrLenGe", "{{param}}", alias, "{{min}}", minLen.toString());
        return null; // 永远不会执行这一句
    }

    public static String validateStrLenLe(Object value, Integer maxLen, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (((String) value).length() <= maxLen)
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrLenLe", "{{param}}", alias, "{{max}}", maxLen.toString());
        return null; // 永远不会执行这一句
    }

    public static String validateStrLenGeLe(Object value, Integer minLen, Integer maxLen, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            int len = ((String) value).length();
            if (len >= minLen && len <= maxLen)
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("StrLenGeLe", "{{param}}", alias, "{{min}}", minLen.toString(), "{{max}}", maxLen.toString());
        return null; // 永远不会执行这一句
    }

    static Pattern patternLetters = Pattern.compile("^[a-zA-Z]+$");

    public static String validateLetters(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternLetters.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Letters", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static String validateAlphabet(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternLetters.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Alphabet", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternNumbers = Pattern.compile("^[0-9]+$");

    public static String validateNumbers(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternNumbers.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Numbers", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static String validateDigits(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternNumbers.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Digits", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternLettersNumbers = Pattern.compile("^[a-zA-Z0-9]+$");

    public static String validateLettersNumbers(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternLettersNumbers.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("LettersNumbers", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternNumeric = Pattern.compile("^[+-]?(\\d+\\.\\d+|\\d+\\.?|\\.\\d+)$");

    public static String validateNumeric(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternNumeric.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Numeric", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternVarName = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    public static String validateVarName(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternVarName.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("VarName", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternEmailUserName = Pattern.compile("^([a-z0-9A-Z]+[-|.]?)+[a-z0-9A-Z]$");
    static Pattern patternEmailDomain = Pattern.compile("^([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$");

    public static String validateEmail(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            String[] parts = ValidationUtils.split((String) value, '@');
            if (parts.length == 2) {
                if (patternEmailUserName.matcher(parts[0]).matches() &&
                    patternEmailDomain.matcher(parts[1]).matches())
                    return (String) value;
            }
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Email", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternUrl = Pattern.compile("^[a-zA-z]{1,100}://[^\\s]+$");

    public static String validateUrl(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternUrl.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Url", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternHttpUrl = Pattern.compile("^[hH][tT][tT][pP][sS]?://[^\\s]+$");

    public static String validateHttpUrl(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternHttpUrl.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("HttpUrl", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static String validateIp(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            String strVal = (String) value;
            if (patternIpv4.matcher(strVal).matches() ||
                patternIpv6.matcher(strVal).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Ip", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternIpv4 = Pattern.compile("^((\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5]|[*])\\.){3}(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5]|[*])$");

    public static String validateIpv4(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternIpv4.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Ipv4", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternIpv6 = Pattern.compile(
        "(^((([0-9A-Fa-f]{1,4}:){7}(([0-9A-Fa-f]{1,4}){1}|:))"
        + "|(([0-9A-Fa-f]{1,4}:){6}((:[0-9A-Fa-f]{1,4}){1}|"
        + "((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
        + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
        + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))|"
        + "(([0-9A-Fa-f]{1,4}:){5}((:[0-9A-Fa-f]{1,4}){1,2}|"
        + ":((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
        + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
        + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))|"
        + "(([0-9A-Fa-f]{1,4}:){4}((:[0-9A-Fa-f]{1,4}){1,3}"
        + "|:((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
        + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|[0-1][0-9][0-9]|"
        + "([0-9]){1,2})){3})|:))|(([0-9A-Fa-f]{1,4}:){3}((:[0-9A-Fa-f]{1,4}){1,4}|"
        + ":((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
        + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
        + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))|"
        + "(([0-9A-Fa-f]{1,4}:){2}((:[0-9A-Fa-f]{1,4}){1,5}|"
        + ":((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
        + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
        + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))"
        + "|(([0-9A-Fa-f]{1,4}:){1}((:[0-9A-Fa-f]{1,4}){1,6}"
        + "|:((22[0-3]|2[0-1][0-9]|[0-1][0-9][0-9]|"
        + "([0-9]){1,2})([.](25[0-5]|2[0-4][0-9]|"
        + "[0-1][0-9][0-9]|([0-9]){1,2})){3})|:))|"
        + "(:((:[0-9A-Fa-f]{1,4}){1,7}|(:[fF]{4}){0,1}:((22[0-3]|2[0-1][0-9]|"
        + "[0-1][0-9][0-9]|([0-9]){1,2})"
        + "([.](25[0-5]|2[0-4][0-9]|[0-1][0-9][0-9]|([0-9]){1,2})){3})|:)))$)");

    public static String validateIpv6(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternIpv6.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Ipv6", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    static Pattern patternMac = Pattern.compile("^[a-fA-F0-9]{2}(:[a-fA-F0-9]{2}){5}$");

    public static String validateMac(Object value, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        boolean isTypeError;
        if (value instanceof String) {
            if (patternMac.matcher((String) value).matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Mac", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static String validateRegexp(Object value, Pattern pattern, String reason, String alias) throws ValidationException {
        if (value == null)
            throw new ValidationException("参数 value 不可为 null");
        if (pattern == null)
            throw new ValidationException("没有提供参数 pattern");

        boolean isTypeError;
        if (value instanceof String) {
            Matcher m = pattern.matcher((String) value);
            if (m.matches())
                return (String) value;
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Str", "{{param}}", alias);
        throwWithErrorTemplate("Regexp", "{{param}}", alias, "{{regexp}}", pattern.pattern());
        return null; // 永远不会执行这一句
    }

    // endregion

    // region Bool

    public static Boolean validateBool(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            String str = (String) value;
            if (str.equalsIgnoreCase("true"))
                return true;
            else if (str.equalsIgnoreCase("false"))
                return false;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("Bool", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static Boolean validateBoolTrue(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            String str = (String) value;
            if (str.equalsIgnoreCase("true"))
                return true;
        } else if (value instanceof Boolean) {
            if ((Boolean) value)
                return (Boolean) value;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("BoolTrue", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static Boolean validateBoolFalse(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            String str = (String) value;
            if (str.equalsIgnoreCase("false"))
                return false;
        } else if (value instanceof Boolean) {
            if (!((Boolean) value))
                return (Boolean) value;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("BoolFalse", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static Boolean validateBoolSmart(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            String str = (String) value;
            if (str.equalsIgnoreCase("true") ||
                str.equals("1") ||
                str.equalsIgnoreCase("yes") ||
                str.equalsIgnoreCase("y"))
                return true;
            else if (str.equalsIgnoreCase("false") ||
                str.equals("0") ||
                str.equalsIgnoreCase("no") ||
                str.equalsIgnoreCase("n"))
                return false;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Integer) {
            int v = (Integer) value;
            if (v == 1)
                return true;
            else if (v == 0)
                return false;
        } else if (value instanceof Long) {
            long v = (Long) value;
            if (v == 1)
                return true;
            else if (v == 0)
                return false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("BoolSmart", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static Boolean validateBoolSmartTrue(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            String str = (String) value;
            if (str.equalsIgnoreCase("true") ||
                str.equals("1") ||
                str.equalsIgnoreCase("yes") ||
                str.equalsIgnoreCase("y"))
                return true;
        } else if (value instanceof Boolean) {
            if ((Boolean) value)
                return (Boolean) value;
        } else if (value instanceof Integer) {
            int v = (Integer) value;
            if (v == 1)
                return true;
        } else if (value instanceof Long) {
            long v = (Long) value;
            if (v == 1)
                return true;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("BoolSmartTrue", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static Boolean validateBoolSmartFalse(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            String str = (String) value;
            if (str.equalsIgnoreCase("false") ||
                str.equals("0") ||
                str.equalsIgnoreCase("no") ||
                str.equalsIgnoreCase("n"))
                return false;
        } else if (value instanceof Boolean) {
            if (!((Boolean) value))
                return (Boolean) value;
        } else if (value instanceof Integer) {
            int v = (Integer) value;
            if (v == 0)
                return false;
        } else if (value instanceof Long) {
            long v = (Long) value;
            if (v == 0)
                return false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("BoolSmartFalse", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    // endregion

    // region Float

    public static Double validateFloat(Object value, String reason, String alias) throws ValidationException {
        if (value instanceof String) {
            Double d = parseParamDouble((String) value);
            if (d != null)
                return d;
        } else if (value instanceof Integer) {
            return ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            return ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Long) { // 不考虑精度不够的问题
            return ((Long) value).doubleValue();
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("Float", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static Double validateFloatGt(Object value, Double min, String reason, String alias) throws ValidationException {
        Double d;
        if (value instanceof String) {
            d = parseParamDouble((String) value);
        } else if (value instanceof Integer) {
            d = ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            d = ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            d = (Double) value;
        } else if (value instanceof Long) { // 不考虑精度不够的问题
            d = ((Long) value).doubleValue();
        } else
            d = null;

        boolean isTypeError;
        if (d == null) {
            isTypeError = true;
        } else {
            if (d > min)
                return d;
            isTypeError = false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Float", "{{param}}", alias);
        throwWithErrorTemplate("FloatGt", "{{param}}", alias, "{{min}}", min.toString());
        return null; // 永远不会执行这一句
    }

    public static Double validateFloatGe(Object value, Double min, String reason, String alias) throws ValidationException {
        Double d;
        if (value instanceof String) {
            d = parseParamDouble((String) value);
        } else if (value instanceof Integer) {
            d = ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            d = ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            d = (Double) value;
        } else if (value instanceof Long) { // 不考虑精度不够的问题
            d = ((Long) value).doubleValue();
        } else
            d = null;

        boolean isTypeError;
        if (d == null) {
            isTypeError = true;
        } else {
            if (d >= min)
                return d;
            isTypeError = false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Float", "{{param}}", alias);
        throwWithErrorTemplate("FloatGe", "{{param}}", alias, "{{min}}", min.toString());
        return null; // 永远不会执行这一句
    }

    public static Double validateFloatLt(Object value, Double max, String reason, String alias) throws ValidationException {
        Double d;
        if (value instanceof String) {
            d = parseParamDouble((String) value);
        } else if (value instanceof Integer) {
            d = ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            d = ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            d = (Double) value;
        } else if (value instanceof Long) { // 不考虑精度不够的问题
            d = ((Long) value).doubleValue();
        } else
            d = null;

        boolean isTypeError;
        if (d == null) {
            isTypeError = true;
        } else {
            if (d < max)
                return d;
            isTypeError = false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Float", "{{param}}", alias);
        throwWithErrorTemplate("FloatLt", "{{param}}", alias, "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Double validateFloatLe(Object value, Double max, String reason, String alias) throws ValidationException {
        Double d;
        if (value instanceof String) {
            d = parseParamDouble((String) value);
        } else if (value instanceof Integer) {
            d = ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            d = ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            d = (Double) value;
        } else if (value instanceof Long) { // 不考虑精度不够的问题
            d = ((Long) value).doubleValue();
        } else
            d = null;

        boolean isTypeError;
        if (d == null) {
            isTypeError = true;
        } else {
            if (d <= max)
                return d;
            isTypeError = false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Float", "{{param}}", alias);
        throwWithErrorTemplate("FloatLe", "{{param}}", alias, "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Double validateFloatGtLt(Object value, Double min, Double max, String reason, String alias) throws ValidationException {
        Double d;
        if (value instanceof String) {
            d = parseParamDouble((String) value);
        } else if (value instanceof Integer) {
            d = ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            d = ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            d = (Double) value;
        } else if (value instanceof Long) { // 不考虑精度不够的问题
            d = ((Long) value).doubleValue();
        } else
            d = null;

        boolean isTypeError;
        if (d == null) {
            isTypeError = true;
        } else {
            if (d > min && d < max)
                return d;
            isTypeError = false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Float", "{{param}}", alias);
        throwWithErrorTemplate("FloatGtLt", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Double validateFloatGeLe(Object value, Double min, Double max, String reason, String alias) throws ValidationException {
        Double d;
        if (value instanceof String) {
            d = parseParamDouble((String) value);
        } else if (value instanceof Integer) {
            d = ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            d = ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            d = (Double) value;
        } else if (value instanceof Long) { // 不考虑精度不够的问题
            d = ((Long) value).doubleValue();
        } else
            d = null;

        boolean isTypeError;
        if (d == null) {
            isTypeError = true;
        } else {
            if (d >= min && d <= max)
                return d;
            isTypeError = false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Float", "{{param}}", alias);
        throwWithErrorTemplate("FloatGeLe", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Double validateFloatGtLe(Object value, Double min, Double max, String reason, String alias) throws ValidationException {
        Double d;
        if (value instanceof String) {
            d = parseParamDouble((String) value);
        } else if (value instanceof Integer) {
            d = ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            d = ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            d = (Double) value;
        } else if (value instanceof Long) { // 不考虑精度不够的问题
            d = ((Long) value).doubleValue();
        } else
            d = null;

        boolean isTypeError;
        if (d == null) {
            isTypeError = true;
        } else {
            if (d > min && d <= max)
                return d;
            isTypeError = false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Float", "{{param}}", alias);
        throwWithErrorTemplate("FloatGtLe", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    public static Double validateFloatGeLt(Object value, Double min, Double max, String reason, String alias) throws ValidationException {
        Double d;
        if (value instanceof String) {
            d = parseParamDouble((String) value);
        } else if (value instanceof Integer) {
            d = ((Integer) value).doubleValue();
        } else if (value instanceof Float) {
            d = ((Float) value).doubleValue();
        } else if (value instanceof Double) {
            d = (Double) value;
        } else if (value instanceof Long) { // 不考虑精度不够的问题
            d = ((Long) value).doubleValue();
        } else
            d = null;

        boolean isTypeError;
        if (d == null) {
            isTypeError = true;
        } else {
            if (d >= min && d < max)
                return d;
            isTypeError = false;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Float", "{{param}}", alias);
        throwWithErrorTemplate("FloatGeLt", "{{param}}", alias, "{{min}}", min.toString(), "{{max}}", max.toString());
        return null; // 永远不会执行这一句
    }

    // endregion

    // region Date & Time

    protected static String validateDate(Object value, String reason, String alias) throws ValidationException {

        if (value instanceof String) {
            String dateString = (String) value;

            if (dateString.indexOf('-') == 4) { // 19-01-01这种格式SimpleDateFormat也是能识别的，要排除这种情况

                SimpleDateFormat fmt = localDateFormatter.get();
                if (fmt == null) {
                    fmt = new SimpleDateFormat("yyyy-MM-dd");
                    localDateFormatter.set(fmt);
                }

                try {
                    fmt.parse(dateString);
                    return dateString;
                } catch (Exception e) {
                    //
                }
            }
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("Date", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    protected static String validateDateFrom(Object value, Long fromTimestamp, String reason, String alias) throws ValidationException {

        boolean isTypeError;

        String dateString;
        if (value instanceof String) {
            dateString = (String) value;
            if (dateString.indexOf('-') != 4) { // 19-01-01这种格式SimpleDateFormat也是能识别的，要排除这种情况
                isTypeError = true;
            } else {
                SimpleDateFormat fmt = localDateFormatter.get();
                if (fmt == null) {
                    fmt = new SimpleDateFormat("yyyy-MM-dd");
                    localDateFormatter.set(fmt);
                }

                try {
                    Date date = fmt.parse(dateString);
                    long timestamp = date.getTime() / 1000L;
                    if (timestamp >= fromTimestamp)
                        return dateString;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true;
                }
            }
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Date", "{{param}}", alias);

        SimpleDateFormat fmt = localDateFormatter.get();
        if (fmt == null) {
            fmt = new SimpleDateFormat("yyyy-MM-dd");
            localDateFormatter.set(fmt);
        }
        String fromDateString = fmt.format(new Date(fromTimestamp * 1000L));
        throwWithErrorTemplate("DateFrom", "{{param}}", alias, "{{from}}", fromDateString);
        return null; // 永远不会执行这一句
    }

    protected static String validateDateTo(Object value, Long toTimestamp, String reason, String alias) throws ValidationException {

        boolean isTypeError;

        String dateString;
        if (value instanceof String) {
            dateString = (String) value;
            if (dateString.indexOf('-') != 4) { // 19-01-01这种格式SimpleDateFormat也是能识别的，要排除这种情况
                isTypeError = true;
            } else {
                SimpleDateFormat fmt = localDateFormatter.get();
                if (fmt == null) {
                    fmt = new SimpleDateFormat("yyyy-MM-dd");
                    localDateFormatter.set(fmt);
                }

                try {
                    Date date = fmt.parse(dateString);
                    long timestamp = date.getTime() / 1000L;
                    if (timestamp <= toTimestamp)
                        return dateString;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true;
                }
            }
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Date", "{{param}}", alias);

        SimpleDateFormat fmt = localDateFormatter.get();
        if (fmt == null) {
            fmt = new SimpleDateFormat("yyyy-MM-dd");
            localDateFormatter.set(fmt);
        }
        String toDateString = fmt.format(new Date(toTimestamp * 1000L));
        throwWithErrorTemplate("DateTo", "{{param}}", alias, "{{to}}", toDateString);
        return null; // 永远不会执行这一句
    }

    protected static String validateDateFromTo(Object value, Long fromTimestamp, Long toTimestamp, String reason, String alias) throws ValidationException {

        boolean isTypeError;

        String dateString;
        if (value instanceof String) {
            dateString = (String) value;
            if (dateString.indexOf('-') != 4) { // 19-01-01这种格式SimpleDateFormat也是能识别的，要排除这种情况
                isTypeError = true;
            } else {
                SimpleDateFormat fmt = localDateFormatter.get();
                if (fmt == null) {
                    fmt = new SimpleDateFormat("yyyy-MM-dd");
                    localDateFormatter.set(fmt);
                }

                try {
                    Date date = fmt.parse(dateString);
                    long timestamp = date.getTime() / 1000L;
                    if (timestamp >= fromTimestamp && timestamp <= toTimestamp)
                        return dateString;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true;
                }
            }
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Date", "{{param}}", alias);

        SimpleDateFormat fmt = localDateFormatter.get();
        if (fmt == null) {
            fmt = new SimpleDateFormat("yyyy-MM-dd");
            localDateFormatter.set(fmt);
        }
        String fromDateString = fmt.format(new Date(fromTimestamp * 1000L));
        String toDateString = fmt.format(new Date(toTimestamp * 1000L));
        throwWithErrorTemplate("DateFromTo", "{{param}}", alias, "{{from}}", fromDateString, "{{to}}", toDateString);
        return null; // 永远不会执行这一句
    }

    protected static String validateDateTime(Object value, String reason, String alias) throws ValidationException {

        if (value instanceof String) {
            String dateString = (String) value;

            if (dateString.indexOf('-') == 4) { // "19-01-01 10:30:33"这种格式SimpleDateFormat也是能识别的，要排除这种情况

                SimpleDateFormat fmt = localDateTimeFormatter.get();
                if (fmt == null) {
                    fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    localDateTimeFormatter.set(fmt);
                }

                try {
                    fmt.parse(dateString);
                    return dateString;
                } catch (Exception e) {
                    //
                }
            }
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("DateTime", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    protected static String validateDateTimeFrom(Object value, Long fromTimestamp, String reason, String alias) throws ValidationException {

        boolean isTypeError;

        String dateString;
        if (value instanceof String) {
            dateString = (String) value;
            if (dateString.indexOf('-') != 4) { // "19-01-01 10:30:33"这种格式SimpleDateFormat也是能识别的，要排除这种情况
                isTypeError = true;
            } else {
                SimpleDateFormat fmt = localDateTimeFormatter.get();
                if (fmt == null) {
                    fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    localDateTimeFormatter.set(fmt);
                }

                try {
                    Date date = fmt.parse(dateString);
                    long timestamp = date.getTime() / 1000L;
                    if (timestamp >= fromTimestamp)
                        return dateString;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true;
                }
            }
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("DateTime", "{{param}}", alias);

        SimpleDateFormat fmt = localDateTimeFormatter.get();
        if (fmt == null) {
            fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            localDateTimeFormatter.set(fmt);
        }
        String fromDateString = fmt.format(new Date(fromTimestamp * 1000L));
        throwWithErrorTemplate("DateTimeFrom", "{{param}}", alias, "{{from}}", fromDateString);
        return null; // 永远不会执行这一句
    }

    protected static String validateDateTimeTo(Object value, Long toTimestamp, String reason, String alias) throws ValidationException {

        boolean isTypeError;

        String dateString;
        if (value instanceof String) {
            dateString = (String) value;
            if (dateString.indexOf('-') != 4) { // "19-01-01 10:30:33"这种格式SimpleDateFormat也是能识别的，要排除这种情况
                isTypeError = true;
            } else {
                SimpleDateFormat fmt = localDateTimeFormatter.get();
                if (fmt == null) {
                    fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    localDateTimeFormatter.set(fmt);
                }

                try {
                    Date date = fmt.parse(dateString);
                    long timestamp = date.getTime() / 1000L;
                    if (timestamp < toTimestamp)
                        return dateString;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true;
                }
            }
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("DateTime", "{{param}}", alias);

        SimpleDateFormat fmt = localDateTimeFormatter.get();
        if (fmt == null) {
            fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            localDateTimeFormatter.set(fmt);
        }
        String toDateString = fmt.format(new Date(toTimestamp * 1000L));
        throwWithErrorTemplate("DateTimeTo", "{{param}}", alias, "{{to}}", toDateString);
        return null; // 永远不会执行这一句
    }

    protected static String validateDateTimeFromTo(Object value, Long fromTimestamp, Long toTimestamp, String reason, String alias) throws ValidationException {

        boolean isTypeError;

        String dateString;
        if (value instanceof String) {
            dateString = (String) value;
            if (dateString.indexOf('-') != 4) { // "19-01-01 10:30:33"这种格式SimpleDateFormat也是能识别的，要排除这种情况
                isTypeError = true;
            } else {
                SimpleDateFormat fmt = localDateTimeFormatter.get();
                if (fmt == null) {
                    fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    localDateTimeFormatter.set(fmt);
                }

                try {
                    Date date = fmt.parse(dateString);
                    long timestamp = date.getTime() / 1000L;
                    if (timestamp >= fromTimestamp && timestamp < toTimestamp)
                        return dateString;
                    isTypeError = false;
                } catch (Exception e) {
                    isTypeError = true;
                }
            }
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("DateTime", "{{param}}", alias);

        SimpleDateFormat fmt = localDateTimeFormatter.get();
        if (fmt == null) {
            fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            localDateTimeFormatter.set(fmt);
        }
        String fromDateString = fmt.format(new Date(fromTimestamp * 1000L));
        String toDateString = fmt.format(new Date(toTimestamp * 1000L));
        throwWithErrorTemplate("DateTimeFromTo", "{{param}}", alias, "{{from}}", fromDateString, "{{to}}", toDateString);
        return null; // 永远不会执行这一句
    }

    // endregion

    // region Arr|List & Map

    public static List<Object> validateArr(Object value, String reason, String alias) throws ValidationException {

        if (value instanceof List) {
            return (List<Object>) value;
        } else if (value instanceof Object[]) {
            ArrayList<Object> list = new ArrayList<Object>();
            Collections.addAll(list, ((Object[]) value));
            return list;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("Arr", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    public static List<Object> validateArrLen(Object value, Integer length, String reason, String alias) throws ValidationException {

        boolean isTypeError;
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            if (list.size() == length)
                return list;
            isTypeError = false;
        } else if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length == length) {
                ArrayList<Object> list = new ArrayList<Object>();
                Collections.addAll(list, arr);
                return list;
            }
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Arr", "{{param}}", alias);
        throwWithErrorTemplate("ArrLen", "{{param}}", alias, "{{length}}", length.toString());
        return null; // 永远不会执行这一句
    }

    public static List<Object> validateArrLenGe(Object value, Integer minLen, String reason, String alias) throws ValidationException {

        boolean isTypeError;
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            if (list.size() >= minLen)
                return list;
            isTypeError = false;
        } else if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length >= minLen) {
                ArrayList<Object> list = new ArrayList<Object>();
                Collections.addAll(list, arr);
                return list;
            }
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Arr", "{{param}}", alias);
        throwWithErrorTemplate("ArrLenGe", "{{param}}", alias, "{{min}}", minLen.toString());
        return null; // 永远不会执行这一句
    }

    public static List<Object> validateArrLenLe(Object value, Integer maxLen, String reason, String alias) throws ValidationException {

        boolean isTypeError;
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            if (list.size() <= maxLen)
                return list;
            isTypeError = false;
        } else if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length <= maxLen) {
                ArrayList<Object> list = new ArrayList<Object>();
                Collections.addAll(list, arr);
                return list;
            }
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Arr", "{{param}}", alias);
        throwWithErrorTemplate("ArrLenLe", "{{param}}", alias, "{{max}}", maxLen.toString());
        return null; // 永远不会执行这一句
    }

    public static List<Object> validateArrLenGeLe(Object value, Integer minLen, Integer maxLen, String reason, String alias) throws ValidationException {

        boolean isTypeError;
        if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            if (list.size() >= minLen && list.size() <= maxLen)
                return list;
            isTypeError = false;
        } else if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            if (arr.length >= minLen && arr.length <= maxLen) {
                ArrayList<Object> list = new ArrayList<Object>();
                Collections.addAll(list, arr);
                return list;
            }
            isTypeError = false;
        } else
            isTypeError = true;

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        if (isTypeError)
            throwWithErrorTemplate("Arr", "{{param}}", alias);
        throwWithErrorTemplate("ArrLenGeLe", "{{param}}", alias, "{{min}}", minLen.toString(), "{{max}}", maxLen.toString());
        return null; // 永远不会执行这一句
    }

    public static Map<String, Object> validateMap(Object value, String reason, String alias) throws ValidationException {

        if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            boolean fail = false;
            for (Object key : map.keySet()) {
                if (!(key instanceof String)) {
                    fail = true;
                    break;
                }
            }
            if (!fail)
                return map;
        }

        throwIfHasReason(reason);

        alias = finalAlias(alias);
        throwWithErrorTemplate("Map", "{{param}}", alias);
        return null; // 永远不会执行这一句
    }

    // endregion

    // region If

    protected static boolean checkIf(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            return str.equalsIgnoreCase("true") ||
                str.equals("1") ||
                str.equalsIgnoreCase("yes") ||
                str.equalsIgnoreCase("y");
        } else if (value instanceof Boolean) {
            return (boolean) (Boolean) value;
        } else if (value instanceof Integer) {
            int v = (Integer) value;
            return v == 1;
        } else if (value instanceof Long) {
            long v = (Long) value;
            return v == 1;
        }
        return false;
    }

    protected static boolean checkIfNot(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            return str.equalsIgnoreCase("false") ||
                str.equals("0") ||
                str.equalsIgnoreCase("no") ||
                str.equalsIgnoreCase("n");
        } else if (value instanceof Boolean) {
            boolean boolVal = (Boolean) value;
            return !boolVal;
        } else if (value instanceof Integer) {
            int v = (Integer) value;
            return v == 0;
        } else if (value instanceof Long) {
            long v = (Long) value;
            return v == 0;
        }
        return false;
    }

    protected static boolean checkIfTrue(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            return str.equalsIgnoreCase("true");
        } else if (value instanceof Boolean) {
            return (boolean) (Boolean) value;
        }
        return false;
    }

    protected static boolean checkIfFalse(Object value) {
        if (value instanceof String) {
            String str = (String) value;
            return str.equalsIgnoreCase("false");
        } else if (value instanceof Boolean) {
            boolean boolVal = (Boolean) value;
            return !boolVal;
        }
        return false;
    }

    protected static boolean checkIfExist(Object value) {
        return value != null;
    }

    protected static boolean checkIfNotExist(Object value) {
        return value == null;
    }

    protected static boolean checkIfIntEq(Object value, Integer equalVal) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    return val == equalVal;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return equalVal.intValue() == (Integer) value;
        }

        return false;
    }

    protected static boolean checkIfIntNe(Object value, Integer equalVal) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    return val != equalVal;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return equalVal.intValue() != (Integer) value;
        }

        return true;
    }

    protected static boolean checkIfIntGt(Object value, Integer min) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    return val > min;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return (Integer) value > min;
        }

        return false;
    }

    protected static boolean checkIfIntGe(Object value, Integer min) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    return val >= min;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return (Integer) value >= min;
        }

        return false;
    }

    protected static boolean checkIfIntLt(Object value, Integer max) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    return val < max;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return (Integer) value < max;
        }

        return false;
    }

    protected static boolean checkIfIntLe(Object value, Integer max) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    int val = Integer.parseInt((String) value);
                    return val <= max;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return (Integer) value <= max;
        }

        return false;
    }

    protected static boolean checkIfIntIn(Object value, List<Integer> values) {
        Integer v;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    v = Integer.parseInt((String) value);
                } catch (Exception e) {
                    return false; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                return false;
        } else if (value instanceof Integer) {
            v = (Integer) value;
        } else
            return false;

        return values.indexOf(v) >= 0;
    }

    protected static boolean checkIfIntNotIn(Object value, List<Integer> values) {
        Integer v;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    v = Integer.parseInt((String) value);
                } catch (Exception e) {
                    return true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                return true;
        } else if (value instanceof Integer) {
            v = (Integer) value;
        } else
            return true;

        return values.indexOf(v) == -1;
    }

    protected static boolean checkIfLongEq(Object value, Long equalVal) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    return val == equalVal;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return equalVal == ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return equalVal == ((Long) value).longValue();
        }

        return false;
    }

    protected static boolean checkIfLongNe(Object value, Long equalVal) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    return val != equalVal;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return equalVal != ((Integer) value).longValue();
        } else if (value instanceof Long) {
            return equalVal != ((Long) value).longValue();
        }

        return true;
    }

    protected static boolean checkIfLongGt(Object value, Long min) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    return val > min;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue() > min;
        } else if (value instanceof Long) {
            return ((Long) value) > min;
        }

        return false;
    }

    protected static boolean checkIfLongGe(Object value, Long min) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    return val >= min;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue() >= min;
        } else if (value instanceof Long) {
            return ((Long) value) >= min;
        }

        return false;
    }

    protected static boolean checkIfLongLt(Object value, Long max) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    return val < max;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue() < max;
        } else if (value instanceof Long) {
            return ((Long) value) < max;
        }

        return false;
    }

    protected static boolean checkIfLongLe(Object value, Long max) {
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    long val = Long.parseLong((String) value);
                    return val <= max;
                } catch (Exception e) {
                    // 实际上是数值溢出, 暂时算作类型错误
                }
            }
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue() <= max;
        } else if (value instanceof Long) {
            return ((Long) value) <= max;
        }

        return false;
    }

    protected static boolean checkIfLongIn(Object value, List<Long> values) {
        Long v;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    v = Long.parseLong((String) value);
                } catch (Exception e) {
                    return false; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                return false;
        } else if (value instanceof Integer) {
            v = ((Integer) value).longValue();
        } else if (value instanceof Long) {
            v = (Long) value;
        } else
            return false;

        return values.indexOf(v) >= 0;
    }

    protected static boolean checkIfLongNotIn(Object value, List<Long> values) {
        Long v;
        if (value instanceof String) {
            if (ValidationUtils.isIntString((String) value)) {
                try {
                    v = Long.parseLong((String) value);
                } catch (Exception e) {
                    return true; // 实际上是数值溢出, 暂时算作类型错误
                }
            } else
                return true;
        } else if (value instanceof Integer) {
            v = ((Integer) value).longValue();
        } else if (value instanceof Long) {
            v = (Long) value;
        } else
            return true;

        return values.indexOf(v) == -1;
    }

    protected static boolean checkIfStrEq(Object value, String string) {
        return string.equals(value);
    }

    protected static boolean checkIfStrNe(Object value, String string) {
        return !string.equals(value);
    }

    protected static boolean checkIfStrGt(Object value, String string) {
        if (value instanceof String)
            return ((String) value).compareTo(string) > 0;
        return false;
    }

    protected static boolean checkIfStrGe(Object value, String string) {
        if (value instanceof String)
            return ((String) value).compareTo(string) >= 0;
        return false;
    }

    protected static boolean checkIfStrLt(Object value, String string) {
        if (value instanceof String)
            return ((String) value).compareTo(string) < 0;
        return false;
    }

    protected static boolean checkIfStrLe(Object value, String string) {
        if (value instanceof String)
            return ((String) value).compareTo(string) <= 0;
        return false;
    }

    protected static boolean checkIfStrIn(Object value, List<String> strings) {
        return strings.indexOf(value) >= 0;
    }

    protected static boolean checkIfStrNotIn(Object value, List<String> strings) {
        return strings.indexOf(value) == -1;
    }

    // endregion

    static private class Validator {
        public int countOfIfs;
        public boolean required;
        public String reason;
        public String alias;
        public List<Object[]> units;

        public Validator() {
        }
    }

    private static class Type {

        static final int Unknown = 0;

        static final int Required = 1;
        static final int Alias = 2;
        static final int Hint = 3;

        // 32位整型
        static final int Int = 100;
        static final int IntEq = 101;
        static final int IntNe = 102;
        static final int IntGt = 103;
        static final int IntGe = 104;
        static final int IntLt = 105;
        static final int IntLe = 106;
        static final int IntGtLt = 107;
        static final int IntGeLe = 108;
        static final int IntGtLe = 109;
        static final int IntGeLt = 110;
        static final int IntIn = 111;
        static final int IntNotIn = 112;

        // 64位整型
        static final int Long = 150;
        static final int LongEq = 151;
        static final int LongNe = 152;
        static final int LongGt = 153;
        static final int LongGe = 154;
        static final int LongLt = 155;
        static final int LongLe = 156;
        static final int LongGtLt = 157;
        static final int LongGeLe = 158;
        static final int LongGtLe = 159;
        static final int LongGeLt = 160;
        static final int LongIn = 161;
        static final int LongNotIn = 162;

        // Float
        static final int Float = 200;
        static final int FloatGt = 201;
        static final int FloatGe = 202;
        static final int FloatLt = 203;
        static final int FloatLe = 204;
        static final int FloatGtLt = 205;
        static final int FloatGeLe = 206;
        static final int FloatGtLe = 207;
        static final int FloatGeLt = 208;

        // Bool
        static final int Bool = 300;
        static final int BoolTrue = 301;
        static final int BoolFalse = 302;
        static final int BoolSmart = 303;
        static final int BoolSmartTrue = 304;
        static final int BoolSmartFalse = 305;

        // String
        static final int Str = 400;
        static final int StrEq = 401;
        static final int StrNe = 402;
        static final int StrEqI = 403;
        static final int StrNeI = 404;
        static final int StrIn = 405;
        static final int StrNotIn = 406;
        static final int StrInI = 407;
        static final int StrNotInI = 408;
        static final int StrLen = 409;
        static final int StrLenGe = 410;
        static final int StrLenLe = 411;
        static final int StrLenGeLe = 412;
        static final int ByteLen = 413;
        static final int ByteLenGe = 414;
        static final int ByteLenLe = 415;
        static final int ByteLenGeLe = 416;
        static final int Letters = 417;
        static final int Alphabet = 418;
        static final int Numbers = 419;
        static final int Digits = 420;
        static final int LettersNumbers = 421;
        static final int Numeric = 422;
        static final int VarName = 423;
        static final int Email = 424;
        static final int Url = 425;
        static final int HttpUrl = 426;
        static final int Ip = 427;
        static final int Ipv4 = 428;
        static final int Ipv6 = 429;
        static final int Mac = 430;
        static final int Regexp = 431;

        // 数组/列表
        static final int List = 500;
        static final int ListLen = 501;
        static final int ListLenGe = 502;
        static final int ListLenLe = 503;
        static final int ListLenGeLe = 504;
        static final int Arr = 505;
        static final int ArrLen = 506;
        static final int ArrLenGe = 507;
        static final int ArrLenLe = 508;
        static final int ArrLenGeLe = 509;

        // Map
        static final int Map = 600;

        // 文件
        static final int File = 700;
        static final int FileMaxSize = 701;
        static final int FileMinSize = 702;
        static final int FileImage = 703;
        static final int FileVideo = 704;
        static final int FileAudio = 705;
        static final int FileMimes = 706;

        // 日期和时间
        static final int Date = 800;
        static final int DateFrom = 801;
        static final int DateTo = 802;
        static final int DateFromTo = 803;
        static final int DateTime = 804;
        static final int DateTimeFrom = 805;
        static final int DateTimeTo = 806;
        static final int DateTimeFromTo = 807;

        // 条件验证器
        static final int If = 900;
        static final int IfNot = 901;
        static final int IfTrue = 902;
        static final int IfFalse = 903;
        static final int IfExist = 904;
        static final int IfNotExist = 905;
        static final int IfIntEq = 906;
        static final int IfIntNe = 907;
        static final int IfIntGt = 908;
        static final int IfIntLt = 909;
        static final int IfIntGe = 910;
        static final int IfIntLe = 911;
        static final int IfIntIn = 912;
        static final int IfIntNotIn = 913;
        static final int IfLongEq = 914;
        static final int IfLongNe = 915;
        static final int IfLongGt = 916;
        static final int IfLongLt = 917;
        static final int IfLongGe = 918;
        static final int IfLongLe = 919;
        static final int IfLongIn = 920;
        static final int IfLongNotIn = 921;
        static final int IfStrEq = 922;
        static final int IfStrNe = 923;
        static final int IfStrGt = 924;
        static final int IfStrLt = 925;
        static final int IfStrGe = 926;
        static final int IfStrLe = 927;
        static final int IfStrIn = 928;
        static final int IfStrNotIn = 929;

        static int fromNameOrThrow(String validatorName) throws ValidationException {
            Integer type = validatorNameToType.get(validatorName);
            if (type == null)
                throw new ValidationException("未知的验证器\"" + validatorName + "\"");
            return type;
        }

        static Map<String, Integer> validatorNameToType = new HashMap<String, Integer>(256) {{

            put("Unknown", Unknown);

            put("Required", Required);
            put("Alias", Alias);
            put(">>>", Hint);

            // 32位整型
            put("Int", Int);
            put("IntEq", IntEq);
            put("IntNe", IntNe);
            put("IntGt", IntGt);
            put("IntGe", IntGe);
            put("IntLt", IntLt);
            put("IntLe", IntLe);
            put("IntGtLt", IntGtLt);
            put("IntGeLe", IntGeLe);
            put("IntGtLe", IntGtLe);
            put("IntGeLt", IntGeLt);
            put("IntIn", IntIn);
            put("IntNotIn", IntNotIn);

            // 64位整型
            put("Long", Long);
            put("LongEq", LongEq);
            put("LongNe", LongNe);
            put("LongGt", LongGt);
            put("LongGe", LongGe);
            put("LongLt", LongLt);
            put("LongLe", LongLe);
            put("LongGtLt", LongGtLt);
            put("LongGeLe", LongGeLe);
            put("LongGtLe", LongGtLe);
            put("LongGeLt", LongGeLt);
            put("LongIn", LongIn);
            put("LongNotIn", LongNotIn);

            // Float
            put("Float", Float);
            put("FloatGt", FloatGt);
            put("FloatGe", FloatGe);
            put("FloatLt", FloatLt);
            put("FloatLe", FloatLe);
            put("FloatGtLt", FloatGtLt);
            put("FloatGeLe", FloatGeLe);
            put("FloatGtLe", FloatGtLe);
            put("FloatGeLt", FloatGeLt);

            // Bool
            put("Bool", Bool);
            put("BoolTrue", BoolTrue);
            put("BoolFalse", BoolFalse);
            put("BoolSmart", BoolSmart);
            put("BoolSmartTrue", BoolSmartTrue);
            put("BoolSmartFalse", BoolSmartFalse);

            // String
            put("Str", Str);
            put("StrEq", StrEq);
            put("StrNe", StrNe);
            put("StrEqI", StrEqI);
            put("StrNeI", StrNeI);
            put("StrIn", StrIn);
            put("StrNotIn", StrNotIn);
            put("StrInI", StrInI);
            put("StrNotInI", StrNotInI);
            put("StrLen", StrLen);
            put("StrLenGe", StrLenGe);
            put("StrLenLe", StrLenLe);
            put("StrLenGeLe", StrLenGeLe);
            put("ByteLen", ByteLen);
            put("ByteLenGe", ByteLenGe);
            put("ByteLenLe", ByteLenLe);
            put("ByteLenGeLe", ByteLenGeLe);
            put("Letters", Letters);
            put("Alphabet", Alphabet);
            put("Numbers", Numbers);
            put("Digits", Digits);
            put("LettersNumbers", LettersNumbers);
            put("Numeric", Numeric);
            put("VarName", VarName);
            put("Email", Email);
            put("Url", Url);
            put("HttpUrl", HttpUrl);
            put("Ip", Ip);
            put("Ipv4", Ipv4);
            put("Ipv6", Ipv6);
            put("Mac", Mac);
            put("Regexp", Regexp);

            // 数组/列表
            put("List", List);
            put("ListLen", ListLen);
            put("ListLenGe", ListLenGe);
            put("ListLenLe", ListLenLe);
            put("ListLenGeLe", ListLenGeLe);
            put("Arr", Arr);
            put("ArrLen", ArrLen);
            put("ArrLenGe", ArrLenGe);
            put("ArrLenLe", ArrLenLe);
            put("ArrLenGeLe", ArrLenGeLe);

            // Map
            put("Map", Map);

            // 文件
            put("File", File);
            put("FileMaxSize", FileMaxSize);
            put("FileMinSize", FileMinSize);
            put("FileImage", FileImage);
            put("FileVideo", FileVideo);
            put("FileAudio", FileAudio);
            put("FileMimes", FileMimes);

            // 日期和时间
            put("Date", Date);
            put("DateFrom", DateFrom);
            put("DateTo", DateTo);
            put("DateFromTo", DateFromTo);
            put("DateTime", DateTime);
            put("DateTimeFrom", DateTimeFrom);
            put("DateTimeTo", DateTimeTo);
            put("DateTimeFromTo", DateTimeFromTo);

            // 条件验证器
            put("If", If);
            put("IfNot", IfNot);
            put("IfTrue", IfTrue);
            put("IfFalse", IfFalse);
            put("IfExist", IfExist);
            put("IfNotExist", IfNotExist);
            put("IfIntEq", IfIntEq);
            put("IfIntNe", IfIntNe);
            put("IfIntGt", IfIntGt);
            put("IfIntLt", IfIntLt);
            put("IfIntGe", IfIntGe);
            put("IfIntLe", IfIntLe);
            put("IfIntIn", IfIntIn);
            put("IfIntNotIn", IfIntNotIn);
            put("IfLongEq", IfLongEq);
            put("IfLongNe", IfLongNe);
            put("IfLongGt", IfLongGt);
            put("IfLongLt", IfLongLt);
            put("IfLongGe", IfLongGe);
            put("IfLongLe", IfLongLe);
            put("IfLongIn", IfLongIn);
            put("IfLongNotIn", IfLongNotIn);
            put("IfStrEq", IfStrEq);
            put("IfStrNe", IfStrNe);
            put("IfStrGt", IfStrGt);
            put("IfStrLt", IfStrLt);
            put("IfStrGe", IfStrGe);
            put("IfStrLe", IfStrLe);
            put("IfStrIn", IfStrIn);
            put("IfStrNotIn", IfStrNotIn);
        }};
    }
}
