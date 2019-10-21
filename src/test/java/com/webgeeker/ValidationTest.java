package com.webgeeker;

import com.webgeeker.validation.Validation;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Unit test for simple App.
 */
public class ValidationTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ValidationTest(String testName )
    {
        super( testName );

        // 预热
        try {
            testValidateRequired();
            testValidateInt();
            testValidateStr();
            testValidateStrLen();
            testValidateStrOther();
            testValidateRegexp();
            testValidateBool();
            testValidateFloat();
            testValidateList();
            testValidateArr();
            testValidateMap();
            testValidateCustomHint();
            testValidateCompile();
        } catch (Exception e) {
            //
        }
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( ValidationTest.class );
    }

    // 因为 Runnable 的 run() 方法不能抛出异常, 所以自定义一个MyRunnable
    interface MyRunnable {
        void run() throws Exception;
    }

    private void _assertThrowExpectionContainErrorString(MyRunnable runnable, String containedErrorString) throws Exception {
        try {
            runnable.run();
        } catch (Exception e) {
            if (e.getMessage().contains(containedErrorString))
                return;
            throw new Exception("抛出异常的描述中应该包含:\n    " + containedErrorString + "\n实际的错误描述为:\n    " + e.getMessage(), e);
        }
        throw new Exception("应该抛出异常\"" + containedErrorString + "\"");
    }

    private static String _implode(String[] strings, String separator) {
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

    public void testValidateRequired() throws Exception {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("name", "WebGeeker");

        Validation.validate(params, new String[]{"name", "Required", null});
        Validation.validate(params, new String[]{"name", "Required|Alias:姓名", null});
        Validation.validate(params, new String[]{"name", "Required|>>>:必须提供姓名", null});
        Validation.validate(params, new String[]{"name", "Required|StrLenGeLe:1,20", null});
        Validation.validate(params, new String[]{"name", "Required|StrLenGeLe:1,20|Alias:姓名", null});
        Validation.validate(params, new String[]{"name", "Required|StrLenGeLe:1,20|>>>:必须提供姓名", null});

        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"phone", "Required", null});
        }, "必须提供“phone”");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"phone", "Required|Alias:电话", null});
        }, "必须提供“电话”");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"phone", "Required|>>>:必须提供电话", null});
        }, "必须提供电话");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"phone", "Required|StrLen:11", null});
        }, "必须提供“phone”");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"phone", "Required|StrLen:11|Alias:电话", null});
        }, "必须提供“电话”");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"phone", "Required|StrLen:11|>>>:必须提供电话", null});
        }, "必须提供电话");

    }

    public void testValidateInt() throws Exception {
        HashMap<String, Object> params = new HashMap<>();

        // Int
        params.put("varInt", "-1");
        Validation.validate(params, new String[]{"varInt", "Int", null});
        params.put("varInt", "0");
        Validation.validate(params, new String[]{"varInt", "Int", null});
        params.put("varInt", "1");
        Validation.validate(params, new String[]{"varInt", "Int", null});
        params.put("varInt", -1);
        Validation.validate(params, new String[]{"varInt", "Int", null});
        params.put("varInt", 0);
        Validation.validate(params, new String[]{"varInt", "Int", null});
        params.put("varInt", 1);
        Validation.validate(params, new String[]{"varInt", "Int", null});
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", true);
            Validation.validate(params, new String[]{"varInt", "Int", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", new ArrayList<>());
            Validation.validate(params, new String[]{"varInt", "Int", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", new HashMap<String, Object>());
            Validation.validate(params, new String[]{"varInt", "Int", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", new Integer[0]);
            Validation.validate(params, new String[]{"varInt", "Int", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", 0.0);
            Validation.validate(params, new String[]{"varInt", "Int", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "Int", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "");
            Validation.validate(params, new String[]{"varInt", "Int", null});
        }, "必须是整数");

        // IntEq
        params.put("varInt", "-1");
        Validation.validate(params, new String[]{"varInt", "IntEq:-1", null});
        params.put("varInt", -1);
        Validation.validate(params, new String[]{"varInt", "IntEq:-1", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntEq:-1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", true);
            Validation.validate(params, new String[]{"varInt", "IntEq:-1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "0");
            Validation.validate(params, new String[]{"varInt", "IntEq:-1", null});
        }, "必须等于 -1");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", 0);
            Validation.validate(params, new String[]{"varInt", "IntEq:-1", null});
        }, "必须等于 -1");

        // IntGt
        params.put("varInt", "1");
        Validation.validate(params, new String[]{"varInt", "IntGt:0", null});
        params.put("varInt", 1);
        Validation.validate(params, new String[]{"varInt", "IntGt:0", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntGt:0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", 1.0);
            Validation.validate(params, new String[]{"varInt", "IntGt:0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "0");
            Validation.validate(params, new String[]{"varInt", "IntGt:0", null});
        }, "必须大于 0");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", 0);
            Validation.validate(params, new String[]{"varInt", "IntGt:0", null});
        }, "必须大于 0");

        // IntGe
        params.put("varInt", "1");
        Validation.validate(params, new String[]{"varInt", "IntGe:0", null});
        params.put("varInt", 1);
        Validation.validate(params, new String[]{"varInt", "IntGe:0", null});
        params.put("varInt", "0");
        Validation.validate(params, new String[]{"varInt", "IntGe:0", null});
        params.put("varInt", 0);
        Validation.validate(params, new String[]{"varInt", "IntGe:0", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntGe:0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", new String[0]);
            Validation.validate(params, new String[]{"varInt", "IntGe:0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "-1");
            Validation.validate(params, new String[]{"varInt", "IntGe:0", null});
        }, "必须大于等于 0");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", -1);
            Validation.validate(params, new String[]{"varInt", "IntGe:0", null});
        }, "必须大于等于 0");

        // IntLt
        params.put("varInt", "-1");
        Validation.validate(params, new String[]{"varInt", "IntLt:0", null});
        params.put("varInt", -1);
        Validation.validate(params, new String[]{"varInt", "IntLt:0", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntLt:0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", new ArrayList<Integer>());
            Validation.validate(params, new String[]{"varInt", "IntLt:0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "0");
            Validation.validate(params, new String[]{"varInt", "IntLt:0", null});
        }, "必须小于 0");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", 0);
            Validation.validate(params, new String[]{"varInt", "IntLt:0", null});
        }, "必须小于 0");

        // IntLe
        params.put("varInt", "-1");
        Validation.validate(params, new String[]{"varInt", "IntLe:0", null});
        params.put("varInt", -1);
        Validation.validate(params, new String[]{"varInt", "IntLe:0", null});
        params.put("varInt", "0");
        Validation.validate(params, new String[]{"varInt", "IntLe:0", null});
        params.put("varInt", 0);
        Validation.validate(params, new String[]{"varInt", "IntLe:0", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", new HashMap<String, Integer>());
            Validation.validate(params, new String[]{"varInt", "IntLe:0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", false);
            Validation.validate(params, new String[]{"varInt", "IntLe:0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "1");
            Validation.validate(params, new String[]{"varInt", "IntLe:0", null});
        }, "必须小于等于 0");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", 1);
            Validation.validate(params, new String[]{"varInt", "IntLe:0", null});
        }, "必须小于等于 0");

        // IntGeLe
        params.put("varInt", "0");
        Validation.validate(params, new String[]{"varInt", "IntGeLe:0,0", null});
        params.put("varInt", 0);
        Validation.validate(params, new String[]{"varInt", "IntGeLe:0,0", null});
        params.put("varInt", "11");
        Validation.validate(params, new String[]{"varInt", "IntGeLe:-100,100", null});
        params.put("varInt", 11);
        Validation.validate(params, new String[]{"varInt", "IntGeLe:-100,100", null});
        params.put("varInt", "0123");
        Validation.validate(params, new String[]{"varInt", "IntGeLe:123,123", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntGeLe:0,0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", 1.0);
            Validation.validate(params, new String[]{"varInt", "IntGeLe:0,0", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "-1");
            Validation.validate(params, new String[]{"varInt", "IntGeLe:0,10", null});
        }, "必须大于等于 0 小于等于 10");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", -1);
            Validation.validate(params, new String[]{"varInt", "IntGeLe:0,10", null});
        }, "必须大于等于 0 小于等于 10");

        // IntGtLt
        params.put("varInt", "0");
        Validation.validate(params, new String[]{"varInt", "IntGtLt:-1,1", null});
        params.put("varInt", 0);
        Validation.validate(params, new String[]{"varInt", "IntGtLt:-1,1", null});
        params.put("varInt", "000");
        Validation.validate(params, new String[]{"varInt", "IntGtLt:-1,1", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntGtLt:-1,1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", 1.0);
            Validation.validate(params, new String[]{"varInt", "IntGtLt:-1,1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "-1");
            Validation.validate(params, new String[]{"varInt", "IntGtLt:-1,1", null});
        }, "必须大于 -1 小于 1");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", 1);
            Validation.validate(params, new String[]{"varInt", "IntGtLt:-1,1", null});
        }, "必须大于 -1 小于 1");

        // IntGtLe
        params.put("varInt", "0");
        Validation.validate(params, new String[]{"varInt", "IntGtLe:-1,1", null});
        params.put("varInt", 0);
        Validation.validate(params, new String[]{"varInt", "IntGtLe:-1,1", null});
        params.put("varInt", "1");
        Validation.validate(params, new String[]{"varInt", "IntGtLe:-1,1", null});
        params.put("varInt", 1);
        Validation.validate(params, new String[]{"varInt", "IntGtLe:-1,1", null});
        params.put("varInt", "001");
        Validation.validate(params, new String[]{"varInt", "IntGtLe:-1,1", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntGtLe:-1,1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", 1.0);
            Validation.validate(params, new String[]{"varInt", "IntGtLe:-1,1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "-1");
            Validation.validate(params, new String[]{"varInt", "IntGtLe:-1,1", null});
        }, "必须大于 -1 小于等于 1");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", 2);
            Validation.validate(params, new String[]{"varInt", "IntGtLe:-1,1", null});
        }, "必须大于 -1 小于等于 1");

        // IntGeLt
        params.put("varInt", "0");
        Validation.validate(params, new String[]{"varInt", "IntGeLt:-1,1", null});
        params.put("varInt", 0);
        Validation.validate(params, new String[]{"varInt", "IntGeLt:-1,1", null});
        params.put("varInt", "-1");
        Validation.validate(params, new String[]{"varInt", "IntGeLt:-1,1", null});
        params.put("varInt", -1);
        Validation.validate(params, new String[]{"varInt", "IntGeLt:-1,1", null});
        params.put("varInt", "-001");
        Validation.validate(params, new String[]{"varInt", "IntGeLt:-1,1", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntGeLt:-1,1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", -1.0);
            Validation.validate(params, new String[]{"varInt", "IntGeLt:-1,1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "-2");
            Validation.validate(params, new String[]{"varInt", "IntGeLt:-1,1", null});
        }, "必须大于等于 -1 小于 1");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", 1);
            Validation.validate(params, new String[]{"varInt", "IntGeLt:-1,1", null});
        }, "必须大于等于 -1 小于 1");

        // IntIn
        params.put("varInt", "1");
        Validation.validate(params, new String[]{"varInt", "IntIn:1,2,-3", null});
        params.put("varInt", 1);
        Validation.validate(params, new String[]{"varInt", "IntIn:1,2,-3", null});
        params.put("varInt", "02");
        Validation.validate(params, new String[]{"varInt", "IntIn:1,2,-3", null});
        params.put("varInt", 2);
        Validation.validate(params, new String[]{"varInt", "IntIn:1,2,-3", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntIn:1,2,-3", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", -1.0);
            Validation.validate(params, new String[]{"varInt", "IntIn:1,2,-3", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "0");
            Validation.validate(params, new String[]{"varInt", "IntIn:1,2,-3", null});
        }, "只能取这些值: 1, 2, -3");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", 5);
            Validation.validate(params, new String[]{"varInt", "IntIn:1,2,-3", null});
        }, "只能取这些值: 1, 2, -3");

        // IntNotIn
        params.put("varInt", "0");
        Validation.validate(params, new String[]{"varInt", "IntNotIn:1,2,-3", null});
        params.put("varInt", 0);
        Validation.validate(params, new String[]{"varInt", "IntNotIn:1,2,-3", null});
        params.put("varInt", "04");
        Validation.validate(params, new String[]{"varInt", "IntNotIn:1,2,-3", null});
        params.put("varInt", 4);
        Validation.validate(params, new String[]{"varInt", "IntNotIn:1,2,-3", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntNotIn:1,2,-3", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", -1.0);
            Validation.validate(params, new String[]{"varInt", "IntNotIn:1,2,-3", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "1");
            Validation.validate(params, new String[]{"varInt", "IntNotIn:1,2,-3", null});
        }, "不能取这些值: 1, 2, -3");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", -3);
            Validation.validate(params, new String[]{"varInt", "IntNotIn:1,2,-3", null});
        }, "不能取这些值: 1, 2, -3");

    }

    public void testValidateStr() throws Exception {
        HashMap<String, Object> params = new HashMap<>();

        // Str
        String[] strVals = new String[]{"", "123", "abc", "你好", "-12311112311111"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Str", null});
        }
        Object[] notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Str", null});
            }, "必须是字符串");
        }

        // StrEq
        strVals = new String[]{"", "123", "abc", "你好", "-12311112311111"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrEq:" + strVal, null});
        }
        for (Object strVal : strVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrEq:" + strVal + "1", null});
            }, "必须等于");
        }
        for (Object strVal : strVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal + "1");
                Validation.validate(params, new String[]{"varStr", "StrEq:" + strVal, null});
            }, "必须等于");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrEq:" + notStrVal, null});
            }, "必须是字符串");
        }

        // StrEqI
        strVals = new String[]{"", "123", "abc", "你好", "-12311112311111", "Abc", "你a好"};
        String[] str2Vals = new String[]{"", "123", "abc", "你好", "-12311112311111", "abC", "你A好"};
        for (int i = 0; i < strVals.length; i++) {
            String strVal = strVals[i];
            String str2Val = str2Vals[i];
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrEqI:" + str2Val, null});
        }
        for (Object strVal : strVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrEqI:" + strVal + "1", null});
            }, "必须等于\"");
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal + "1");
                Validation.validate(params, new String[]{"varStr", "StrEqI:" + strVal, null});
            }, "\"（忽略大小写）");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrEqI:" + notStrVal, null});
            }, "必须是字符串");
        }

        // StrNe
        strVals = new String[]{"", "123", "abc", "你好", "-12311112311111"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrNe:" + strVal + "1", null});
        }
        for (Object strVal : strVals) {
            params.put("varStr", strVal + "1");
            Validation.validate(params, new String[]{"varStr", "StrNe:" + strVal, null});
        }
        for (Object strVal : strVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrNe:" + strVal, null});
            }, "不能等于");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrNe:" + notStrVal, null});
            }, "必须是字符串");
        }

        // StrNeI
        strVals = new String[]{"", "123", "abc", "你好", "-12311112311111", "Abc", "你a好"};
        str2Vals = new String[]{"", "123", "abc", "你好", "-12311112311111", "abC", "你A好"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrNeI:" + strVal + "1", null});
        }
        for (Object strVal : strVals) {
            params.put("varStr", strVal + "1");
            Validation.validate(params, new String[]{"varStr", "StrNeI:" + strVal, null});
        }
        for (int i = 0; i < strVals.length; i++) {
            String strVal = strVals[i];
            String str2Val = str2Vals[i];
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrNeI:" + str2Val, null});
            }, "不能等于\"");
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrNeI:" + str2Val, null});
            }, "\"（忽略大小写）");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrNeI:" + notStrVal, null});
            }, "必须是字符串");
        }

        // StrIn
        strVals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrIn:" + _implode(strVals, ","), null});
        }
        for (Object strVal : strVals) {
            String listString = _implode(strVals, ",");
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal + "1");
                Validation.validate(params, new String[]{"varStr", "StrIn:" + listString, null});
            }, "只能取这些值:");
        }
        params.put("varStr", "abc");
        Validation.validate(params, new String[]{"varStr", "StrIn:abc", null});
        params.put("varStr", "");
        Validation.validate(params, new String[]{"varStr", "StrIn:", null});
        params.put("varStr", " ");
        Validation.validate(params, new String[]{"varStr", "StrIn: ", null});
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", "abcd");
            Validation.validate(params, new String[]{"varStr", "StrIn:abc", null});
        }, "只能取这些值: \"abc\"");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", " ");
            Validation.validate(params, new String[]{"varStr", "StrIn:  ", null});
        }, "只能取这些值: \"  \"");
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrIn:Hello", null});
            }, "必须是字符串");
        }

        // StrInI
        strVals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111", "Abcd", "你a好"};
        str2Vals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111", "abCd", "你A好"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrInI:" + _implode(str2Vals, ","), null});
        }
        for (Object strVal : strVals) {
            String listString = _implode(strVals, ",");
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal + "1");
                Validation.validate(params, new String[]{"varStr", "StrInI:" + listString, null});
            }, "只能取这些值:");
            String listString2 = _implode(strVals, "1,") + "1";
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrInI:" + listString2, null});
            }, "（忽略大小写）");
        }
        params.put("varStr", "abc");
        Validation.validate(params, new String[]{"varStr", "StrInI:Abc", null});
        params.put("varStr", "");
        Validation.validate(params, new String[]{"varStr", "StrInI:", null});
        params.put("varStr", " ");
        Validation.validate(params, new String[]{"varStr", "StrInI: ", null});
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", "abcd");
            Validation.validate(params, new String[]{"varStr", "StrInI:abc", null});
        }, "只能取这些值: \"abc\"");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", " ");
            Validation.validate(params, new String[]{"varStr", "StrInI:  ", null});
        }, "只能取这些值: \"  \"");
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrInI:Hello", null});
            }, "必须是字符串");
        }

        // StrNotIn
        strVals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111"};
        for (String strVal : strVals) {
            params.put("varStr", strVal + "postfix");
            Validation.validate(params, new String[]{"varStr", "StrNotIn:" + _implode(strVals, ","), null});
        }
        for (Object strVal : strVals) {
            String listString = _implode(strVals, ",");
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrNotIn:" + listString, null});
            }, "不能取这些值:");
        }
        params.put("varStr", "abcd");
        Validation.validate(params, new String[]{"varStr", "StrNotIn:abc", null});
        params.put("varStr", "abc");
        Validation.validate(params, new String[]{"varStr", "StrNotIn:", null});
        params.put("varStr", "");
        Validation.validate(params, new String[]{"varStr", "StrNotIn:abc", null});
        params.put("varStr", "  ");
        Validation.validate(params, new String[]{"varStr", "StrNotIn: ", null});
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", "abc");
            Validation.validate(params, new String[]{"varStr", "StrNotIn:abc", null});
        }, "不能取这些值: \"abc\"");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", "");
            Validation.validate(params, new String[]{"varStr", "StrNotIn:", null});
        }, "不能取这些值: \"\"");
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrNotIn:Hello", null});
            }, "必须是字符串");
        }

        // StrNotInI
        strVals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111", "Abcd", "你a好"};
        str2Vals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111", "abCd", "你A好"};
        for (String strVal : strVals) {
            params.put("varStr", strVal + "postfix");
            Validation.validate(params, new String[]{"varStr", "StrNotInI:" + _implode(str2Vals, ","), null});
        }
        for (Object strVal : strVals) {
            String listString = _implode(str2Vals, ",");
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrNotInI:" + listString, null});
            }, "不能取这些值:");
        }
        params.put("varStr", "abcd");
        Validation.validate(params, new String[]{"varStr", "StrNotInI:abc", null});
        params.put("varStr", "abc");
        Validation.validate(params, new String[]{"varStr", "StrNotInI:", null});
        params.put("varStr", "");
        Validation.validate(params, new String[]{"varStr", "StrNotInI:abc", null});
        params.put("varStr", "  ");
        Validation.validate(params, new String[]{"varStr", "StrNotInI: ", null});
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", "abc");
            Validation.validate(params, new String[]{"varStr", "StrNotInI:Abc", null});
        }, "不能取这些值: \"Abc\"");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", "abc");
            Validation.validate(params, new String[]{"varStr", "StrNotInI:aBc", null});
        }, "（忽略大小写）");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", "");
            Validation.validate(params, new String[]{"varStr", "StrNotInI:", null});
        }, "不能取这些值: \"\"");
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrNotInI:Hello", null});
            }, "必须是字符串");
        }

    }

    public void testValidateStrLen() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        String[] strVals;
        Object[] notStrVals;

        // StrLen
        strVals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111", "Abcd", "你a好"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrLen:" + strVal.length(), null});
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrLen:" + (strVal.length() + 1), null});
            }, "长度必须等于");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrLen:8", null});
            }, "必须是字符串");
        }

        // StrLenGe
        strVals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111", "Abcd", "你a好"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrLenGe:" + strVal.length(), null});
            params.put("varStr", strVal + "1");
            Validation.validate(params, new String[]{"varStr", "StrLenGe:" + strVal.length(), null});
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "StrLenGe:" + (strVal.length() + 1), null});
            }, "长度必须大于等于");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrLenGe:8", null});
            }, "必须是字符串");
        }

        // StrLenLe
        strVals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111", "Abcd", "你a好"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrLenLe:" + strVal.length(), null});
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrLenLe:" + (strVal.length() + 1), null});
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal + "1");
                Validation.validate(params, new String[]{"varStr", "StrLenLe:" + strVal.length(), null});
            }, "长度必须小于等于");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrLenLe:8", null});
            }, "必须是字符串");
        }

        // StrLenGeLe
        strVals = new String[]{"", " ", "  ", "\t", "123", "abc", "你好", "-12311112311111", "Abcd", "你a好"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "StrLenGeLe:" + strVal.length() + "," + strVal.length(), null});
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal + "1");
                Validation.validate(params, new String[]{"varStr", "StrLenGeLe:" + strVal.length() + "," + strVal.length(), null});
            }, "长度必须在");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "StrLenGeLe:8,8", null});
            }, "必须是字符串");
        }

    }

    public void testValidateStrOther() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        String[] strVals;
        String[] wrongVals;
        Object[] notStrVals;

        // Letters
        strVals = new String[]{"a", "z", "A", "Z", "abc", "ABC", "Hello", "ZZZ", "abc"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Letters", null});
        }
        wrongVals = new String[]{"", " ", "  ", "\t", "123", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Letters", null});
            }, "只能包含字母");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Letters", null});
            }, "必须是字符串");
        }

        // Alphabet
        strVals = new String[]{"a", "z", "A", "Z", "abc", "ABC", "Hello", "ZZZ", "abc"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Alphabet", null});
        }
        wrongVals = new String[]{"", " ", "  ", "\t", "123", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Alphabet", null});
            }, "只能包含字母");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Alphabet", null});
            }, "必须是字符串");
        }

        // Numbers
        strVals = new String[]{"0", "1", "123", "32456236234523452354324"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Numbers", null});
        }
        wrongVals = new String[]{"", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Numbers", null});
            }, "只能是纯数字");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Numbers", null});
            }, "必须是字符串");
        }

        // Digits
        strVals = new String[]{"0", "1", "123", "32456236234523452354324"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Digits", null});
        }
        wrongVals = new String[]{"", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Digits", null});
            }, "只能是纯数字");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Digits", null});
            }, "必须是字符串");
        }

        // LettersNumbers
        strVals = new String[]{"a", "z", "A", "Z", "abc", "ABC", "Hello", "ZZZ", "abc", "0", "1", "123", "32456236234523452354324", "abc123"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "LettersNumbers", null});
        }
        wrongVals = new String[]{"_abc123", "", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "LettersNumbers", null});
            }, "只能包含字母和数字");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "LettersNumbers", null});
            }, "必须是字符串");
        }

        // Numeric
        strVals = new String[]{"0", "-0", "0.0", "-0.0", "1", "-1", "1.0", "-1.0", "123", "-123", "123.0", "-123.0", "-.0", ".0", "1.", "-1.", "23412341234.423412341241234", "3245623623452341234234123452354324", ".3245623623452341234234123452354324", "3245623623452341234234123452354324.3245623623452341234234123452354324"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Numeric", null});
        }
        wrongVals = new String[]{"1.2.3", ".", "-.", "", " ", "  ", "\t", " 123", "abc.def", "你好", "abc123"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Numeric", null});
            }, "必须是数值");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Numeric", null});
            }, "必须是字符串");
        }

        // VarName
        strVals = new String[]{"_", "_abc", "_abc123", "a", "z", "A", "Z", "abc", "ABC", "Hello", "ZZZ", "abc", "abc123"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "VarName", null});
        }
        wrongVals = new String[]{"0", "1", "123", "32456236234523452354324", "1abc", "", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "VarName", null});
            }, "只能包含字母、数字和下划线，并且以字母或下划线开头");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "VarName", null});
            }, "必须是字符串");
        }

        // Email
        strVals = new String[]{"hi@abc.com", "admin@webgeeker.com", "32456236234523452354324@webgeeker.com", "hello@abc-def.com"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Email", null});
        }
        wrongVals = new String[]{"hi @abc.com", "0", "1", "123", "32456236234523452354324", "1abc", "", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        int x = 0;
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Email", null});
            }, "不是合法的email");
            x++;
            if (x == 5)
                break;
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Email", null});
            }, "必须是字符串");
        }

        // Url
        strVals = new String[]{"http://abc.com", "https://webgeeker.com", "http://hello.com/p/1", "http://hello.com/p/1?str=1&abc=123", "ftp://abc.com"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Url", null});
        }
        wrongVals = new String[]{"abc.com", "//abc.com", "hi @abc.com", "0", "1", "123", "32456236234523452354324", "1abc", "", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Url", null});
            }, "不是合法的Url地址");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Url", null});
            }, "必须是字符串");
        }

        // HttpUrl
        strVals = new String[]{"http://abc.com", "https://webgeeker.com", "http://hello.com/p/1", "http://hello.com/p/1?str=1&abc=123"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "HttpUrl", null});
        }
        wrongVals = new String[]{"abc.com", "ftp://abc.com", "//abc.com", "hi @abc.com", "0", "1", "123", "32456236234523452354324", "1abc", "", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "HttpUrl", null});
            }, "不是合法的Http地址");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "HttpUrl", null});
            }, "必须是字符串");
        }

        // Ip
        strVals = new String[]{"1.1.1.1", "0.0.0.0", "8.8.8.8", "255.255.255.255",
            "::",
            "::1", // 本地回环地址.相当于ipv4的127.0.0.1
            "::ffff:192.168.89.9", // ipv4的ipv6形式（IPv4映射地址）
            "::ffff:c0a8:5909", // 等价于::ffff:192.168.89.9
            "fe80::", //fe80::/10－这些链路本地地址指明，这些地址只在区域连接中是合法的，这有点类似于IPv4中的169.254.0.0/16
            "169.254.0.0",
            "2001:0DB8:02de:0000:0000:0000:0000:0e13",
            "2001:DB8:2de:0000:0000:0000:0000:e13",
            "2001:DB8:2de:000:000:000:000:e13",
            "2001:DB8:2de:00:00:00:00:e13",
            "2001:DB8:2de:0:0:0:0:e13",
            "2001:DB8:2de::e13",
            "2001:0DB8:0000:0000:0000:0000:1428:57ab",
            "2001:0DB8:0000:0000:0000::1428:57ab",
            "2001:0DB8:0:0:0:0:1428:57ab",
            "2001:0DB8:0::0:1428:57ab",
            "2001:0DB8::1428:57ab",
        };
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Ip", null});
        }
        wrongVals = new String[]{"1.2.3.", "1.2.3.256", "2001::25de::cade", ":::",
            "abc.com", "//abc.com", "hi @abc.com", "0", "1", "123", "32456236234523452354324", "1abc", "", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Ip", null});
            }, "不是合法的IP地址");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Ip", null});
            }, "必须是字符串");
        }

        // Ipv4
        strVals = new String[]{"1.1.1.1", "0.0.0.0", "8.8.8.8", "255.255.255.255", "169.254.0.0"};
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Ipv4", null});
        }
        wrongVals = new String[]{"1.2.3.", "1.2.3.256", "2001::25de::cade", ":::", "2001:0DB8:02de:0000:0000:0000:0000:0e13",
            "abc.com", "//abc.com", "hi @abc.com", "0", "1", "123", "32456236234523452354324", "1abc", "", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Ipv4", null});
            }, "不是合法的IPv4地址");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Ipv4", null});
            }, "必须是字符串");
        }

        // Ipv6
        strVals = new String[]{
            "::",
            "::1", // 本地回环地址.相当于ipv4的127.0.0.1
            "::ffff:192.168.89.9", // ipv4的ipv6形式（IPv4映射地址）
            "::ffff:c0a8:5909", // 等价于::ffff:192.168.89.9
            "fe80::", //fe80::/10－这些链路本地地址指明，这些地址只在区域连接中是合法的，这有点类似于IPv4中的169.254.0.0/16
            "2001:0DB8:02de:0000:0000:0000:0000:0e13",
            "2001:DB8:2de:0000:0000:0000:0000:e13",
            "2001:DB8:2de:000:000:000:000:e13",
            "2001:DB8:2de:00:00:00:00:e13",
            "2001:DB8:2de:0:0:0:0:e13",
            "2001:DB8:2de::e13",
            "2001:0DB8:0000:0000:0000:0000:1428:57ab",
            "2001:0DB8:0000:0000:0000::1428:57ab",
            "2001:0DB8:0:0:0:0:1428:57ab",
            "2001:0DB8:0::0:1428:57ab",
            "2001:0DB8::1428:57ab",
        };
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Ipv6", null});
        }
        wrongVals = new String[]{"1.2.3.", "1.2.3.256", "2001::25de::cade", ":::", "169.254.0.0",
            "abc.com", "//abc.com", "hi @abc.com", "0", "1", "123", "32456236234523452354324", "1abc", "", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Ipv6", null});
            }, "不是合法的IPv6地址");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Ipv6", null});
            }, "必须是字符串");
        }

        // Mac
        strVals = new String[]{
            "00:16:3e:02:02:9c",
            "00:0A:02:0B:03:0C",
            "ff:ff:ff:ff:ff:ff", // 广播地址
            "01:00:00:00:00:00", // 01:xx:xx:xx:xx:xx是多播地址
            "01:00:5e:00:00:00", // 01:00:5e:xx:xx:xx是IPv4多播地址
        };
        for (String strVal : strVals) {
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Mac", null});
        }
        wrongVals = new String[]{"00:16:3e:02:02:9", "1.2.3.", "1.2.3.256", "2001::25de::cade", ":::",
            "abc.com", "//abc.com", "hi @abc.com", "0", "1", "123", "32456236234523452354324", "1abc", "", " ", "  ", "\t", " 123", "-123", "1.0", "abc.def", "你好", "-12311112311111"};
        for (String wrongVal : wrongVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", wrongVal);
                Validation.validate(params, new String[]{"varStr", "Mac", null});
            }, "不是合法的MAC地址");
        }
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Mac", null});
            }, "必须是字符串");
        }

    }

    public void testValidateRegexp() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        HashMap<String, String> valExps;
        String[] wrongVals;
        Object[] notStrVals;

        // Letters
        valExps = new HashMap<>();
//        valExps.put("0123456789", "/345/"); // 在PHP中这个是匹配的, 但是Java中这个不匹配.
        valExps.put("01234567", "/.*345.*/");
        valExps.put("012345678", "/^.*345.*$/");
        valExps.put("0123456789", "/0123456789/");
        valExps.put("10.", "/^[0-9.]+$/");
        valExps.put("10/ab|cd", "/^[0-9]+\\/ab\\|cd$/");
        valExps.put("var=123", "/^\\s*var\\s*=\\s*[0-9]+\\s*$/");
        valExps.put(" var = 123 ", "/^\\s*var\\s*=\\s*[0-9]+\\s*$/");
        for (String strVal : valExps.keySet()) {
            String exp = valExps.get(strVal);
            params.put("varStr", strVal);
            Validation.validate(params, new String[]{"varStr", "Regexp:" + exp, null});
        }
        valExps.clear();

        HashMap<String, String> notMatchValExps = new HashMap<>();
        notMatchValExps.put("a10.", "/^[0-9.]+$/");
        notMatchValExps.put("a10/abcd", "/^[0-9]+\\/abcd$/");
        for (String strVal : notMatchValExps.keySet()) {
            String exp = notMatchValExps.get(strVal);
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", strVal);
                Validation.validate(params, new String[]{"varStr", "Regexp:" + exp, null});
            }, "不匹配正则表达式");
        }
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", "abc");
            Validation.validate(params, new String[]{"varStr", "Regexp:/abc", null});
        }, "正则表达式验证器Regexp格式错误");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varStr", "abc");
            Validation.validate(params, new String[]{"varStr", "Regexp:abc/", null});
        }, "正则表达式验证器Regexp格式错误");
        notStrVals = new Object[]{1, 0, 1.0, 0.0, true, false, new String[0], new HashMap<String, String>()};
        for (Object notStrVal : notStrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varStr", notStrVal);
                Validation.validate(params, new String[]{"varStr", "Regexp:/abc/", null});
            }, "必须是字符串");
        }

    }

    public void testValidateBool() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] boolVals;
        Object[] notBoolVals;

        // Bool
        boolVals = new Object[]{true, false, "true", "false", "tRue", "fAlse"};
        for (Object boolVal : boolVals) {
            params.put("varBool", boolVal);
            Validation.validate(params, new String[]{"varBool", "Bool", null});
        }
        notBoolVals = new Object[]{1, 0, "1", "0", 1.0, "hello", new String[0], new HashMap<String, String>()};
        for (Object notBoolVal : notBoolVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varBool", notBoolVal);
                Validation.validate(params, new String[]{"varBool", "Bool", null});
            }, "必须是bool型");
        }

        // BoolTrue
        boolVals = new Object[]{true, "true", "trUe"};
        for (Object boolVal : boolVals) {
            params.put("varBool", boolVal);
            Validation.validate(params, new String[]{"varBool", "BoolTrue", null});
        }
        notBoolVals = new Object[]{false, "false", "falsE", 1, 0, "1", "0", 1.0, "hello", new String[0], new HashMap<String, String>()};
        for (Object notBoolVal : notBoolVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varBool", notBoolVal);
                Validation.validate(params, new String[]{"varBool", "BoolTrue", null});
            }, "必须为true");
        }

        // BoolFalse
        boolVals = new Object[]{false, "false", "fAlse"};
        for (Object boolVal : boolVals) {
            params.put("varBool", boolVal);
            Validation.validate(params, new String[]{"varBool", "BoolFalse", null});
        }
        notBoolVals = new Object[]{true, "true", "trUe", 1, 0, "1", "0", 1.0, "hello", new String[0], new HashMap<String, String>()};
        for (Object notBoolVal : notBoolVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varBool", notBoolVal);
                Validation.validate(params, new String[]{"varBool", "BoolFalse", null});
            }, "必须为false");
        }

        // BoolSmart
        boolVals = new Object[]{true, false, "true", "false", "tRue", "fAlse", 1, 0, "1", "0", "Yes", "no", "y", "n"};
        for (Object boolVal : boolVals) {
            params.put("varBool", boolVal);
            Validation.validate(params, new String[]{"varBool", "BoolSmart", null});
        }
        notBoolVals = new Object[]{8, "100", 1.0, 0.0, "1.0", "0.0", "hello", new String[0], new HashMap<String, String>()};
        for (Object notBoolVal : notBoolVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varBool", notBoolVal);
                Validation.validate(params, new String[]{"varBool", "BoolSmart", null});
            }, "只能取这些值: true, false");
        }

        // BoolSmartTrue
        boolVals = new Object[]{true, "true", "tRue", 1, "1", "Yes", "y"};
        for (Object boolVal : boolVals) {
            params.put("varBool", boolVal);
            Validation.validate(params, new String[]{"varBool", "BoolSmartTrue", null});
        }
        notBoolVals = new Object[]{false, "false", "fAlse", 0, "0", "no", "n", 8, "100", 1.0, 0.0, "1.0", "0.0", "hello", new String[0], new HashMap<String, String>()};
        for (Object notBoolVal : notBoolVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varBool", notBoolVal);
                Validation.validate(params, new String[]{"varBool", "BoolSmartTrue", null});
            }, "只能取这些值: true, 1, yes, y（忽略大小写）");
        }

        // BoolSmartFalse
        boolVals = new Object[]{false, "false", "fAlse", 0, "0", "no", "n"};
        for (Object boolVal : boolVals) {
            params.put("varBool", boolVal);
            Validation.validate(params, new String[]{"varBool", "BoolSmartFalse", null});
        }
        notBoolVals = new Object[]{true, "true", "tRue", 1, "1", "Yes", "y", 8, "100", 1.0, 0.0, "1.0", "0.0", "hello", new String[0], new HashMap<String, String>()};
        for (Object notBoolVal : notBoolVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varBool", notBoolVal);
                Validation.validate(params, new String[]{"varBool", "BoolSmartFalse", null});
            }, "只能取这些值: false, 0, no, n（忽略大小写）");
        }

    }

    public void testValidateFloat() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] floatVals;
        Object[] notFloatVals;

        // Float
        floatVals = new Object[]{"-1", "0", "1", "-1.0", "0.0", "1.0", -1.0, 0.0, 1.0, -1, 0, 1};
        for (Object floatVal : floatVals) {
            params.put("varFloat", floatVal);
            Validation.validate(params, new String[]{"varFloat", "Float", null});
        }
        notFloatVals = new Object[]{true, false, "abc", ".", " -1.0", "", new String[0], new HashMap<String, String>()};
        for (Object notFloatVal : notFloatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", notFloatVal);
                Validation.validate(params, new String[]{"varFloat", "Float", null});
            }, "必须是浮点数");
        }

        // FloatGt
        floatVals = new Object[]{"1", "1.0", "0.1", 1, 1.0, 0.1};
        for (Object floatVal : floatVals) {
            params.put("varFloat", floatVal);
            Validation.validate(params, new String[]{"varFloat", "FloatGt:0.0", null});
        }
        floatVals = new Object[]{"-1", "-1.0", "-0.1", "0", "0.0", -1, -1.0, -0.1, 0, 0.0};
        for (Object floatVal : floatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", floatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGt:0", null});
            }, "必须大于 0.0");
        }
        notFloatVals = new Object[]{true, false, "abc", ".", " -1.0", "", new String[0], new HashMap<String, String>()};
        for (Object notFloatVal : notFloatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", notFloatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGt:0.0", null});
            }, "必须是浮点数");
        }

        // FloatGe
        floatVals = new Object[]{"1", "1.0", "0.1", "0", "0.0", 1, 1.0, 0.1, 0, 0.0};
        for (Object floatVal : floatVals) {
            params.put("varFloat", floatVal);
            Validation.validate(params, new String[]{"varFloat", "FloatGe:0.0", null});
        }
        floatVals = new Object[]{"-1", "-1.0", "-0.1", -1, -1.0, -0.1};
        for (Object floatVal : floatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", floatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGe:0", null});
            }, "必须大于等于 0.0");
        }
        notFloatVals = new Object[]{true, false, "abc", ".", " -1.0", "", new String[0], new HashMap<String, String>()};
        for (Object notFloatVal : notFloatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", notFloatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGe:0.0", null});
            }, "必须是浮点数");
        }

        // FloatLt
        floatVals = new Object[]{"-1", "-1.0", "-0.1", -1, -1.0, -0.1};
        for (Object floatVal : floatVals) {
            params.put("varFloat", floatVal);
            Validation.validate(params, new String[]{"varFloat", "FloatLt:0.0", null});
        }
        floatVals = new Object[]{"1", "1.0", "0.1", "0", "0.0", 1, 1.0, 0.1, 0, 0.0};
        for (Object floatVal : floatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", floatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatLt:0", null});
            }, "必须小于 0.0");
        }
        notFloatVals = new Object[]{true, false, "abc", ".", " -1.0", "", new String[0], new HashMap<String, String>()};
        for (Object notFloatVal : notFloatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", notFloatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatLt:0.0", null});
            }, "必须是浮点数");
        }

        // FloatLe
        floatVals = new Object[]{"-1", "-1.0", "-0.1", "0", "0.0", -1, -1.0, -0.1, 0, 0.0};
        for (Object floatVal : floatVals) {
            params.put("varFloat", floatVal);
            Validation.validate(params, new String[]{"varFloat", "FloatLe:0.0", null});
        }
        floatVals = new Object[]{"1", "1.0", "0.1", 1, 1.0, 0.1};
        for (Object floatVal : floatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", floatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatLe:0", null});
            }, "必须小于等于 0.0");
        }
        notFloatVals = new Object[]{true, false, "abc", ".", " -1.0", "", new String[0], new HashMap<String, String>()};
        for (Object notFloatVal : notFloatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", notFloatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatLe:0.0", null});
            }, "必须是浮点数");
        }

        // FloatGeLe
        params.put("varFloat", "0");
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:0.0,0", null});
        params.put("varFloat", "0.0");
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:0.0,0", null});
        params.put("varFloat", 0);
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:0,0.0", null});
        params.put("varFloat", 0.0);
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:0,0.0", null});
        params.put("varFloat", "-11");
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:-100.0,100", null});
        params.put("varFloat", "11.0");
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:-100.0,100", null});
        params.put("varFloat", 11);
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:-100,100.0", null});
        params.put("varFloat", -11.0);
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:-100,100.0", null});
        params.put("varFloat", "0123");
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:123.0,123", null});
        params.put("varFloat", "0123.0");
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:123.0,123", null});
        params.put("varFloat", -123);
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:-123,-123.0", null});
        params.put("varFloat", -123.0);
        Validation.validate(params, new String[]{"varFloat", "FloatGeLe:-123,-123.0", null});
        floatVals = new Object[]{"-1", "-1.0", "-0.51", "1.1", "2", -1, -1.0, -0.51, 1.1, 2};
        for (Object floatVal : floatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", floatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGeLe:-0.5,1.0", null});
            }, "必须大于等于 -0.5 小于等于 1.0");
        }
        notFloatVals = new Object[]{true, false, "abc", ".", " -1.0", "", new String[0], new HashMap<String, String>()};
        for (Object notFloatVal : notFloatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", notFloatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGeLe:0.0,10", null});
            }, "必须是浮点数");
        }

        // FloatGtLt
        floatVals = new Object[]{"0", "0.0", "-.1", -0.1, 0, 0.0, "000", "00.0"};
        for (Object floatVal : floatVals) {
            params.put("varFloat", floatVal);
            Validation.validate(params, new String[]{"varFloat", "FloatGtLt:-1.0,1", null});
        }
        floatVals = new Object[]{"-1", "-1.0", "1", "1.0", -1, -1.0, 1, 1.0};
        for (Object floatVal : floatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", floatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGtLt:-1,1.0", null});
            }, "必须大于 -1.0 小于 1.0");
        }
        notFloatVals = new Object[]{true, false, "abc", ".", " -1.0", "", new String[0], new HashMap<String, String>()};
        for (Object notFloatVal : notFloatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", notFloatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGtLt:0.0,1.0", null});
            }, "必须是浮点数");
        }

        // FloatGtLe
        floatVals = new Object[]{"0", "0.0", "-.9", "1", "1.0", -0.9, 0, 0.0, 1, 1.0, "000", "00.0"};
        for (Object floatVal : floatVals) {
            params.put("varFloat", floatVal);
            Validation.validate(params, new String[]{"varFloat", "FloatGtLe:-1.,1", null});
        }
        floatVals = new Object[]{"-1", "-1.0", "1.1", "10", -1, -1.0, 1.1, 10};
        for (Object floatVal : floatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", floatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGtLe:-1,1.0", null});
            }, "必须大于 -1.0 小于等于 1.0");
        }
        notFloatVals = new Object[]{true, false, "abc", ".", " -1.0", "", new String[0], new HashMap<String, String>()};
        for (Object notFloatVal : notFloatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", notFloatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGtLe:0.0,1.0", null});
            }, "必须是浮点数");
        }

        // FloatGeLt
        floatVals = new Object[]{"0", "0.0", ".9", "-1", "-1.0", 0.9, 0, 0.0, -1, -1.0, "000", "00.0"};
        for (Object floatVal : floatVals) {
            params.put("varFloat", floatVal);
            Validation.validate(params, new String[]{"varFloat", "FloatGeLt:-1.,1", null});
        }
        floatVals = new Object[]{"1", "1.0", "-1.1", "-10", 1, 1.0, -1.1, -10};
        for (Object floatVal : floatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", floatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGeLt:-1,1.0", null});
            }, "必须大于等于 -1.0 小于 1.0");
        }
        notFloatVals = new Object[]{true, false, "abc", ".", " -1.0", "", new String[0], new HashMap<String, String>()};
        for (Object notFloatVal : notFloatVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varFloat", notFloatVal);
                Validation.validate(params, new String[]{"varFloat", "FloatGeLt:0.0,1.0", null});
            }, "必须是浮点数");
        }

    }

    public void testValidateList() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] listVals;
        Object[] notListVals;

        // List
        listVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde", "fg"},
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde", "fg")),
        };
        for (Object listVal : listVals) {
            params.put("varList", listVal);
            Validation.validate(params, new String[]{"varList", "List", null});
        }
        notListVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notListVal : notListVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varList", notListVal);
                Validation.validate(params, new String[]{"varList", "List", null});
            }, "必须是数组或List");
        }

        // ListLen
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varList", 123);
            Validation.validate(params, new String[]{"varList", "ListLen:-22", null});
        }, "验证器 ListLen 的参数必须是一个非负整数");

        listVals = new Object[]{
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde", "fg")),
        };
        for (Object listVal : listVals) {
            params.put("varList", listVal);
            Validation.validate(params, new String[]{"varList", "ListLen:" + ((List<Object>) listVal).size(), null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varList", "ListLen:" + (((List<Object>) listVal).size() + 1), null});
            }, "长度必须等于");
        }
        listVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde", "fg"},
        };
        for (Object listVal : listVals) {
            params.put("varList", listVal);
            Validation.validate(params, new String[]{"varList", "ListLen:" + ((Object[]) listVal).length, null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varList", "ListLen:" + (((Object[]) listVal).length + 1), null});
            }, "长度必须等于");
        }
        notListVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notListVal : notListVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varList", notListVal);
                Validation.validate(params, new String[]{"varList", "ListLen:22", null});
            }, "必须是数组或List");
        }

        // ListLenGe
        listVals = new Object[]{
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde", "fg")),
        };
        for (Object listVal : listVals) {
            params.put("varList", listVal);
            Validation.validate(params, new String[]{"varList", "ListLenGe:" + ((List<Object>) listVal).size(), null});
            if (((List<Object>) listVal).size() > 0)
                Validation.validate(params, new String[]{"varList", "ListLenGe:" + (((List<Object>) listVal).size() - 1), null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varList", "ListLenGe:" + (((List<Object>) listVal).size() + 1), null});
            }, "长度必须大于等于");
        }
        listVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde", "fg"},
        };
        for (Object listVal : listVals) {
            params.put("varList", listVal);
            Validation.validate(params, new String[]{"varList", "ListLenGe:" + ((Object[]) listVal).length, null});
            if (((Object[]) listVal).length > 0)
                Validation.validate(params, new String[]{"varList", "ListLenGe:" + (((Object[]) listVal).length - 1), null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varList", "ListLenGe:" + (((Object[]) listVal).length + 1), null});
            }, "长度必须大于等于");
        }
        notListVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notListVal : notListVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varList", notListVal);
                Validation.validate(params, new String[]{"varList", "ListLenGe:22", null});
            }, "必须是数组或List");
        }

        // ListLenLe
        listVals = new Object[]{
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde", "fg")),
        };
        for (Object listVal : listVals) {
            params.put("varList", listVal);
            Validation.validate(params, new String[]{"varList", "ListLenLe:" + ((List<Object>) listVal).size(), null});
            Validation.validate(params, new String[]{"varList", "ListLenLe:" + (((List<Object>) listVal).size() + 1), null});
            if (((List<Object>) listVal).size() > 0) {
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"varList", "ListLenLe:" + (((List<Object>) listVal).size() - 1), null});
                }, "长度必须小于等于");
            }
        }
        listVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde", "fg"},
        };
        for (Object listVal : listVals) {
            params.put("varList", listVal);
            Validation.validate(params, new String[]{"varList", "ListLenLe:" + ((Object[]) listVal).length, null});
            Validation.validate(params, new String[]{"varList", "ListLenLe:" + (((Object[]) listVal).length + 1), null});
            if (((Object[]) listVal).length > 0) {
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"varList", "ListLenLe:" + (((Object[]) listVal).length - 1), null});
                }, "长度必须小于等于");
            }
        }
        notListVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notListVal : notListVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varList", notListVal);
                Validation.validate(params, new String[]{"varList", "ListLenLe:22", null});
            }, "必须是数组或List");
        }

        // ListLenGeLe
        listVals = new Object[]{
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde", "fg")),
        };
        for (Object listVal : listVals) {
            params.put("varList", listVal);
            List<Object> list = (List<Object>) listVal;
            Validation.validate(params, new String[]{"varList", "ListLenGeLe:" + list.size() + "," + list.size(), null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varList", "ListLenGeLe:" + (list.size() + 1) + "," + (list.size() + 1), null});
            }, "长度必须在");
        }
        listVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde", "fg"},
        };
        for (Object listVal : listVals) {
            params.put("varList", listVal);
            Object[] list = (Object[]) listVal;
            Validation.validate(params, new String[]{"varList", "ListLenGeLe:" + list.length + "," + list.length, null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varList", "ListLenGeLe:" + (list.length + 1) + "," + (list.length + 1), null});
            }, "长度必须在");
        }
        notListVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notListVal : notListVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varList", notListVal);
                Validation.validate(params, new String[]{"varList", "ListLenGeLe:22,23", null});
            }, "必须是数组或List");
        }

    }

    public void testValidateArr() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] arrVals;
        Object[] notArrVals;

        // Arr
        arrVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde"},
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde")),
        };
        for (Object arrVal : arrVals) {
            params.put("varArr", arrVal);
            Validation.validate(params, new String[]{"varArr", "Arr", null});
        }
        notArrVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notArrVal : notArrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varArr", notArrVal);
                Validation.validate(params, new String[]{"varArr", "Arr", null});
            }, "必须是数组或List");
        }

        // ArrLen
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varArr", 123);
            Validation.validate(params, new String[]{"varArr", "ArrLen:-22", null});
        }, "验证器 ArrLen 的参数必须是一个非负整数");

        arrVals = new Object[]{
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde")),
        };
        for (Object arrVal : arrVals) {
            params.put("varArr", arrVal);
            Validation.validate(params, new String[]{"varArr", "ArrLen:" + ((List<Object>) arrVal).size(), null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varArr", "ArrLen:" + (((List<Object>) arrVal).size() + 1), null});
            }, "长度必须等于");
        }
        arrVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde"},
        };
        for (Object arrVal : arrVals) {
            params.put("varArr", arrVal);
            Validation.validate(params, new String[]{"varArr", "ArrLen:" + ((Object[]) arrVal).length, null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varArr", "ArrLen:" + (((Object[]) arrVal).length + 1), null});
            }, "长度必须等于");
        }
        notArrVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notArrVal : notArrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varArr", notArrVal);
                Validation.validate(params, new String[]{"varArr", "ArrLen:22", null});
            }, "必须是数组或List");
        }

        // ArrLenGe
        arrVals = new Object[]{
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde")),
        };
        for (Object arrVal : arrVals) {
            params.put("varArr", arrVal);
            Validation.validate(params, new String[]{"varArr", "ArrLenGe:" + ((List<Object>) arrVal).size(), null});
            if (((List<Object>) arrVal).size() > 0)
                Validation.validate(params, new String[]{"varArr", "ArrLenGe:" + (((List<Object>) arrVal).size() - 1), null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varArr", "ArrLenGe:" + (((List<Object>) arrVal).size() + 1), null});
            }, "长度必须大于等于");
        }
        arrVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde"},
        };
        for (Object arrVal : arrVals) {
            params.put("varArr", arrVal);
            Validation.validate(params, new String[]{"varArr", "ArrLenGe:" + ((Object[]) arrVal).length, null});
            if (((Object[]) arrVal).length > 0)
                Validation.validate(params, new String[]{"varArr", "ArrLenGe:" + (((Object[]) arrVal).length - 1), null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varArr", "ArrLenGe:" + (((Object[]) arrVal).length + 1), null});
            }, "长度必须大于等于");
        }
        notArrVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notArrVal : notArrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varArr", notArrVal);
                Validation.validate(params, new String[]{"varArr", "ArrLenGe:22", null});
            }, "必须是数组或List");
        }


        // ArrLenLe
        arrVals = new Object[]{
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde")),
        };
        for (Object arrVal : arrVals) {
            params.put("varArr", arrVal);
            Validation.validate(params, new String[]{"varArr", "ArrLenLe:" + ((List<Object>) arrVal).size(), null});
            Validation.validate(params, new String[]{"varArr", "ArrLenLe:" + (((List<Object>) arrVal).size() + 1), null});
            if (((List<Object>) arrVal).size() > 0) {
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"varArr", "ArrLenLe:" + (((List<Object>) arrVal).size() - 1), null});
                }, "长度必须小于等于");
            }
        }
        arrVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde"},
        };
        for (Object arrVal : arrVals) {
            params.put("varArr", arrVal);
            Validation.validate(params, new String[]{"varArr", "ArrLenLe:" + ((Object[]) arrVal).length, null});
            Validation.validate(params, new String[]{"varArr", "ArrLenLe:" + (((Object[]) arrVal).length + 1), null});
            if (((Object[]) arrVal).length > 0) {
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"varArr", "ArrLenLe:" + (((Object[]) arrVal).length - 1), null});
                }, "长度必须小于等于");
            }
        }
        notArrVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notArrVal : notArrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varArr", notArrVal);
                Validation.validate(params, new String[]{"varArr", "ArrLenLe:22", null});
            }, "必须是数组或List");
        }

        // ArrLenGeLe
        arrVals = new Object[]{
            new ArrayList<>(),
            new ArrayList<>(Arrays.asList(1, 2, 3)),
            new ArrayList<>(Arrays.asList("a", "b", "cde", "fg")),
        };
        for (Object arrVal : arrVals) {
            params.put("varArr", arrVal);
            List<Object> list = (List<Object>) arrVal;
            Validation.validate(params, new String[]{"varArr", "ArrLenGeLe:" + list.size() + "," + list.size(), null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varArr", "ArrLenGeLe:" + (list.size() + 1) + "," + (list.size() + 1), null});
            }, "长度必须在");
        }
        arrVals = new Object[]{
            new String[0],
            new Integer[]{1, 2, 3},
            new String[]{"a", "b", "cde", "fg"},
        };
        for (Object arrVal : arrVals) {
            params.put("varArr", arrVal);
            Object[] list = (Object[]) arrVal;
            Validation.validate(params, new String[]{"varArr", "ArrLenGeLe:" + list.length + "," + list.length, null});
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varArr", "ArrLenGeLe:" + (list.length + 1) + "," + (list.length + 1), null});
            }, "长度必须在");
        }
        notArrVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new HashMap<String, String>()};
        for (Object notArrVal : notArrVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varArr", notArrVal);
                Validation.validate(params, new String[]{"varArr", "ArrLenGeLe:22,23", null});
            }, "必须是数组或List");
        }

    }

    public void testValidateMap() throws Exception {
        HashMap<String, Object> params = new HashMap<>();

        // Map
        Object[] mapVals = new Object[]{
            new HashMap<String, Object>(),
            new HashMap<String, Object>() {{
                put("a", 1);
            }},
            new HashMap<String, Object>() {{
                put("abc", "123");
                put("hello", "hi");
            }},
        };
        for (Object mapVal : mapVals) {
            params.put("varMap", mapVal);
            Validation.validate(params, new String[]{"varMap", "Map", null});
        }
        Object[] notMapVals = new Object[]{10.0, 1, -100, true, false, "abc", ".", " -1.0", "", new String[0]};
        for (Object notMapVal : notMapVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("varMap", notMapVal);
                Validation.validate(params, new String[]{"varMap", "Map", null});
            }, "必须是 Map<String, Object>");
        }
    }

    public void testValidateCustomHint() throws Exception {
        HashMap<String, Object> params = new HashMap<>();

        // 验证器为空时
        params.put("id", 1);
        Validation.validate(params, new String[]{"id", "", null});

        // >>> 自定义验证失败的提示
        Validation.validateValue(1, new String[]{"Int|>>>:验证会通过,不会抛出异常"});
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validateValue("abc", new String[]{"Int|>>>:对不起, 您必须输入一个整数"});
        }, "对不起, 您必须输入一个整数");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validateValue("abc", new String[]{"Int|>>>:对不起, 您必须输入一个整数|Arr"});
        }, "对不起, 您必须输入一个整数|Arr");
        Validation.validateValue("123", new String[]{"Str|Int|>>>:对不起, 您必须输入一个包含数字的字符串"});
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validateValue(123, new String[]{"Str|Int|>>>:对不起, 您必须输入一个包含数字的字符串"});
        }, "对不起, 您必须输入一个包含数字的字符串");

        // Alias 参数别名相关
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validateValue("abc", new String[]{"Alias:参数别名|Int"});
        }, "“参数别名”必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validateValue("abc", new String[]{"Bool|Alias:param alias"});
        }, "“param alias”必须是bool型(true or false)");
        Validation.validateValue("123", new String[]{"Alias:参数别名"});

    }

    public void testValidateCompile() throws Exception {
        // >>> 自定义验证失败的提示
        Validation.validateValue("1||2/3/", new String[]{"Regexp:/^1\\|\\|2\\/3\\//"});
    }
}