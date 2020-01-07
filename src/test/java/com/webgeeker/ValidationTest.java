package com.webgeeker;

import com.webgeeker.validation.Validation;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.JSONObject;

import java.util.*;

/**
 * Unit test for simple App.
 */
public class ValidationTest
    extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ValidationTest(String testName) {
        super(testName);

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
    public static Test suite() {
        return new TestSuite(ValidationTest.class);
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
            Validation.validate(params, new String[]{"name", "Str|Required", null});
        }, "Required只能出现在验证规则的开头（IfXxx后面）");

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

        // IntNe
        params.put("varInt", "1");
        Validation.validate(params, new String[]{"varInt", "IntNe:-1", null});
        params.put("varInt", 1);
        Validation.validate(params, new String[]{"varInt", "IntNe:-1", null});
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误1
            params.put("varInt", "abc");
            Validation.validate(params, new String[]{"varInt", "IntNe:-1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            // 类型错误2
            params.put("varInt", true);
            Validation.validate(params, new String[]{"varInt", "IntNe:-1", null});
        }, "必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", "-1");
            Validation.validate(params, new String[]{"varInt", "IntNe:-1", null});
        }, "不能等于 -1");
        _assertThrowExpectionContainErrorString(() -> {
            params.put("varInt", -1);
            Validation.validate(params, new String[]{"varInt", "IntNe:-1", null});
        }, "不能等于 -1");

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

    public void testValidateLong() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] longVals = null;
        Object[] notLongVals = null;

        // Long
        longVals = new Object[]{"-1", "0", "1", -1, 0, 1, -1L, 0L, 1L, 4294967296L, -4294967297L, "4294967296", "-4294967297"};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "Long", null});
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "Long", null});
            }, "必须是长整数");
        }

        // LongEq
        longVals = new Object[]{"-1", -1, -1L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongEq:-1", null});
        }
        longVals = new Object[]{"0", 0, 0L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongEq:-1", null});
            }, "必须等于 -1");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongEq:-1", null});
            }, "必须是长整数");
        }

        // LongNe
        longVals = new Object[]{"0", 0, 0L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongNe:-1", null});
        }
        longVals = new Object[]{"-1", -1, -1L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongNe:-1", null});
            }, "不能等于 -1");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongNe:-1", null});
            }, "必须是长整数");
        }

        // LongGt
        longVals = new Object[]{"1", 1, 1L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongGt:0", null});
        }
        longVals = new Object[]{"0", 0, 0L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGt:0", null});
            }, "必须大于 0");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGt:0", null});
            }, "必须是长整数");
        }

        // LongGe
        longVals = new Object[]{"1", 1, 1L, "0", 0, 0L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongGe:0", null});
        }
        longVals = new Object[]{"-1", -1, -1L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGe:0", null});
            }, "必须大于等于 0");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGe:0", null});
            }, "必须是长整数");
        }

        // LongLt
        longVals = new Object[]{"-1", -1, -1L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongLt:0", null});
        }
        longVals = new Object[]{"0", 0, 0L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongLt:0", null});
            }, "必须小于 0");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongLt:0", null});
            }, "必须是长整数");
        }

        // LongLe
        longVals = new Object[]{"-1", -1, -1L, "0", 0, 0L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongLe:0", null});
        }
        longVals = new Object[]{"1", 1, 1L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongLe:0", null});
            }, "必须小于等于 0");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongLe:0", null});
            }, "必须是长整数");
        }

        // LongGeLe
        longVals = new Object[]{"0", 0, 0L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongGeLe:0,0", null});
        }
        longVals = new Object[]{"-123", -123, -123L, "-0123", "-1", -1, -1L, "0", 0, 0L, "1", 1, 1L, "123", 123, 123L, "0123"};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongGeLe:-123,123", null});
        }
        longVals = new Object[]{"-124", -124, -124L, "124", 124, 124L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGeLe:-123,123", null});
            }, "必须大于等于 -123 小于等于 123");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGeLe:0,0", null});
            }, "必须是长整数");
        }

        // LongGtLt
        longVals = new Object[]{"-122", -122, -122L, "-0122", "-1", -1, -1L, "0", "000", 0, 0L, "1", 1, 1L, "122", 122, 122L, "0122"};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongGtLt:-123,123", null});
        }
        longVals = new Object[]{"-123", -123, -123L, "123", 123, 123L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGtLt:-123,123", null});
            }, "必须大于 -123 小于 123");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGtLt:-123,123", null});
            }, "必须是长整数");
        }

        // LongGtLe
        longVals = new Object[]{"-122", -122, -122L, "-0122", "-1", -1, -1L, "01", "0", 0, 0L, "1", 1, 1L, "123", 123, 123L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongGtLe:-123,123", null});
        }
        longVals = new Object[]{"-123", -123, -123L, "124", 124, 124L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGtLe:-123,123", null});
            }, "必须大于 -123 小于等于 123");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGtLe:-123,123", null});
            }, "必须是长整数");
        }

        // LongGeLt
        longVals = new Object[]{"-123", -123, -123L, "-0122", "-1", -1, -1L, "01", "0", 0, 0L, "1", 1, 1L, "122", 122, 122L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongGeLt:-123,123", null});
        }
        longVals = new Object[]{"-124", -124, -124L, "123", 123, 123L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGeLt:-123,123", null});
            }, "必须大于等于 -123 小于 123");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongGeLt:-123,123", null});
            }, "必须是长整数");
        }

        // LongIn
        longVals = new Object[]{"1", 1, 1L, "2", "002", 2, 2L, "-3", -3, -3L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongIn:1,2,-3", null});
        }
        longVals = new Object[]{"0", 0, 0L, "123", 123, 123L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongIn:1,2,-3", null});
            }, "只能取这些值: 1, 2, -3");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongIn:1,2,-3", null});
            }, "必须是长整数");
        }

        // LongNotIn
        longVals = new Object[]{"0", 0, 0L, "123", 123, 123L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            Validation.validate(params, new String[]{"varLong", "LongNotIn:1,2,-3", null});
        }
        longVals = new Object[]{"1", 1, 1L, "2", "002", 2, 2L, "-3", -3, -3L};
        for (Object longVal : longVals) {
            params.put("varLong", longVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongNotIn:1,2,-3", null});
            }, "不能取这些值: 1, 2, -3");
        }
        notLongVals = new Object[]{true, 0.0, 1.0f, "0.0", "abc", "", 'a', new Byte((byte) 1), new Short((short) 1), new Long[0], new ArrayList<>(), new HashMap<String, Object>()};
        for (Object notLongVal : notLongVals) {
            params.put("varLong", notLongVal);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"varLong", "LongNotIn:1,2,-3", null});
            }, "必须是长整数");
        }

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

        // 为了提高测试覆盖率: compileValidator() 方法中的行: else if (pos == len - 2)
        params.put("param", "abc/");
        Validation.validate(params, new String[]{"param", "Regexp:/^(abc\\/|def)$/", null});
        params.put("param", "def");
        Validation.validate(params, new String[]{"param", "Regexp:/^(abc\\/|def)$/", null});

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

    public void testValidateDate() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        String[] dateVals;
        Object[] notDateVals;

        // Date
        dateVals = new String[] {"2017-06-01", "2017-6-1", "2017-6-01", "2017-06-1"};
        for (String dateVal : dateVals) {
            params.put("date", dateVal);
            Validation.validate(params, new String[]{"date", "Date", null});
        }
        notDateVals = new Object[]{"17-6-1", "2017 6 1", "2017/6/1", "2017-06", "2017-06-31", true, 1.0, "345", new String[0]};
        for (Object notDateVal : notDateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("date", notDateVal);
                Validation.validate(params, new String[]{"date", "Date", null});
            }, "必须是合法的日期，格式为：YYYY-MM-DD");
        }

        // DateFrom
        dateVals = new String[] {"2017-06-15", "2017-6-16"};
        for (String dateVal : dateVals) {
            params.put("date", dateVal);
            Validation.validate(params, new String[]{"date", "DateFrom:2017-06-15", null});
        }
        dateVals = new String[]{"2017-06-14", "2011-6-1"};
        for (String dateVal : dateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("date", dateVal);
                Validation.validate(params, new String[]{"date", "DateFrom:2017-06-15", null});
            }, "不得早于 2017-06-15");
        }
        notDateVals = new Object[]{"17-6-1", "2017 6 1", "2017/6/1", "2017-06", true, 1.0, "345", new String[0]};
        for (Object notDateVal : notDateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("date", notDateVal);
                Validation.validate(params, new String[]{"date", "DateFrom:2017-06-15", null});
            }, "必须是合法的日期，格式为：YYYY-MM-DD");
        }
        _assertThrowExpectionContainErrorString(() -> {
            params.put("date", "2017-06-15");
            Validation.validate(params, new String[]{"date", "DateFrom:2017/06/15", null});
        }, "验证器 DateFrom 格式错误. 正确的格式示例: DateFrom:2017-04-13");

        // DateTo
        dateVals = new String[] {"2017-06-15", "2017-6-14"};
        for (String dateVal : dateVals) {
            params.put("date", dateVal);
            Validation.validate(params, new String[]{"date", "DateTo:2017-06-15", null});
        }
        dateVals = new String[]{"2017-06-16", "2017-6-17"};
        for (String dateVal : dateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("date", dateVal);
                Validation.validate(params, new String[]{"date", "DateTo:2017-06-15", null});
            }, "不得晚于 2017-06-15");
        }
        notDateVals = new Object[]{"17-6-1", "2017 6 1", "2017/6/1", "2017-06", true, 1.0, "345", new String[0]};
        for (Object notDateVal : notDateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("date", notDateVal);
                Validation.validate(params, new String[]{"date", "DateTo:2017-06-15", null});
            }, "必须是合法的日期，格式为：YYYY-MM-DD");
        }
        _assertThrowExpectionContainErrorString(() -> {
            params.put("date", "2017-06-15");
            Validation.validate(params, new String[]{"date", "DateTo:2017/06/15", null});
        }, "验证器 DateTo 格式错误. 正确的格式示例: DateTo:2017-04-13");

        // DateFromTo
        dateVals = new String[] {"2017-06-15", "2017-6-14", "2017-6-10", "2017-6-20"};
        for (String dateVal : dateVals) {
            params.put("date", dateVal);
            Validation.validate(params, new String[]{"date", "DateFromTo:2017-06-10,2017-06-20", null});
        }
        dateVals = new String[]{"2017-06-9", "2017-6-21"};
        for (String dateVal : dateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("date", dateVal);
                Validation.validate(params, new String[]{"date", "DateFromTo:2017-06-10,2017-06-20", null});
            }, "必须在 2017-06-10 ~ 2017-06-20 之间");
        }
        notDateVals = new Object[]{"17-6-1", "2017 6 1", "2017/6/1", "2017-06", true, 1.0, "345", new String[0]};
        for (Object notDateVal : notDateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("date", notDateVal);
                Validation.validate(params, new String[]{"date", "DateFromTo:2017-06-10,2017-06-20", null});
            }, "必须是合法的日期，格式为：YYYY-MM-DD");
        }
        _assertThrowExpectionContainErrorString(() -> {
            params.put("date", "2017-06-15");
            Validation.validate(params, new String[]{"date", "DateFromTo:2017-06-15", null});
        }, "验证器 DateFromTo 格式错误. 正确的格式示例: DateFromTo:2017-04-13,2017-04-13");

    }

    public void testValidateDateTime() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        String[] dateVals;
        Object[] notDateVals;

        // DateTime
        dateVals = new String[] {"2017-06-01 12:00:00", "2017-6-1 12:00:00", "2017-6-01 12:00:00", "2017-06-1 12:00:00"};
        for (String dateVal : dateVals) {
            params.put("datetime", dateVal);
            Validation.validate(params, new String[]{"datetime", "DateTime", null});
        }
        notDateVals = new Object[]{"2017-06-01 12:00:aa", "2017-06-01 12:00", "2017-06-01 12/00/00", "17-06-01 12:00:00", "2017-06-01", "17-6-1", "2017 6 1", "2017/6/1", "2017-06", "2017-06-01 12:80:80", "2017-06-31 12:00:00", true, 1.0, "345", new String[0]};
        for (Object notDateVal : notDateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("datetime", notDateVal);
                Validation.validate(params, new String[]{"datetime", "DateTime", null});
            }, "必须是合法的日期时间，格式为：YYYY-MM-DD HH:mm:ss");
        }

        // DateTimeFrom
        dateVals = new String[] {"2017-06-15 12:00:00", "2017-6-15 12:00:01"};
        for (String dateVal : dateVals) {
            params.put("datetime", dateVal);
            Validation.validate(params, new String[]{"datetime", "DateTimeFrom:2017-06-15 12:00:00", null});
        }
        dateVals = new String[]{"2017-06-15 11:59:59", "2017-6-15 00:00:00"};
        for (String dateVal : dateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("datetime", dateVal);
                Validation.validate(params, new String[]{"datetime", "DateTimeFrom:2017-06-15 12:00:00", null});
            }, "不得早于 2017-06-15 12:00:00");
        }
        notDateVals = new Object[]{"2017-06-01 12:00:aa", "2017-06-01 12:00", "2017-06-01 12/00/00", "17-06-01 12:00:00", "2017-06-01", "17-6-1", "2017 6 1", "2017/6/1", "2017-06", true, 1.0, "345", new String[0]};
        for (Object notDateVal : notDateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("datetime", notDateVal);
                Validation.validate(params, new String[]{"datetime", "DateTimeFrom:2017-06-15 12:00:00", null});
            }, "必须是合法的日期时间，格式为：YYYY-MM-DD HH:mm:ss");
        }
        _assertThrowExpectionContainErrorString(() -> {
            params.put("datetime", "2017-06-15 12:00:00");
            Validation.validate(params, new String[]{"datetime", "DateTimeFrom:2017-06-15 12/00/00", null});
        }, "验证器 DateTimeFrom 格式错误. 正确的格式示例: DateTimeFrom:2017-04-13 12:00:00");

        // DateTimeTo
        dateVals = new String[] {"2017-06-15 11:59:59", "2017-6-15 11:59:58"};
        for (String dateVal : dateVals) {
            params.put("datetime", dateVal);
            Validation.validate(params, new String[]{"datetime", "DateTimeTo:2017-06-15 12:00:00", null});
        }
        dateVals = new String[]{"2017-06-15 12:00:01", "2017-6-15 12:00:02"};
        for (String dateVal : dateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("datetime", dateVal);
                Validation.validate(params, new String[]{"datetime", "DateTimeTo:2017-06-15 12:00:00", null});
            }, "必须早于 2017-06-15 12:00:00");
        }
        notDateVals = new Object[]{"2017-06-01 12:00:aa", "2017-06-01 12:00", "2017-06-01 12/00/00", "17-06-01 12:00:00", "2017-06-01", "17-6-1", "2017 6 1", "2017/6/1", "2017-06", true, 1.0, "345", new String[0]};
        for (Object notDateVal : notDateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("datetime", notDateVal);
                Validation.validate(params, new String[]{"datetime", "DateTimeTo:2017-06-15 12:00:00", null});
            }, "必须是合法的日期时间，格式为：YYYY-MM-DD HH:mm:ss");
        }
        _assertThrowExpectionContainErrorString(() -> {
            params.put("datetime", "2017-06-15 12:00:00");
            Validation.validate(params, new String[]{"datetime", "DateTimeTo:2017-06-15 12/00/00", null});
        }, "验证器 DateTimeTo 格式错误. 正确的格式示例: DateTimeTo:2017-04-13 12:00:00");

        // DateTimeFromTo
        dateVals = new String[] {"2017-06-15 12:00:00", "2017-06-15 12:30:00", "2017-06-15 12:59:59"};
        for (String dateVal : dateVals) {
            params.put("datetime", dateVal);
            Validation.validate(params, new String[]{"datetime", "DateTimeFromTo:2017-06-15 12:00:00,2017-06-15 13:00:00", null});
        }
        dateVals = new String[]{"2017-06-15 11:59:59", "2017-06-15 13:00:00"};
        for (String dateVal : dateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("datetime", dateVal);
                Validation.validate(params, new String[]{"datetime", "DateTimeFromTo:2017-06-15 12:00:00,2017-06-15 13:00:00", null});
            }, "必须在 2017-06-15 12:00:00 ~ 2017-06-15 13:00:00 之间");
        }
        notDateVals = new Object[]{"2017-06-01 12:00:aa", "2017-06-01 12:00", "2017-06-01 12/00/00", "17-06-01 12:00:00", "2017-06-01", "17-6-1", "2017 6 1", "2017/6/1", "2017-06", true, 1.0, "345", new String[0]};
        for (Object notDateVal : notDateVals) {
            _assertThrowExpectionContainErrorString(() -> {
                params.put("datetime", notDateVal);
                Validation.validate(params, new String[]{"datetime", "DateTimeFromTo:2017-06-15 12:00:00,2017-06-15 13:00:00", null});
            }, "必须是合法的日期时间，格式为：YYYY-MM-DD HH:mm:ss");
        }
        _assertThrowExpectionContainErrorString(() -> {
            params.put("datetime", "2017-06-15 12:00:00");
            Validation.validate(params, new String[]{"datetime", "DateTimeFromTo:2017-06-15", null});
        }, "验证器 DateTimeFromTo 格式错误. 正确的格式示例: DateTimeFromTo:2017-04-13 12:00:00,2017-04-13 12:00:00");

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

        // 多维数组
        Validation.validate(new HashMap<String, Object>() {{
            put("matrix", new Object[]{
                new Object[]{1, 2, 345},
                new Object[]{6, 7, 8},
            });
        }}, new String[]{
            "matrix[*][*]", "Int", null,
            "matrix[0][1]", "IntEq:2", null,
            "matrix[0][100]", "IntEq:2", null,
        });
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>() {{
                put("matrix", new Object[]{
                    new Object[]{1, 2, "abc"},
                    new Object[]{6, 7, 8},
                });
            }}, new String[]{"matrix[*][*]", "Int", null});
        }, "“matrix[0][2]”必须是整数");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>() {{
                put("matrix", new Object[]{
                    new Object[]{1, 2, 345},
                    new Object[]{6, "ddd", 8},
                });
            }}, new String[]{"matrix[*][*]", "Int", null});
        }, "“matrix[1][1]”必须是整数");

        params.clear();
        params.put("comments", new String[0]);
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"comments[*]", "Required", null});
        }, "必须提供“comments[*]”");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"comments", "ArrLenGe:1", null});
        }, "“comments”长度必须大于等于 1");
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
        Validation.validateValue("1||2/3/", new String[]{"Regexp:/^1\\|\\|2\\/3\\//"});
    }

    public void testValidateIfXxx() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        params.put("condition", 1);
        params.put("param", 1);
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"param", "IfIntEq:condition,abc|IntEq:1", null}); //条件成立+验证不通过
        }, "“IfIntEq:condition,abc”中“condition”后面必须是整数");
    }

    public void testValidateIfBool() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] trues;
        Object[] falses;

        // If
        trues = new Object[]{1, "1", true, "true", "yes", "y"};
        falses = new Object[]{0, "0", false, "false", "no", "n", "hello", 2.5}; //'hello'和2.5即不是true, 也不是false
        for (Object trueVal : trues) {
            for (Object falseVal : falses) {
                params.clear();
                params.put("type", falseVal);
                params.put("state", 0);
                Validation.validate(params, new String[]{"state", "If:type|IntEq:0", null}); //条件不成立+验证通过（忽略这条）
                Validation.validate(params, new String[]{"state", "If:type|IntEq:1", null}); //条件不成立+验证不通过（忽略这条）
                params.put("type", trueVal);
                Validation.validate(params, new String[]{"state", "If:type|IntEq:0", null}); //条件成立+验证通过
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"state", "If:type|IntEq:1", null}); //条件成立+验证不通过
                }, "必须等于 1");
            }
        }

        // IfNot
        trues = new Object[]{1, "1", true, "true", "yes", "y", "hello", 2.5}; //'hello'和2.5即不是true, 也不是false
        falses = new Object[]{0, "0", false, "false", "no", "n"};
        for (Object trueVal : trues) {
            for (Object falseVal : falses) {
                params.clear();
                params.put("type", trueVal);
                params.put("state", 0);
                Validation.validate(params, new String[]{"state", "IfNot:type|IntEq:0", null}); //条件不成立+验证通过（忽略这条）
                Validation.validate(params, new String[]{"state", "IfNot:type|IntEq:1", null}); //条件不成立+验证不通过（忽略这条）
                params.put("type", falseVal);
                Validation.validate(params, new String[]{"state", "IfNot:type|IntEq:0", null}); //条件成立+验证通过
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"state", "IfNot:type|IntEq:1", null}); //条件成立+验证不通过
                }, "必须等于 1");
            }
        }

        // IfTrue
        trues = new Object[]{true, "true"};
        falses = new Object[]{false, "false", 0, "0", 1, "1", "yes", "y", "no", "n", "hello", 2.5};
        for (Object trueVal : trues) {
            for (Object falseVal : falses) {
                params.clear();
                params.put("type", falseVal);
                params.put("state", 0);
                Validation.validate(params, new String[]{"state", "IfTrue:type|IntEq:0", null}); //条件不成立+验证通过（忽略这条）
                Validation.validate(params, new String[]{"state", "IfTrue:type|IntEq:1", null}); //条件不成立+验证不通过（忽略这条）
                params.put("type", trueVal);
                Validation.validate(params, new String[]{"state", "IfTrue:type|IntEq:0", null}); //条件成立+验证通过
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"state", "IfTrue:type|IntEq:11", null}); //条件成立+验证不通过
                }, "必须等于 1");
            }
        }

        // IfFalse
        trues = new Object[]{true, "true", 0, "0", 1, "1", "yes", "y", "no", "n", "hello", 2.5};
        falses = new Object[]{false, "false"};
        for (Object trueVal : trues) {
            for (Object falseVal : falses) {
                params.clear();
                params.put("type", trueVal);
                params.put("state", 0);
                Validation.validate(params, new String[]{"state", "IfFalse:type|IntEq:0", null}); //条件不成立+验证通过（忽略这条）
                Validation.validate(params, new String[]{"state", "IfFalse:type|IntEq:1", null}); //条件不成立+验证不通过（忽略这条）
                params.put("type", falseVal);
                Validation.validate(params, new String[]{"state", "IfFalse:type|IntEq:0", null}); //条件成立+验证通过
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"state", "IfFalse:type|IntEq:11", null}); //条件成立+验证不通过
                }, "必须等于 1");
            }
        }

    }

    public void testValidateIfExist() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] existVals;
        Object[] notExistVals;

        // IfExist
        existVals = new Object[]{0, 123, "", "123", true, false, 0.0, 1.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()};
        notExistVals = new Object[]{null, "undefined"}; // 后面对 "undefined" 会作特殊处理(表示条件参数不存在的情况)
        for (Object existVal : existVals) {
            for (Object notExistVal : notExistVals) {
                params.clear();
                if (notExistVal == null)
                    params.put("param1", null);
                params.put("param2", 0);
                Validation.validate(params, new String[]{"param2", "IfExist:param1|IntEq:0", null}); //条件不成立+验证通过（忽略这条）
                Validation.validate(params, new String[]{"param2", "IfExist:param1|IntEq:1", null}); //条件不成立+验证不通过（忽略这条）
                params.put("param1", existVal);
                Validation.validate(params, new String[]{"param2", "IfExist:param1|IntEq:0", null}); //条件成立+验证通过
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"param2", "IfExist:param1|IntEq:1", null}); //条件成立+验证不通过
                }, "必须等于 1");
            }
        }

        // IfNotExist
        existVals = new Object[]{0, 123, "", "123", true, false, 0.0, 1.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()};
        notExistVals = new Object[]{null, "undefined"}; // 后面对 "undefined" 会作特殊处理(表示条件参数不存在的情况)
        for (Object existVal : existVals) {
            for (Object notExistVal : notExistVals) {
                params.clear();
                params.put("param1", existVal);
                params.put("param2", 0);
                Validation.validate(params, new String[]{"param2", "IfNotExist:param1|IntEq:0", null}); //条件不成立+验证通过（忽略这条）
                Validation.validate(params, new String[]{"param2", "IfNotExist:param1|IntEq:1", null}); //条件不成立+验证不通过（忽略这条）
                if (notExistVal == null)
                    params.put("param1", null);
                else
                    params.remove("param1");
                Validation.validate(params, new String[]{"param2", "IfNotExist:param1|IntEq:0", null}); //条件成立+验证通过
                _assertThrowExpectionContainErrorString(() -> {
                    Validation.validate(params, new String[]{"param2", "IfNotExist:param1|IntEq:1", null}); //条件成立+验证不通过
                }, "必须等于 1");
            }
        }
    }

    public void testValidateIfInt() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] intVals;
        Object[] notIntVals;

        // IfIntEq
        intVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100",};
        for (Object intVal : intVals) {
            int intVal2;
            if (intVal instanceof String)
                intVal2 = Integer.parseInt((String) intVal) + 1;
            else
                intVal2 = ((Integer) intVal) + 1;

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntEq:condition," + intVal2 + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntEq:condition," + intVal2 + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntEq:condition," + intVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntEq:condition," + intVal + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfIntEq 条件参数类型错误导致的条件不成立的情况
        notIntVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是整型也不是整型字符串
        for (Object notIntVal : notIntVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntEq:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntEq:condition,1|IntEq:1", null});

        }

        // IfIntNe
        intVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100",};
        for (Object intVal : intVals) {
            int intVal2;
            if (intVal instanceof String)
                intVal2 = Integer.parseInt((String) intVal) + 1;
            else
                intVal2 = ((Integer) intVal) + 1;

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntNe:condition," + intVal + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntNe:condition," + intVal + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntNe:condition," + intVal2 + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntNe:condition," + intVal2 + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfIntNe 条件参数类型错误导致的条件成立的情况
        notIntVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是整型也不是整型字符串
        for (Object notIntVal : notIntVals) {

            //条件成立+验证通过
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntNe:condition,0|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntNe:condition,1|IntEq:1", null});
            }, "必须等于 1");
        }

        // IfIntGt
        intVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100",};
        for (Object intVal : intVals) {
            int intVal2;
            if (intVal instanceof String)
                intVal2 = Integer.parseInt((String) intVal);
            else
                intVal2 = ((Integer) intVal);

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntGt:condition," + (intVal2 + 1) + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntGt:condition," + (intVal2 + 1) + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntGt:condition," + (intVal2 - 1) + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntGt:condition," + (intVal2 - 10) + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfIntGt 条件参数类型错误导致的条件不成立的情况
        notIntVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是整型也不是整型字符串
        for (Object notIntVal : notIntVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntGt:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntGt:condition,1|IntEq:1", null});

        }

        // IfIntGe
        intVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100",};
        for (Object intVal : intVals) {
            int intVal2;
            if (intVal instanceof String)
                intVal2 = Integer.parseInt((String) intVal);
            else
                intVal2 = ((Integer) intVal);

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntGe:condition," + (intVal2 + 1) + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntGe:condition," + (intVal2 + 2) + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntGe:condition," + intVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntGe:condition," + (intVal2 - 1) + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfIntGe 条件参数类型错误导致的条件不成立的情况
        notIntVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是整型也不是整型字符串
        for (Object notIntVal : notIntVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntGe:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntGe:condition,1|IntEq:1", null});

        }

        // IfIntLt
        intVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100",};
        for (Object intVal : intVals) {
            int intVal2;
            if (intVal instanceof String)
                intVal2 = Integer.parseInt((String) intVal);
            else
                intVal2 = ((Integer) intVal);

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntLt:condition," + intVal + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntLt:condition," + (intVal2 - 1) + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntLt:condition," + (intVal2 + 1) + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntLt:condition," + (intVal2 + 10) + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfIntLt 条件参数类型错误导致的条件不成立的情况
        notIntVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是整型也不是整型字符串
        for (Object notIntVal : notIntVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntLt:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntLt:condition,1|IntEq:1", null});

        }

        // IfIntLe
        intVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100",};
        for (Object intVal : intVals) {
            int intVal2;
            if (intVal instanceof String)
                intVal2 = Integer.parseInt((String) intVal);
            else
                intVal2 = ((Integer) intVal);

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntLe:condition," + (intVal2 - 1) + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntLe:condition," + (intVal2 - 2) + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntLe:condition," + intVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntLe:condition," + (intVal2 + 1) + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfIntLe 条件参数类型错误导致的条件不成立的情况
        notIntVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是整型也不是整型字符串
        for (Object notIntVal : notIntVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntLe:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntLe:condition,1|IntEq:1", null});

        }

        // IfIntIn 条件不成立
        Object[] intNotInVals = new Object[]{-13, 13, 123, -123, "-13", "13", "123", "-123",};
        for (Object intNotInVal : intNotInVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intNotInVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntIn:condition,-100,-1,0,1,100|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intNotInVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntIn:condition,-100,-1,0,1,100|IntEq:1", null});
        }
        // IfIntIn 条件成立
        intVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100",};
        for (Object intVal : intVals) {

            //条件成立+验证通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntIn:condition,-100,-1,0,1,100|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", intVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntIn:condition,-100,-1,0,1,100|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfIntIn 条件参数类型错误导致的条件不成立的情况
        notIntVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是整型也不是整型字符串
        for (Object notIntVal : notIntVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntIn:condition,-100,-1,0,1,100|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntIn:condition,-100,-1,0,1,100|IntEq:1", null});

        }

        // IfIntNotIn 条件不成立
        Object[] intInVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100",};
        for (Object intInVal : intInVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intInVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntNotIn:condition,-100,-1,0,1,100|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intInVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfIntNotIn:condition,-100,-1,0,1,100|IntEq:1", null});
        }
        // IfIntNotIn 条件成立
        intNotInVals = new Object[]{-13, 13, 123, -123, "-13", "13", "123", "-123",};
        for (Object intNotInVal : intNotInVals) {

            //条件成立+验证通过
            params.clear();
            params.put("condition", intNotInVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntNotIn:condition,-100,-1,0,1,100|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", intNotInVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntNotIn:condition,-100,-1,0,1,100|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfIntNotIn 条件参数类型错误导致的条件不成立的情况
        notIntVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是整型也不是整型字符串
        for (Object notIntVal : notIntVals) {

            //条件成立+验证通过
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfIntNotIn:condition,-100,-1,0,1,100|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", notIntVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfIntNotIn:condition,-100,-1,0,1,100|IntEq:1", null});
            }, "必须等于 1");

        }

    }

    public void testValidateIfLong() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] longVals;
        Object[] notLongVals;

        // IfLongEq
        longVals = new Object[]{0, -1, 1, 100, -100, 0L, -1L, 1L, 100L, -100L, "0", "-1", "1", "100", "-100", 4294967296L, -4294967297L, "4294967296", "-4294967297",};
        for (Object longVal : longVals) {
            long longVal2;
            if (longVal instanceof String)
                longVal2 = Long.parseLong((String) longVal) + 1;
            else if (longVal instanceof Integer)
                longVal2 = ((Integer) longVal).longValue() + 1;
            else
                longVal2 = ((Long) longVal) + 1;

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongEq:condition," + longVal2 + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongEq:condition," + longVal2 + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongEq:condition," + longVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongEq:condition," + longVal + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfLongEq 条件参数类型错误导致的条件不成立的情况
        notLongVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Byte((byte) 1), new Short((short) 1), new Object[0], new long[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是长整型也不是长整型字符串
        for (Object notLongVal : notLongVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongEq:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongEq:condition,1|IntEq:1", null});

        }

        // IfLongNe
        longVals = new Object[]{0, -1, 1, 100, -100, 0L, -1L, 1L, 100L, -100L, "0", "-1", "1", "100", "-100", 4294967296L, -4294967297L, "4294967296", "-4294967297",};
        for (Object longVal : longVals) {
            long longVal2;
            if (longVal instanceof String)
                longVal2 = Long.parseLong((String) longVal) + 1;
            else if (longVal instanceof Integer)
                longVal2 = ((Integer) longVal).longValue() + 1;
            else
                longVal2 = ((Long) longVal) + 1;

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongNe:condition," + longVal + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongNe:condition," + longVal + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongNe:condition," + longVal2 + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongNe:condition," + longVal2 + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfLongNe 条件参数类型错误导致的条件成立的情况
        notLongVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Byte((byte) 1), new Short((short) 1), new Object[0], new long[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是长整型也不是长整型字符串
        for (Object notLongVal : notLongVals) {

            //条件成立+验证通过
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongNe:condition,0|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongNe:condition,1|IntEq:1", null});
            }, "必须等于 1");
        }

        // IfLongGt
        longVals = new Object[]{0, -1, 1, 100, -100, 0L, -1L, 1L, 100L, -100L, "0", "-1", "1", "100", "-100", 4294967296L, -4294967297L, "4294967296", "-4294967297",};
        for (Object longVal : longVals) {
            long longVal2;
            if (longVal instanceof String)
                longVal2 = Long.parseLong((String) longVal);
            else if (longVal instanceof Integer)
                longVal2 = ((Integer) longVal).longValue();
            else
                longVal2 = ((Long) longVal);

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongGt:condition," + (longVal2 + 1) + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongGt:condition," + (longVal2 + 1) + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongGt:condition," + (longVal2 - 1) + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongGt:condition," + (longVal2 - 10) + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfLongGt 条件参数类型错误导致的条件不成立的情况
        notLongVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Byte((byte) 1), new Short((short) 1), new Object[0], new long[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是长整型也不是长整型字符串
        for (Object notLongVal : notLongVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongGt:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongGt:condition,1|IntEq:1", null});

        }

        // IfLongGe
        longVals = new Object[]{0, -1, 1, 100, -100, 0L, -1L, 1L, 100L, -100L, "0", "-1", "1", "100", "-100", 4294967296L, -4294967297L, "4294967296", "-4294967297",};
        for (Object longVal : longVals) {
            long longVal2;
            if (longVal instanceof String)
                longVal2 = Long.parseLong((String) longVal);
            else if (longVal instanceof Integer)
                longVal2 = ((Integer) longVal).longValue();
            else
                longVal2 = ((Long) longVal);

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongGe:condition," + (longVal2 + 1) + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongGe:condition," + (longVal2 + 2) + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongGe:condition," + longVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongGe:condition," + (longVal2 - 1) + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfLongGe 条件参数类型错误导致的条件不成立的情况
        notLongVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Byte((byte) 1), new Short((short) 1), new Object[0], new long[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是长整型也不是长整型字符串
        for (Object notLongVal : notLongVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongGe:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongGe:condition,1|IntEq:1", null});

        }

        // IfLongLt
        longVals = new Object[]{0, -1, 1, 100, -100, 0L, -1L, 1L, 100L, -100L, "0", "-1", "1", "100", "-100", 4294967296L, -4294967297L, "4294967296", "-4294967297",};
        for (Object longVal : longVals) {
            long longVal2;
            if (longVal instanceof String)
                longVal2 = Long.parseLong((String) longVal);
            else if (longVal instanceof Integer)
                longVal2 = ((Integer) longVal).longValue();
            else
                longVal2 = ((Long) longVal);

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongLt:condition," + longVal + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongLt:condition," + (longVal2 - 1) + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongLt:condition," + (longVal2 + 1) + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongLt:condition," + (longVal2 + 10) + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfLongLt 条件参数类型错误导致的条件不成立的情况
        notLongVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Byte((byte) 1), new Short((short) 1), new Object[0], new long[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是长整型也不是长整型字符串
        for (Object notLongVal : notLongVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongLt:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongLt:condition,1|IntEq:1", null});

        }

        // IfLongLe
        longVals = new Object[]{0, -1, 1, 100, -100, 0L, -1L, 1L, 100L, -100L, "0", "-1", "1", "100", "-100", 4294967296L, -4294967297L, "4294967296", "-4294967297",};
        for (Object longVal : longVals) {
            long longVal2;
            if (longVal instanceof String)
                longVal2 = Long.parseLong((String) longVal);
            else if (longVal instanceof Integer)
                longVal2 = ((Integer) longVal).longValue();
            else
                longVal2 = ((Long) longVal);

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongLe:condition," + (longVal2 - 1) + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongLe:condition," + (longVal2 - 2) + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongLe:condition," + longVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongLe:condition," + (longVal2 + 1) + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfLongLe 条件参数类型错误导致的条件不成立的情况
        notLongVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Byte((byte) 1), new Short((short) 1), new Object[0], new long[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是长整型也不是长整型字符串
        for (Object notLongVal : notLongVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongLe:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongLe:condition,1|IntEq:1", null});

        }

        // IfLongIn 条件不成立
        Object[] intNotInVals = new Object[]{-13, 13, 123, -123, "-13", "13", "123", "-123", 4294967297L, -4294967298L, "4294967297", "-4294967298",};
        for (Object intNotInVal : intNotInVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intNotInVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intNotInVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});
        }
        // IfLongIn 条件成立
        longVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100", 4294967296L, -4294967297L, "4294967296", "-4294967297",};
        for (Object longVal : longVals) {

            //条件成立+验证通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", longVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfLongIn 条件参数类型错误导致的条件不成立的情况
        notLongVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Byte((byte) 1), new Short((short) 1), new Object[0], new long[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是长整型也不是长整型字符串
        for (Object notLongVal : notLongVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});

        }

        // IfLongNotIn 条件不成立
        Object[] intInVals = new Object[]{0, -1, 1, 100, -100, "0", "-1", "1", "100", "-100", 4294967296L, -4294967297L, "4294967296", "-4294967297",};
        for (Object intInVal : intInVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intInVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongNotIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", intInVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfLongNotIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});
        }
        // IfLongNotIn 条件成立
        intNotInVals = new Object[]{-13, 13, 123, -123, "-13", "13", "123", "-123", 4294967297L, -4294967298L, "4294967297", "-4294967298",};
        for (Object intNotInVal : intNotInVals) {

            //条件成立+验证通过
            params.clear();
            params.put("condition", intNotInVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongNotIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", intNotInVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongNotIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfLongNotIn 条件参数类型错误导致的条件不成立的情况
        notLongVals = new Object[]{true, false, 1.0, 0.0, "1.0", "0.0", "", "abc", new Byte((byte) 1), new Short((short) 1), new Object[0], new long[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是长整型也不是长整型字符串
        for (Object notLongVal : notLongVals) {

            //条件成立+验证通过
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfLongNotIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", notLongVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfLongNotIn:condition,-4294967297,-100,-1,0,1,100,4294967296|IntEq:1", null});
            }, "必须等于 1");

        }

    }

    public void testValidateIfStr() throws Exception {
        HashMap<String, Object> params = new HashMap<>();
        Object[] strVals;
        Object[] notStrVals;

        // IfStrEq
        strVals = new Object[]{"", "a", "0", "-1", "1", "100", "-100", "abc", "1.0"};
        for (Object strVal : strVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrEq:condition," + strVal + 'p' + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrEq:condition," + strVal + '0' + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrEq:condition," + strVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrEq:condition," + strVal + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfStrEq 条件参数类型错误导致的条件不成立的情况
        notStrVals = new Object[]{true, false, 1, 0, 1.0, 0.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是字符串型
        for (Object notStrVal : notStrVals) {
            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrEq:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrEq:condition,1|IntEq:1", null});
        }

        // IfStrNe
        strVals = new Object[]{"", "a", "0", "-1", "1", "100", "-100", "abc", "1.0"};
        for (Object strVal : strVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrNe:condition," + strVal + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrNe:condition," + strVal + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrNe:condition," + strVal + 'p' + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrNe:condition," + strVal + '0' + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfStrNe 条件参数类型错误导致的条件不成立的情况
        notStrVals = new Object[]{true, false, 1, 0, 1.0, 0.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是字符串型
        for (Object notStrVal : notStrVals) {
            //条件成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrNe:condition,0|IntEq:1", null});

            //条件成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrNe:condition,1|IntEq:1", null});
            }, "必须等于 1");
        }

        // IfStrGt
        strVals = new Object[]{"", "a", "0", "-1", "1", "100", "-100", "abc", "1.0"};
        for (Object strVal : strVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrGt:condition," + strVal + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrGt:condition," + strVal + '0' + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", "" + strVal + 'p');
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrGt:condition," + strVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", "" + strVal + 'a');
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrGt:condition," + strVal + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfStrGt 条件参数类型错误导致的条件不成立的情况
        notStrVals = new Object[]{true, false, 1, 0, 1.0, 0.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是字符串型
        for (Object notStrVal : notStrVals) {
            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrGt:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrGt:condition,1|IntEq:1", null});
        }

        // IfStrGe
        strVals = new Object[]{"", "a", "0", "-1", "1", "100", "-100", "abc", "1.0"};
        for (Object strVal : strVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrGe:condition," + strVal + 'a' + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrGe:condition," + strVal + '0' + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrGe:condition," + strVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", "" + strVal + 'a');
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrGe:condition," + strVal + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfStrGe 条件参数类型错误导致的条件不成立的情况
        notStrVals = new Object[]{true, false, 1, 0, 1.0, 0.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是字符串型
        for (Object notStrVal : notStrVals) {
            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrGe:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrGe:condition,1|IntEq:1", null});
        }

        // IfStrLt
        strVals = new Object[]{"", "a", "0", "-1", "1", "100", "-100", "abc", "1.0"};
        for (Object strVal : strVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrLt:condition," + strVal + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", "" + strVal + '0');
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrLt:condition," + strVal + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrLt:condition," + strVal + 'p' + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrLt:condition," + strVal + 'a' + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfStrLt 条件参数类型错误导致的条件不成立的情况
        notStrVals = new Object[]{true, false, 1, 0, 1.0, 0.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是字符串型
        for (Object notStrVal : notStrVals) {
            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrLt:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrLt:condition,1|IntEq:1", null});
        }

        // IfStrLe
        strVals = new Object[]{"", "a", "0", "-1", "1", "100", "-100", "abc", "1.0"};
        for (Object strVal : strVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", "" + strVal + 'a');
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrLe:condition," + strVal + "|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", "" + strVal + '0');
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrLe:condition," + strVal + "|IntEq:1", null});

            //条件成立+验证通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrLe:condition," + strVal + "|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrLe:condition," + strVal + 'a' + "|IntEq:1", null});
            }, "必须等于 1");
        }
        // IfStrLe 条件参数类型错误导致的条件不成立的情况
        notStrVals = new Object[]{true, false, 1, 0, 1.0, 0.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是字符串型
        for (Object notStrVal : notStrVals) {
            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrLe:condition,0|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrLe:condition,1|IntEq:1", null});
        }

        // IfStrIn 条件不成立
        strVals = new Object[]{"hello", "world", "-13", "13", "-123", "123"};
        for (Object strVal : strVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrIn:condition,,abc,-100,-1,0,1,100|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrIn:condition,abc,-100,-1,0,1,100,|IntEq:1", null});
        }
        //条件不成立+验证通过（此条检测会被忽略）
        params.clear();
        params.put("condition", "");
        params.put("param", 1);
        Validation.validate(params, new String[]{"param", "IfStrIn:condition,abc,-100,-1,0,1,100|IntEq:1", null});
        //条件不成立+验证不通过（此条检测会被忽略）
        params.clear();
        params.put("condition", "");
        params.put("param", 0);
        Validation.validate(params, new String[]{"param", "IfStrIn:condition,abc,-100,-1,0,1,100|IntEq:1", null});
        // IfStrIn 条件成立
        strVals = new Object[]{"", "abc", "0", "-1", "1", "100", "-100"};
        for (Object strVal : strVals) {
            //条件成立+验证通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrIn:condition,,abc,-100,-1,0,1,100|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrIn:condition,abc,-100,-1,0,1,100,|IntEq:1", null});
            }, "必须等于 1");
        }
        //条件成立+验证通过
        params.clear();
        params.put("condition", "");
        params.put("param", 1);
        Validation.validate(params, new String[]{"param", "IfStrIn:condition,|IntEq:1", null});
        //条件成立+验证不通过
        params.clear();
        params.put("condition", "");
        params.put("param", 0);
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"param", "IfStrIn:condition,,|IntEq:1", null});
        }, "必须等于 1");
        // IfStrIn 条件参数类型错误导致的条件不成立的情况
        notStrVals = new Object[]{true, false, 1, 0, 1.0, 0.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是字符串型
        for (Object notStrVal : notStrVals) {
            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrIn:condition,,abc,-100,-1,0,1,100|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrIn:condition,abc,-100,-1,0,1,100,1|IntEq:1", null});
        }

        // IfStrNotIn 条件不成立
        strVals = new Object[]{"", "abc", "0", "-1", "1", "100", "-100"};
        for (Object strVal : strVals) {

            //条件不成立+验证通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,,abc,-100,-1,0,1,100|IntEq:1", null});

            //条件不成立+验证不通过（此条检测会被忽略）
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,abc,-100,-1,0,1,100,|IntEq:1", null});
        }
        //条件不成立+验证通过（此条检测会被忽略）
        params.clear();
        params.put("condition", "");
        params.put("param", 1);
        Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,|IntEq:1", null});
        //条件不成立+验证不通过（此条检测会被忽略）
        params.clear();
        params.put("condition", "");
        params.put("param", 0);
        Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,,|IntEq:1", null});
        // IfStrNotIn 条件成立
        strVals = new Object[]{"hello", "world", "-13", "13", "-123", "123"};
        for (Object strVal : strVals) {
            //条件成立+验证通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,,abc,-100,-1,0,1,100|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", strVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,abc,-100,-1,0,1,100,|IntEq:1", null});
            }, "必须等于 1");
        }
        //条件成立+验证通过
        params.clear();
        params.put("condition", "");
        params.put("param", 1);
        Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,abc,-100,-1,0,1,100|IntEq:1", null});
        //条件成立+验证不通过
        params.clear();
        params.put("condition", "");
        params.put("param", 0);
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,abc,-100,-1,0,1,100|IntEq:1", null});
        }, "必须等于 1");
        // IfStrNotIn 条件参数类型错误导致的条件不成立的情况
        notStrVals = new Object[]{true, false, 1, 0, 1.0, 0.0, new Object[0], new int[]{1, 2, 3}, new HashMap<String, Object>()}; // 不是字符串型
        for (Object notStrVal : notStrVals) {
            //条件成立+验证通过
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 1);
            Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,,abc,-100,-1,0,1,100|IntEq:1", null});

            //条件成立+验证不通过
            params.clear();
            params.put("condition", notStrVal);
            params.put("param", 0);
            _assertThrowExpectionContainErrorString(() -> {
                Validation.validate(params, new String[]{"param", "IfStrNotIn:condition,abc,-100,-1,0,1,100,1|IntEq:1", null});
            }, "必须等于 1");
        }

    }

    public void testValidateIfParamExistence() throws Exception {
        HashMap<String, Object> params = new HashMap<>();

        // ignoreRequired=false + 条件参数不存在 + 参数存在 -> 应该抛出异常
        params.clear();
        params.put("param", 1);
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"param", "IfStrEq:condition,1|IntEq:1", null}, false);
        }, "必须提供条件参数“condition”，因为“param”的验证依赖它");

        // ignoreRequired=false + 条件参数不存在 + 参数不存在 -> 应该抛出异常
        params.clear();
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"param", "IfStrEq:condition,1|IntEq:1", null}, false);
        }, "必须提供条件参数“condition”，因为“param”的验证依赖它");

        // ignoreRequired=true + 条件参数不存在 + 参数存在 -> 应该抛出异常
        params.clear();
        params.put("param", 1);
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, new String[]{"param", "IfStrEq:condition,1|IntEq:1", null}, true);
        }, "必须提供条件参数“condition”，因为“param”的验证依赖它");

        // ignoreRequired=true + 条件参数不存在 + 参数不存在 -> 无需检测该参数
        params.clear();
        Validation.validate(params, new String[]{"param", "IfStrEq:condition,1|IntEq:1", null}, true);

    }

    public void testValidateIf() throws Exception {
        HashMap<String, Object> articleInfo = new HashMap<>();
        articleInfo.put("type", 1); // 1-普通文章, 2-用户投诉
        articleInfo.put("title", "WebGeeker Validation");
        articleInfo.put("content", "WebGeeker Validation 是一个非常强大的参数验证工具, 能够验证无限嵌套的数据结构");
        articleInfo.put("state", 0);

        HashMap<String, Object> complaintInfo = new HashMap<>();
        complaintInfo.put("type", 2); // 1-普通文章, 2-用户投诉
        complaintInfo.put("title", "客服（10000）的服务太差了");
        complaintInfo.put("content", "客服（10000）的服务太差了, 我要投诉他, 砸他饭碗");
        complaintInfo.put("state", 1); // 0-待处理, 1-处理中, 2-已处理

        String[] validations = new String[]{
            "type", "Required|IntIn:1,2", null,
            "title", "Required|StrLenGeLe:2,100", null,
            "content", "Required|StrLenGe:1|StrLenLe:10000000", null,
            "state", "IfIntEq:type,1|IntEq:0"/*检测 type==1 普通文章*/, "IfIntEq:type,2|Required|IntIn:0,1,2"/*检测 type==2 用户投诉*/, null,
        };

        Validation.validate(articleInfo, validations);
        Validation.validate(complaintInfo, validations);

        String[] validations2 = new String[]{
            "article.type", "Required|IntIn:1,2", null,
            "article.title", "Required|StrLenGeLe:2,100", null,
            "article.content", "Required|StrLenGe:1|StrLenLe:10000000", null,
            "article.state",
                "IfIntEq:.type,1|IntEq:0"/*检测 type==1 普通文章*/,
                "IfIntEq:article.type,2|Required|IntIn:0,1,2"/*检测 type==2 用户投诉*/,
            null,
        };
        Validation.validate(new HashMap<String, Object>(){{put("article", articleInfo);}}, validations2);
        Validation.validate(new HashMap<String, Object>(){{put("article", complaintInfo);}}, validations2);

        // If验证器的参数为嵌套的参数
        HashMap<String, Object> setting = new HashMap<String, Object>() {{
            put("flags", new Integer[]{
                1,  // 是否绑定了手机
                1,  // 是否绑定了邮箱
                1,  // 是否绑定了支付宝
            });
        }};
        HashMap<String, Object> user = new HashMap<String, Object>() {{
            put("name", "hello");
            put("setting", setting);
            put("phone", "18812340001");
            put("email", "18812340001@163.com");
            put("alipay", "18812340001@alipay.com");
        }};
        HashMap<String, Object> params = new HashMap<String, Object>() {{
            put("user", user);
        }};
        String[] validations3 = new String[]{
            "user.phone", "If:user.setting.flags[0]|Required|StrLen:11", null,
            "user.email", "If:user.setting.flags[1]|Required|StrLenGeLe:1,100", null,
            "user.alipay", "If:user.setting.flags[2]|Required|StrLenGeLe:1,100", null,
        };
        Validation.validate(params, validations3);
        setting.put("flags", new Integer[]{1, 1}); // If 条件参数的不存在
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, validations3);
        }, "必须提供条件参数“user.setting.flags[2]”，因为“user.alipay”的验证依赖它");
        user.remove("email"); // 参数的不存在
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, validations3);
        }, "必须提供“user.email”");
        setting.remove("flags"); // If 条件参数的上一级不存在
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, validations3);
        }, "必须提供条件参数“user.setting.flags[0]”，因为“user.phone”的验证依赖它");
        user.remove("setting"); // If 条件参数的上上级不存在
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, validations3);
        }, "必须提供条件参数“user.setting.flags[0]”，因为“user.phone”的验证依赖它");
        user.put("setting", "abc"); // If 条件参数的上上级不是map
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, validations3);
        }, "“user.setting”必须是 Map<String, Object>");
        user.put("setting", setting);
        setting.put("flags", "abc");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(params, validations3);
        }, "“user.setting.flags”必须是数组或List");
        // If 条件参数的类型错误, 应该算条件不成立, 忽略
        setting.put("flags", new Object[]{"abc", 1.0, new int[0]});
        Validation.validate(params, validations3);
        user.remove("alipay");
        Validation.validate(params, validations3);
        user.remove("phone");
        Validation.validate(params, validations3);

        // 多个If串联
        Validation.validate(new HashMap<String, Object>() {{
            put("cond", 1);
            put("param", 2);
        }}, new String[]{"params", "IfIntGe:cond,1|IfIntLe:cond,1|IntGe:2", null,});

        // If验证器位置不对
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>() {{
                put("cond", 1);
                put("param", 2);
            }}, new String[]{"params", "IfIntGe:cond,1|IntGe:2|IfIntLe:cond,1", null,});
        }, "条件验证器 IfXxx 只能出现在验证规则的开头");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>() {{
                put("cond", 1);
                put("param", 2);
            }}, new String[]{"params", "IntGe:2|IfIntGe:cond,1|IfIntLe:cond,1", null,});
        }, "条件验证器 IfXxx 只能出现在验证规则的开头");

        // 以下测试主要是为了完善测试覆盖率
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>() {{
                put("cond", 1);
                put("param", 2);
            }}, new String[]{"params", "If:cond[*]|Int", null,});
        }, "IfXxx中的条件参数“cond[*]”中不得包含*号");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>() {{
                put("cond", new Integer[]{1, 2});
                put("param", 2);
            }}, new String[]{"param", "IfIntEq:cond[1],2|IntEq:3", null,});
        }, "“param”必须等于 3");

    }

    public void testValidateSample1() throws Exception {
        //验证规则
        String[] validations = new String[]{
            "offset", "IntGe:0", null, // 参数offset应该大于等于0
            "count", "Required|IntGeLe:1,200", null, // 参数count是必需的且大于等于1小于等于200
            "type", "IntIn:1,2", null, // 参数type可取值为: 1, 2
            "state", // 参数 state 有两条验证，这两条验证规则是或的关系
            "IfIntEq:type,1|IntEq:0", // 如果type==1（批评建议），那么参数state只能是0
            "IfIntEq:type,2|IntIn:0,1,2", // 如果type==2（用户投诉），那么参数state可取值为: 1, 2, 3
            null,
            "search.keyword", "StrLenGeLe:1,100", null, // search.keyword 应该是一个长度在[1, 100]之间的字符串
//            "search.start_time", "Date", null, // search.start_time 应该是一个包含合法日期的字符串
//            "search.end_time", "Date", null, // search.end_time 应该是一个包含合法日期的字符串
        };

        // 待验证参数
        HashMap<String, Object> params = new HashMap<String, Object>() {{
            put("offset", 0); // 从第0条记录开始
            put("count", 10); // 最多返回10条记录
            put("type", 2); // 1-批评建议, 2-用户投诉
            put("state", 0); // 0-待处理, 1-处理中, 2-已处理
            put("search", new HashMap<String, Object>() {{ // 搜索条件
                put("keyword", "硬件故障"); // 关键字
                put("start_time", "2018-01-01"); // 起始日期
                put("end_time", "2018-01-31"); // 结束日期
            }});
        }};

        // 验证（如果验证不通过，会抛出异常）
        Validation.validate(params, validations);
    }

    public void testValidate() throws Exception {

        Validation.validate(new HashMap<String, Object>(), new String[0]); // 没有验证规则
        Validation.validate(new HashMap<String, Object>(), new String[]{"abc", "", null}); // 验证规则为""
        Validation.validate(new HashMap<String, Object>(){{put("abc", 1);}}, new String[]{"abc", "", null}); // 验证规则为""
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"param"});
        }, "参数validations格式错误");

        String paramsJson = "{\n" +
            "    \"id\": 1,\n" +
            "    \"title\": \"WebGeeker Validation\",\n" +
            "    \"content\": \"WebGeeker Validation \\u662f\\u4e00\\u4e2a\\u975e\\u5e38\\u5f3a\\u5927\\u7684\\u53c2\\u6570\\u9a8c\\u8bc1\\u5de5\\u5177, \\u80fd\\u591f\\u9a8c\\u8bc1\\u65e0\\u9650\\u5d4c\\u5957\\u7684\\u6570\\u636e\\u7ed3\\u6784\",\n" +
            "    \"timestamp\": 1491127037.37,\n" +
            "    \"contentType\": 0,\n" +
            "    \"author\": {\n" +
            "        \"id\": 1,\n" +
            "        \"username\": \"photondragon\",\n" +
            "        \"nickname\": \"\\u8ff7\\u9014\\u8001\\u7801\",\n" +
            "        \"email\": \"photondragon@163.com\"\n" +
            "    },\n" +
            "    \"comments\": [\n" +
            "        {\n" +
            "            \"content\": \"webgeeker\\/validation \\u68d2\\u68d2\\u54d2\",\n" +
            "            \"author\": {\n" +
            "                \"email\": \"admin@webgeeker.com\",\n" +
            "                \"nickname\": \"\\u963f\\u8fbe\\u660e\"\n" +
            "            }\n" +
            "        },\n" +
            "        {\n" +
            "            \"content\": \"webgeeker\\/validation is amazing!\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";
        /* params的数据结构(PHP语法)为:
        [
            'id' => 1,
            'title' => 'WebGeeker Validation',
            'content' => 'WebGeeker Validation 是一个非常强大的参数验证工具, 能够验证无限嵌套的数据结构',
            'timestamp' => 1491127037.37,
            'contentType' => 0, // 内容类型. 0-html, 1-txt, 2-markdown
            'author' => [
                'id' => 1,
                'username' => 'photondragon',
                'nickname' => '迷途老码',
                'email' => 'photondragon@163.com',
            ],
            'comments' => [
                [
                    'content' => 'webgeeker/validation 棒棒哒',
                    'author' => [
                        'email' => 'admin@webgeeker.com',
                        'nickname' => '阿达明',
                    ],
                ],
                [
                    'content' => 'webgeeker/validation is amazing!',
                ],
            ],
        ]*/
        HashMap<String, Object> params = new HashMap<>();
        params.putAll(new JSONObject(paramsJson).toMap());

        String[] validations = new String[]{
            "id", "Required|IntGt:0", null,
            "title", "Required|StrLenGeLe:2,100", null,
            "content", "Required|StrLenGe:1|StrLenLe:10000000", null,
            "timestamp", "FloatGt:0", null,
            "contentType", "Required|IntIn:0,1,2", null,
            "author", "Required|Map", null,
            "author.id", "Required|IntGt:0", null,
            "author.username", "Required|StrLenGe:4|Regexp:/^[a-zA-Z0-9]+$/", null,
            "author.nickname", "StrLenGe:0", null,
            "author.email", "Regexp:/^[a-zA-Z0-9]+@[a-zA-Z0-9-]+.[a-z]+$/", null,
            "comments", "Arr", null,
            "comments[*]", "Map", null,
            "comments[*].content", "Required|StrLenGe:8", null,
            "comments[*].author", "Map", null,
            "comments[*].author.email", "Regexp:/^[a-zA-Z0-9]+@[a-zA-Z0-9-]+.[a-z]+$/", null,
            "comments[*].author.nickname", "StrLenGe:0", null,
            "visitors", "List", null,
            "visitors[*]", "Map", null,
            "visitors[*].id", "Required|IntGt:0", null,
        };

        Validation.validate(params, new String[0]);
        Validation.validate(params, validations);

        // ignore Required
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(){{
                put("content", null);
            }}, new String[]{"content", "Required|StrLenLe:20", null});
        }, "必须提供“content”");
        Validation.validate(new HashMap<String, Object>(){{
            put("content", null);
        }}, new String[]{"content", "Required|StrLenLe:20", null}, true);

        // 纯粹为了提高测试覆盖率
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validateValue(123, null);
        }, "没有提供验证规则");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validateValue(123, new String[0]);
        }, "没有提供验证规则");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validateValue(123, new String[]{"Haha:3"});
        }, "未知的验证器\"Haha\"");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validateValue(123, new String[]{":pending"});
        }, "“:pending”中的':'号前面没有验证器");
    }

    public void testValidateKeyPath() throws Exception {

        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"1abc", "", null});
        }, "参数名称“1abc”不得以数字开头");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"1abc.def", "", null});
        }, "“1abc.def”中包含了以数字开头的参数名称“1abc”");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"abc.34ab", "", null});
        }, "“abc.34ab”中包含了以数字开头的参数名称“34ab”");

        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"[*]", "Map", null});
        }, "“[*]”中'['号前面没有参数名称");

        Validation.validate(new HashMap<String, Object>(), new String[]{"numbers[*]", "Int", null});
        Validation.validate(new HashMap<String, Object>(), new String[]{"numbers[0]", "Int", null});
        Validation.validate(new HashMap<String, Object>(), new String[]{"numbers[0][1]", "Int", null});
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"comments[abc]", "Map", null});
        }, "“comments[abc]”中的方括号[]之间只能包含'*'号或数字");

        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"matrix[*]abc[*]", "Int", null});
        }, "“matrix[*]abc[*]”中的“[*]”之后包含非法字符");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"matrix[*]abc", "Int", null});
        }, "“matrix[*]abc”中的“[*]”之后包含非法字符");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"matrix[*", "Int", null});
        }, "“matrix[*”中的'['号之后缺少']'");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"matrix[*][*", "Int", null});
        }, "“matrix[*][*”中的'['号之后缺少']'");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"matrix[*[*]", "Int", null});
        }, "“matrix[*[*]”中的方括号[]之间只能包含'*'号或数字");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"matrix[*][aaa]", "Int", null});
        }, "“matrix[*][aaa]”中的方括号[]之间只能包含*号或数字");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"matrix[0][-110]", "Int", null});
        }, "非法的参数名称“matrix[0][-110]”");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"", "Int", null});
        }, "参数validations中包含空的参数名称");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{".", "Int", null});
        }, "“.”中包含空的参数名称");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"abc..def", "Int", null});
        }, "“abc..def”中包含空的参数名称");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"abc*", "Int", null});
        }, "“abc*”中'*'号只能处于方括号[]中");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"abc]", "Int", null});
        }, "“abc]”中包含了非法的']'号");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"abc]*[", "Int", null});
        }, "“abc]*[”中'[', ']'顺序颠倒了");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"abc*[*]", "Int", null});
        }, "“abc*[*]”中包含了非法的'*'号");
        _assertThrowExpectionContainErrorString(() -> {
            Validation.validate(new HashMap<String, Object>(), new String[]{"3abc[*]", "Int", null});
        }, "“3abc[*]”中包含了以数字开头的参数名称“3abc”");

    }

}