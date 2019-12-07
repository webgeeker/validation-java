# WebGeeker-Validation for Java

一个强大的 Java 参数验证器，用于对API接口的请求参数进行合法性检查。

在实现服务端的API接口时，对于每一个接口的每一个参数，都应该检测其取值是否合法，以免错误的数据输入到系统中。这个工作可以说是费时费力，但又不得不做。

本工具就是针对这个工作而设计的，能够有效地减少编码量，代码可读性好。

看看下面这段代码，可以对用法有个大概印象，应该不难看懂：
```java
HashMap<String, Object> params = Validation.getParameters(request);

// 验证（如果验证不通过，会抛出异常）
Validation.validate(params, new String[] {
    "offset", "Required|IntGe:0", null,  // 参数"offset"必须是大于等于0的整数
    "count", "Required|IntGtLe:0,1000", null,  // 参数"count"是必需的且取值在 1 - 200 之间
});
```

支持多种数据类型的校验：整型、浮点型、bool型、字符串、数组、List、Map，能够验证嵌套的数据结构中的参数，还支持带条件判断的验证。

- 目录
  * [1 简介](#1-%E7%AE%80%E4%BB%8B)
    + [1.1 为什么要写这样一个工具?](#11-%E4%B8%BA%E4%BB%80%E4%B9%88%E8%A6%81%E5%86%99%E8%BF%99%E6%A0%B7%E4%B8%80%E4%B8%AA%E5%B7%A5%E5%85%B7)
    + [1.2 特点](#12-%E7%89%B9%E7%82%B9)
    + [1.3 一个简单示例](#13-%E4%B8%80%E4%B8%AA%E7%AE%80%E5%8D%95%E7%A4%BA%E4%BE%8B)
  * [2 安装](#2-%E5%AE%89%E8%A3%85)
  * [3 快速上手](#3-%E5%BF%AB%E9%80%9F%E4%B8%8A%E6%89%8B)
    + [3.1 一个完整的示例](#31-%E4%B8%80%E4%B8%AA%E5%AE%8C%E6%95%B4%E7%9A%84%E7%A4%BA%E4%BE%8B)
    + [3.2 验证不通过的错误处理](#32-%E9%AA%8C%E8%AF%81%E4%B8%8D%E9%80%9A%E8%BF%87%E7%9A%84%E9%94%99%E8%AF%AF%E5%A4%84%E7%90%86)
  * [4 详细使用方法](#4-%E8%AF%A6%E7%BB%86%E4%BD%BF%E7%94%A8%E6%96%B9%E6%B3%95)
    + [4.1 验证整型参数](#41-%E9%AA%8C%E8%AF%81%E6%95%B4%E5%9E%8B%E5%8F%82%E6%95%B0)
    + [4.2 验证浮点型参数](#42-%E9%AA%8C%E8%AF%81%E6%B5%AE%E7%82%B9%E5%9E%8B%E5%8F%82%E6%95%B0)
    + [4.3 验证bool型参数](#43-%E9%AA%8C%E8%AF%81bool%E5%9E%8B%E5%8F%82%E6%95%B0)
    + [4.4 验证字符串型参数](#44-%E9%AA%8C%E8%AF%81%E5%AD%97%E7%AC%A6%E4%B8%B2%E5%9E%8B%E5%8F%82%E6%95%B0)
    + [4.5 验证数组型、Map型、文件型、日期时间型参数](#45-%E9%AA%8C%E8%AF%81%E6%95%B0%E7%BB%84%E5%9E%8Bmap%E5%9E%8B%E6%96%87%E4%BB%B6%E5%9E%8B%E6%97%A5%E6%9C%9F%E6%97%B6%E9%97%B4%E5%9E%8B%E5%8F%82%E6%95%B0)
    + [4.6 验证器串联（与）](#46-%E9%AA%8C%E8%AF%81%E5%99%A8%E4%B8%B2%E8%81%94%E4%B8%8E)
    + [4.7 Required 验证器](#47-required-%E9%AA%8C%E8%AF%81%E5%99%A8)
    + [4.8 忽略所有 Required 验证器](#48-%E5%BF%BD%E7%95%A5%E6%89%80%E6%9C%89-required-%E9%AA%8C%E8%AF%81%E5%99%A8)
    + [4.9 嵌套参数的验证](#49-%E5%B5%8C%E5%A5%97%E5%8F%82%E6%95%B0%E7%9A%84%E9%AA%8C%E8%AF%81)
    + [4.10 条件判断型验证器](#410-%E6%9D%A1%E4%BB%B6%E5%88%A4%E6%96%AD%E5%9E%8B%E9%AA%8C%E8%AF%81%E5%99%A8)
    + [4.11 验证规则并联（或）](#411-%E9%AA%8C%E8%AF%81%E8%A7%84%E5%88%99%E5%B9%B6%E8%81%94%E6%88%96)
    + [4.12 关于特殊值`null`, `""`，`0`，`false`的问题](#412-%E5%85%B3%E4%BA%8E%E7%89%B9%E6%AE%8A%E5%80%BCnull-0false%E7%9A%84%E9%97%AE%E9%A2%98)
    + [4.13 关于基本数据类型与字符串的关系](#413-%E5%85%B3%E4%BA%8E%E5%9F%BA%E6%9C%AC%E6%95%B0%E6%8D%AE%E7%B1%BB%E5%9E%8B%E4%B8%8E%E5%AD%97%E7%AC%A6%E4%B8%B2%E7%9A%84%E5%85%B3%E7%B3%BB)
    + [4.14 自定义错误信息输出文本](#414-%E8%87%AA%E5%AE%9A%E4%B9%89%E9%94%99%E8%AF%AF%E4%BF%A1%E6%81%AF%E8%BE%93%E5%87%BA%E6%96%87%E6%9C%AC)
    + [4.15 国际化](#415-%E5%9B%BD%E9%99%85%E5%8C%96)
  * [A 附录 - 验证器列表](#a-%E9%99%84%E5%BD%95---%E9%AA%8C%E8%AF%81%E5%99%A8%E5%88%97%E8%A1%A8)
    + [A.1 整型](#a1-%E6%95%B4%E5%9E%8B)
    + [A.2 浮点型](#a2-%E6%B5%AE%E7%82%B9%E5%9E%8B)
    + [A.3 bool型](#a3-bool%E5%9E%8B)
    + [A.4 字符串型](#a4-%E5%AD%97%E7%AC%A6%E4%B8%B2%E5%9E%8B)
    + [A.5 数组型](#a5-%E6%95%B0%E7%BB%84%E5%9E%8B)
    + [A.6 Map型](#a6-%E5%AF%B9%E8%B1%A1%E5%9E%8B)
    + [A.7 文件型](#a7-%E6%96%87%E4%BB%B6%E5%9E%8B)
    + [A.8 日期和时间型](#a8-%E6%97%A5%E6%9C%9F%E5%92%8C%E6%97%B6%E9%97%B4%E5%9E%8B)
    + [A.9 条件判断型](#a9-%E6%9D%A1%E4%BB%B6%E5%88%A4%E6%96%AD%E5%9E%8B)
    + [A.10 其它验证器](#a10-%E5%85%B6%E5%AE%83%E9%AA%8C%E8%AF%81%E5%99%A8)

## 1 简介

### 1.1 为什么要写这样一个工具?

方便: 使用本工具验证从头到尾只需要调用一个函数, 就是:
```java
public static HashMap<String, Object> validate(HashMap<String, Object> params, String[] validations, boolean ignoreRequired);
```
灵活

适用广泛: 绝大多数API参数验证需要都能满足

易学易记: 所有验证器按数据类型分类, 比如整型验证器都是以"Int"开头，浮点型验证器都是以"Float"开头, 完全可以轻松背下大部分验证器.

### 1.2 特点
1. 每个功能特性都有单元测试（共有 600+ assertions）
1. 支持无限嵌套的数据结构的验证（参考 1.3 节的例子）
1. 支持条件验证，根据参数取值不同，应用不同的验证规则（参考 1.3 节的例子）
1. 支持正则表达式验证
1. 简洁，验证逻辑一目了然
1. 轻量，不需要定义和维护各种验证类
1. 验证器语义明确，没有“一词多义”的问题
1. 易学易记。比如整型验证器都是以"Int"开头，浮点型验证器都是以"Float"开头，等等。唯一不符合这一规则的是字符串型验证器，它们一部分以"Str"开头的，但也有一部分不以"Str"开头，比如`Regexp`, `Ip`, `Email`, `Url`等。
1. 不绑定任何一个框架，不依赖任何一个第三方库。你可以在任何一个框架中使用这个工具。

### 1.3 一个简单示例

下面这个示例展示了一个查询获取用户投诉列表的Request参数的验证（注意其中的*条件验证*和针对*嵌套数据结构的验证*）：

```java
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
    "search.start_time", "Date", null, // search.start_time 应该是一个包含合法日期的字符串
    "search.end_time", "Date", null, // search.end_time 应该是一个包含合法日期的字符串
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
```

## 2 安装

在 pom.xml 中添加 maven 依赖
```
<dependency>
    <groupId>com.webgeeker</groupId>
    <artifactId>validation</artifactId>
    <version>0.1.0</version>
</dependency>
```

项目的 maven 仓库地址:  
https://mvnrepository.com/artifact/com.webgeeker/validation

## 3 快速上手

### 3.1 一个完整的示例

本工具主要用于验证API接口的参数，下面的例子是一个RestController，实现了一个分页获取文章的接口:  
/Article/Page?offset=0&count=10

```java
package com.webgeeker.demo;

import app.Service.Validation.Validation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
public class ArticleController {

    @RequestMapping("Article/Page")
    public Object getPage(HttpServletRequest request) throws Exception {

        HashMap<String, Object> params = Validation.getParameters(request);

        Validation.validate(params, new String[] {
            "offset", "Required|IntGe:0", null, // 参数offset是必需的且大于等于0
            "count", "Required|IntGtLe:0,1000", null, // 参数count是必需的且大于0小于等于1000
        });

        // 此处省略数据查询...

        return new HashMap<String, Object>() {{ // 假设查询到的数据为空
            put("total", 0);
            put("Articles", new ArrayList<>());
        }};
    }
}
```
*注意*：验证不通过会抛出异常，该异常中包含有错误描述信息

### 3.2 验证不通过的错误处理

如果验证不通过，`Validation::validate(...)`方法会抛出异常，建议在框架层面统一捕获这些异常，提取错误描述信息并返回给客户端。

## 4 详细使用方法

### 4.1 验证整型参数

整型验证器全部以"Int"开头，用于验证整型数值（如`123`）或整型字符串（如`"123"`）。其它数据类型均不匹配。

```java
Validation.validate(params, new String[]{
    "size", "IntGeLe:1,100", null,
});
```
这条验证要求参数"size"是整数，并且大于等于1，小于等于100。

完整的整型验证器的列表参考附录 A.1 。

### 4.2 验证长整型参数

整型验证器全部以"Long"开头，用于验证整型数值（如`123`）、整型字符串（如`"123"`）、长整型数值（如`4294967296L`）、长整型字符串（如`"4294967296"`）。其它数据类型均不匹配。

```java
Validation.validate(params, new String[]{
    "money", "LongGeLe:5000000000,10000000000", null,
});
```
这条验证要求参数"money"是整数，并且大于等于5000000000，小于等于10000000000。

完整的长整型验证器的列表参考附录 A.2 。

### 4.3 验证浮点型参数

浮点型验证器全部以"Float"开头，用于验证浮点型数值（如`1.0`）、浮点型字符串（如`"1.0"`）、整型数值（如`123`）或整型字符串（如`"123"`）。其它数据类型均不匹配。

*注意*: FloatXxx不区分单精度和双精度, 一率按双精度浮点数来处理.

```java
Validation.validate(params, new String[]{
    "height", "FloatGeLe:0.0,100.0", null,
});
```
这条验证要求参数"height"是浮点数，并且大于等于0，小于等于100.0。

完整的浮点型验证器的列表参考附录 A.3 。

### 4.4 验证bool型参数

bool型验证器：
* Bool: 合法的取值为: `true`, `false`, `"true"`, `"false"`（字符串忽略大小写）。
* BoolTrue: 合法的取值为: `true`, `"true"`（字符串忽略大小写）。
* BoolFalse: 合法的取值为: `false`, `"false"`（字符串忽略大小写）。
* BoolSmart: 合法的取值为: `true`, `false`, `"true"`, `"false"`, `1`, `0`, `"1"`, `"0"`, `"yes"`, `"no"`, `"y"`, `"n"`（字符串忽略大小写）
* BoolSmartTrue: 合法的取值为: `true`, `"true"`, `1`, `"1"`, `"yes"`, `"y"`（字符串忽略大小写）
* BoolSmartFalse: 合法的取值为: `false`, `"false"`, `0`, `"0"`, `"no"`, `"n"`（字符串忽略大小写）

例
```java
Validation.validate(params, new String[]{
    "accept", "BoolSmart", null,
});
```

完整的bool型验证器的列表参考附录 A.4 。

### 4.5 验证字符串型参数

字符串型验证器不全以"Str"开头。只接收字符串型数据，其它数据类型均不匹配。

例1：
```java
Validation.validate(params, new String[]{
    "name", "StrLenGeLe:2,20", null,
});
```
这条验证要求参数"name"是字符串，长度在2-20之间。

例2：
```java
Validation.validate(params, new String[]{
    "comment", "StrLenLe:1048576", null,
});
```
这条验证要求参数"comment"是字符串，长度不超过1048576。

例3：
```java
Validation.validate(params, new String[]{
    "email", "Email", null,
});
```
这条验证要求参数"email"是必须是合法的电子邮件地址。

例4（正则表达式验证）：
```java
Validation.validate(params, new String[]{
    "phone", "Regexp:/^1(3[0-9]|4[579]|5[0-35-9]|7[0135678]|8[0-9]|66|9[89])\d{8}$/", null,
});
```
这条验证要求参数"phone"是合法的手机号。

关于正则表达式中的哪些特殊字符需要做额外的转义的问题，只需要先用 `String.matches()` 函数验证好，如：
```
string.matches("^abc/$")
```
然后把 matches() 的参数值原封不动地拷贝出来，首尾加上`"/"`号，放在`Regexp:`后面即可。  
只有 matches() 的参数中包含`"/"`号，才需要做额外的转义；其它特殊字符均不需要做额外的转义，即使是`"|'`这种特殊符号，也不需要。  
比如上面那个matches()对应的正则验证器应该这么写：
```java
"Regexp:/^abc\\/$/"
```
而`string.matches("^(yes|no)$")`对应的验证器为`"Regexp:/^(yes|no)$/"`

完整的字符串型验证器的列表参考附录 A.5 。

### 4.6 验证数组型、Map型、文件型、日期时间型参数

参考附录A.6-A.9

### 4.7 验证器串联（与）

一条规则中可以有多个验证器前后串联，它们之间是“AND”的关系，如：
```java
Validation.validate(params, new String[]{
    "file", "FileMaxSize:10m|FileImage", null,
});
```
这个验证要求参数"file"是一个图像文件，并且文件大小不超过10m


### 4.8 Required 验证器

* Required验证器要求参数必须存在，且其值不能为`null`（这个是PHP的`null`值，而不是字符串"null"）（参数值为`null`等价于参数不存在）。
* 如果多个验证器串联，Required验证器必须在其它验证器前面。
* 如果还有条件验证器，Required必须串联在条件验证器后面。
* 如果验证规则中没有 Required，当参数存在时才进行验证，验证不通过会抛异常；如果参数不存在，那么就不验证（相当于验证通过）

例：
```java
Validation.validate(params, new String[]{
    "size", "Required|StrIn:small,middle,large", null,
});
```
该验证要求参数"size"是必需的，且只能是字符串的"small", "middle"或者"large"。

### 4.9 忽略所有 Required 验证器

比如当创建一个用户时，要求姓名、性别、年龄全部都要提供；但是当更新用户信息时，不需要提供全部信息，提供哪个信息就更新哪个信息。

```java
String[] validations = new String[]{
    "name", "Required|StrLenGeLe:2,20", null,
    "sex", "Required|IntIn:0,1", null,
    "age", "Required|IntGeLe:1,200", null,
};

HashMap<String, Object> userInfo = new HashMap<String, Object>() {{
    put("name", "tom");
    put("sex", "0");
    put("age", "10");
}};
Validation.validate(userInfo, validations); // 创建用户时的验证

userInfo.remove("age"); // 删除age字段
Validation.validate(userInfo, validations, true); // 更新用户信息时的验证
```
注意上面代码的最后一行：`validate()`函数的第三个参数为true表示忽略所有的 Required 验证器。

这样我们就只需要写一份验证规则，就可以同时用于创建用户和更新用户信息这两个接口。

### 4.10 嵌套参数的验证

下面这个例子展示了包含数组和Map的嵌套的参数的验证：
```java
String[] validations = new String[]{
    "comments[*].title", "Required|StrLenGeLe:2,50", null,
    "comments[*].content", "Required|StrLenGeLe:2,500", null,
};

HashMap<String, Object> params = new HashMap<String, Object>() {{
    put("comments", new ArrayList<HashMap<String, Object>>() {{
        add(new HashMap<String, Object>() {{
            put("title", "title 1");
            put("content", "content 1");
        }});
        add(new HashMap<String, Object>() {{
            put("title", "title 2");
            put("content", "content 2");
        }});
        add(new HashMap<String, Object>() {{
            put("title", "title 3");
            put("content", "content 3");
        }});
    }});
}};

Validation.validate(params, validations);
```

### 4.11 条件判断型验证器

条件判断型验证器都以"If"开头。

如果条件不满足，则条件验证器后面的规则都不检测，忽略当前这条验证规则。

比如你想招聘一批模特，男的要求180以上，女的要求170以上，验证可以这样写：
```java
String[] validations = new String[]{
    "sex", "StrIn:male,female", null,
    "height",
        "IfStrEq:sex,male|IntGe:180",
        "IfStrEq:sex,female|IntGe:170",
    null,
};
```
参数"sex"的值不同，参数"height"的验证规则也不一样。

除了`IfExist`和`IfNotExist`，其它的条件验证器 IfXxx 都要求*条件参数*必须存在。如果希望*条件参数*是可选的，那么可以结合`IfExist`或`IfNotExist`一起使用, 如:  
```java
"IfExist:sex|IfStrEq:sex,male|IntGe:180"
```

注意：  
设计条件验证器的主要目的是根据一个参数的取值不同，对另外一个参数应用不同的验证规则。  
"IfXxx:"的后面应该是另一个参数的名称，而不是当前参数，这一点一定要注意。  
比如上面的例子中，是根据参数"sex"的取值不同，对参数"height"应用了不同的验证规则，"IfXxx:"后面跟的是"sex"。

完整的条件判断型验证器的列表参考附录 A.10 。

### 4.12 验证规则并联（或）

多条验证规则可以并联，它们之间是“或”的关系，如
```java
String[] validations = new String[]{
    "type",
        "StrIn:small,middle,large",
        "IntIn:1,2,3",
    null,
};
```
上面这条验证要求参数"type"既可以是字符串"small", "middle"或"large"，也可以整型的1, 2或3

验证规则并联不是简单的“或”的关系，具体验证流程如下：
1. 按顺序验证这些规则，如果有一条验证规则通过, 则该参数验证通过。
2. 如果全部验证规则都被忽略（If验证器条件不满足，或者没有Required验证器并且该参数不存在，或者有0条验证规则），也算参数验证通过。
3. 上面两条都不满足, 则该参数验证失败。

这些规则如果要完全理清并不是一件容易的事，所以不建议使用验证规则并联，也尽量不要设计需要这种验证方式的参数。

### 4.13 关于特殊值`null`, `""`，`0`，`false`的问题

这些特殊的值是不等价的，它们是不同的数据类型（需要用不同的验证器去验证）：
* `""`是字符串。
* `0`是整型。
* `false`是bool型。
* `null`是java的空指针。在本工具中它有特殊的含义。

如果某个参数的值为`null`，则本工具会视为该参数不存在。

比如下面两个Map对于本工具来说是等价的.
```java
params = new HashMap<String, Object>(){{
    put("name", "hello");
}};
```
与
```java
params = new HashMap<String, Object>(){{
    put("name", "hello");
    put("comment", null);
}};
```
是等价的。

为什么本工具会将参数的值为`null`等价为该参数不存在？

因为常规的HTTP请求无法传递`null`值，一般情况下客户端如果要发送一个值为`null`的参数，实际的HTTP请求中是没有这个参数的，这样处理歧义是最小的。  

但是有一些客户端不是这么处理的，而是将`null`值转换为字符串`"null"`传递。这种情况服务端很难正确处理，因为无法知道客户端传递的原始值是`null`值还是字符串`"null"`。

如果非要从客户端传递`null`值，那只能把所有参数转换为json格式作为Body以POST方式发送请求，并且`Content-Type`要设置为`application/json`；然后服务端要手动把json格式的body解析出来，因为servlet的 ServletRequest 不会解析 json 格式的body。

### 4.14 关于基本数据类型与字符串的关系

对于以下url地址
```
http://abc.com/index.php?p1=&&p2=hello&&p3=123
```
我们将得到的参数数组：
```java
params = new HashMap<String, Object>(){{
    put("p1", "");
    put("p2", "hello");
    put("p3", "123");
}};
```
*注意*：
* 参数"p1"的值为空字符串`""`，而不是`null`。
* 参数"p3"的值为字符串`"123"`，而不是整型`123`。
* GET方式的HTTP请求是传递不了`null`值的。

本工具的所有验证器都是**强类型**的，IntXxx验证的是整型，FloatXxx验证的是浮点型，StrXxx验证的是字符串型，数据类型不匹配，验证是通不过的。但是字符串类型是个例外。

因为常规的HTTP请求，所有的基本数据类型最终都会转换成字符串，所以：
* 整型`123`和字符串`"123"`均可以通过验证器IntXxx的验证；
* 浮点型`123.0`和字符串`"123.0"`均可以通过验证器FloatXxx的验证；
* bool型`true`和字符串`"true"`均可以通过验证器BoolXxx的验证；
* 但是`null`值和字符串`"null"`永远不等价，字符串`"null"`就只是普通的字符串。

### 4.15 自定义错误信息输出文本

如果参数验证不通过，`Validation.validate()`方法会抛出异常，这个异常会包含验证不通过的错误信息描述的文本。

但是这个描述文本对用户来说可能不那么友好，我们可以通过两个伪验证器来自定义这些文本：
* `Alias` 用于自定义参数名称（这个名称会与内部的错误信息模版相结合，生成最终的错误信息描述文本）
* `>>>` 用于自定义错误描述文本（这个文本会完全取代模版生成的错误描述文本）。

看下面的例子：

```java
HashMap<String, Object> params = new HashMap<String, Object>() {{
    put("title", "a");
}};
Validation.validate(params, new String[]{
    "title", "Required|StrLenGeLe:2,50", null,
}); // 抛出异常的错误描述为：“title”长度必须在 2 - 50 之间
Validation.validate(params, new String[]{
    "title", "Required|StrLenGeLe:2,50|Alias:标题", null, // 自定义参数名称
}); // 抛出异常的错误描述为：“标题”长度必须在 2 - 50 之间
Validation.validate(params, new String[]{
    "title", "Required|StrLenGeLe:2,50|>>>:标题长度应在2~50之间", null, // 自定义错误信息描述文本
}); // 抛出异常的错误描述为：标题长度应在2~50之间
```
参考附录A.10获取更详细的信息

### 4.16 国际化

尚未实现

## A 附录 - 验证器列表

### A.1 整型

整型验证器全部以"Int"开头。

| 整型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| Int | Int | “{{param}}”必须是整数 |
| IntEq | IntEq:100 | “{{param}}”必须等于 {{value}} |
| IntNe | IntNe:100 | “{{param}}”不能等于 {{value}} |
| IntGt | IntGt:100 | “{{param}}”必须大于 {{min}} |
| IntGe | IntGe:100 | “{{param}}”必须大于等于 {{min}} |
| IntLt | IntLt:100 | “{{param}}”必须小于 {{max}} |
| IntLe | IntLe:100 | “{{param}}”必须小于等于 {{max}} |
| IntGtLt | IntGtLt:1,100 | “{{param}}”必须大于 {{min}} 小于 {{max}} |
| IntGeLe | IntGeLe:1,100 | “{{param}}”必须大于等于 {{min}} 小于等于 {{max}} |
| IntGtLe | IntGtLe:1,100 | “{{param}}”必须大于 {{min}} 小于等于 {{max}} |
| IntGeLt | IntGeLt:1,100 | “{{param}}”必须大于等于 {{min}} 小于 {{max}} |
| IntIn | IntIn:2,3,5,7,11 | “{{param}}”只能取这些值: {{valueList}} |
| IntNotIn | IntNotIn:2,3,5,7,11 | “{{param}}”不能取这些值: {{valueList}} |

### A.2 长整型

整型验证器全部以"Long"开头。

| 整型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| Long | Long | “{{param}}”必须是长整数 |
| LongEq | LongEq:100 | “{{param}}”必须等于 {{value}} |
| LongNe | LongNe:100 | “{{param}}”不能等于 {{value}} |
| LongGt | LongGt:100 | “{{param}}”必须大于 {{min}} |
| LongGe | LongGe:100 | “{{param}}”必须大于等于 {{min}} |
| LongLt | LongLt:100 | “{{param}}”必须小于 {{max}} |
| LongLe | LongLe:100 | “{{param}}”必须小于等于 {{max}} |
| LongGtLt | LongGtLt:1,100 | “{{param}}”必须大于 {{min}} 小于 {{max}} |
| LongGeLe | LongGeLe:1,100 | “{{param}}”必须大于等于 {{min}} 小于等于 {{max}} |
| LongGtLe | LongGtLe:1,100 | “{{param}}”必须大于 {{min}} 小于等于 {{max}} |
| LongGeLt | LongGeLt:1,100 | “{{param}}”必须大于等于 {{min}} 小于 {{max}} |
| LongIn | LongIn:2,3,5,7,11 | “{{param}}”只能取这些值: {{valueList}} |
| LongNotIn | LongNotIn:2,3,5,7,11 | “{{param}}”不能取这些值: {{valueList}} |

### A.3 浮点型

内部一律使用双精度浮点数来处理

| 浮点型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| Float | Float | “{{param}}”必须是浮点数 |
| FloatGt | FloatGt:1.0 | “{{param}}”必须大于 {{min}} |
| FloatGe | FloatGe:1.0 | “{{param}}”必须大于等于 {{min}} |
| FloatLt | FloatLt:1.0 | “{{param}}”必须小于 {{max}} |
| FloatLe | FloatLe:1.0 | “{{param}}”必须小于等于 {{max}} |
| FloatGtLt | FloatGtLt:0,1.0 | “{{param}}”必须大于 {{min}} 小于 {{max}} |
| FloatGeLe | FloatGeLe:0,1.0 | “{{param}}”必须大于等于 {{min}} 小于等于 {{max}} |
| FloatGtLe | FloatGtLe:0,1.0 | “{{param}}”必须大于 {{min}} 小于等于 {{max}} |
| FloatGeLt | FloatGeLt:0,1.0 | “{{param}}”必须大于等于 {{min}} 小于 {{max}} |

### A.4 bool型

| bool型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| Bool | Bool | 合法的取值为: `true`, `false`, `"true"`, `"false"`（忽略大小写） |
| BoolTrue | BoolTrue | 合法的取值为: `true`, `"true"`（忽略大小写） |
| BoolFalse | BoolFalse | 合法的取值为: `false`,`"false"`（忽略大小写） |
| BoolSmart | BoolSmart | 合法的取值为: `true`, `false`, `"true"`, `"false"`, `1`, `0`, `"1"`, `"0"`, `"yes"`, `"no"`, `"y"`, `"n"`（忽略大小写） |
| BoolSmartTrue | BoolSmartTrue | 合法的取值为: `true`, `"true"`, `1`, `"1"`, `"yes"`, `"y"`（忽略大小写） |
| BoolSmartFalse | BoolSmartFalse | 合法的取值为: `false`, `"false"`, `0`, `"0"`, `"no"`, `"n"`（忽略大小写） |

### A.5 字符串型

| 字符串型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| Str | Str | “{{param}}”必须是字符串 |
| StrEq | StrEq:abc | “{{param}}”必须等于"{{value}}" |
| StrEqI | StrEqI:abc | “{{param}}”必须等于"{{value}}"（忽略大小写） |
| StrNe | StrNe:abc | “{{param}}”不能等于"{{value}}" |
| StrNeI | StrNeI:abc | “{{param}}”不能等于"{{value}}"（忽略大小写） |
| StrIn | StrIn:abc,def,g | “{{param}}”只能取这些值: {{valueList}} |
| StrInI | StrInI:abc,def,g | “{{param}}”只能取这些值: {{valueList}}（忽略大小写） |
| StrNotIn | StrNotIn:abc,def,g | “{{param}}”不能取这些值: {{valueList}} |
| StrNotInI | StrNotInI:abc,def,g | “{{param}}”不能取这些值: {{valueList}}（忽略大小写） |
| StrLen | StrLen:8 | “{{param}}”长度必须等于 {{length}} |
| StrLenGe | StrLenGe:8 | “{{param}}”长度必须大于等于 {{min}} |
| StrLenLe | StrLenLe:8 | “{{param}}”长度必须小于等于 {{max}} |
| StrLenGeLe | StrLenGeLe:6,8 | “{{param}}”长度必须在 {{min}} - {{max}} 之间 |
| Letters | Letters | “{{param}}”只能包含字母 |
| Alphabet | Alphabet | 同Letters |
| Numbers | Numbers | “{{param}}”只能是纯数字 |
| Digits | Digits | 同Numbers |
| LettersNumbers | LettersNumbers | “{{param}}”只能包含字母和数字 |
| Numeric | Numeric | “{{param}}”必须是数值。一般用于大数处理（超过double表示范围的数,一般会用字符串来表示）（尚未实现大数处理）, 如果是正常范围内的数, 可以使用IntXxx或FloatXxx来检测 |
| VarName | VarName | “{{param}}”只能包含字母、数字和下划线，并且以字母或下划线开头 |
| Email | Email | “{{param}}”必须是合法的email |
| Url | Url | “{{param}}”必须是合法的Url地址 |
| Ip | Ip | “{{param}}”必须是合法的IP地址 |
| Mac | Mac | “{{param}}”必须是合法的MAC地址 |
| Regexp | Regexp:/^abc$/ | Perl正则表达式匹配 |

### A.6 数组型

| 数组型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| List | List | “{{param}}”必须是数组或List |
| ListLen | ListLen:5 | “{{param}}”长度必须等于 {{length}} |
| ListLenGe | ListLenGe:1 | “{{param}}”长度必须大于等于 {{min}} |
| ListLenLe | ListLenLe:9 | “{{param}}”长度必须小于等于 {{max}} |
| ListLenGeLe | ListLenGeLe:1,9 | “{{param}}”组度必须在 {{min}} ~ {{max}} 之间 |
| Arr | Arr | “{{param}}”必须是数组或List |
| ArrLen | ArrLen:5 | “{{param}}”长度必须等于 {{length}} |
| ArrLenGe | ArrLenGe:1 | “{{param}}”长度必须大于等于 {{min}} |
| ArrLenLe | ArrLenLe:9 | “{{param}}”长度必须小于等于 {{max}} |
| ArrLenGeLe | ArrLenGeLe:1,9 | “{{param}}”组度必须在 {{min}} ~ {{max}} 之间 |

### A.7 Map型

| Map型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| Map | Map | “{{param}}”必须是 Map<String, Object> |

### A.8 文件型

*尚未实现*

| 文件型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| File | File | “{{param}}”必须是文件 |
| FileMaxSize | FileMaxSize:10mb | “{{param}}”必须是文件, 且文件大小不超过{{size}} |
| FileMinSize | FileMinSize:100kb | “{{param}}”必须是文件, 且文件大小不小于{{size}} |
| FileImage | FileImage | “{{param}}”必须是图片 |
| FileVideo | FileVideo | “{{param}}”必须是视频文件 |
| FileAudio | FileAudio | “{{param}}”必须是音频文件 |
| FileMimes | FileMimes:mpeg,jpeg,png | “{{param}}”必须是这些MIME类型的文件:{{mimes}} |

### A.9 日期和时间型

| 日期和时间型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| Date | Date | “{{param}}”必须符合日期格式YYYY-MM-DD |
| DateFrom | DateFrom:2017-04-13 | “{{param}}”不得早于 {{from}} |
| DateTo | DateTo:2017-04-13 | “{{param}}”不得晚于 {{to}} |
| DateFromTo | DateFromTo:2017-04-13,2017-04-13 | “{{param}}”必须在 {{from}} ~ {{to}} 之间 |
| DateTime | DateTime | “{{param}}”必须符合日期时间格式YYYY-MM-DD HH:mm:ss |
| DateTimeFrom | DateTimeFrom:2017-04-13 12:00:00 | “{{param}}”不得早于 {{from}} |
| DateTimeTo | DateTimeTo:2017-04-13 12:00:00 | “{{param}}”必须早于 {{to}} |
| DateTimeFromTo | DateTimeFromTo:2017-04-13 12:00:00,2017-04-13 12:00:00 | “{{param}}”必须在 {{from}} ~ {{to}} 之间 |

### A.10 条件判断型

在一条验证规则中，条件验证器必须在其它验证器前面，多个条件验证器可以串联。

注意，条件判断中的“条件”一般是检测**另外一个参数**的值，而当前参数的值是由串联在条件判断验证器后面的其它验证器来验证。

| 条件判断型验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| If|  If:selected |  如果参数"selected"值等于 1, true, "1", "true", "yes"或 "y"(字符串忽略大小写) |
| IfNot|  IfNot:selected |  如果参数"selected"值等于 0, false, "0", "false", "no"或"n"(字符串忽略大小写) |
| IfTrue|  IfTrue:selected |  如果参数"selected"值等于 true 或 "true"(忽略大小写) |
| IfFalse|  IfFalse:selected |  如果参数"selected"值等于 false 或 "false"(忽略大小写) |
| IfExist|  IfExist:var |  如果参数"var"存在 |
| IfNotExist|  IfNotExist:var |  如果参数"var"不存在 |
| IfIntEq|  IfIntEq:var,1 |  if (var == 1) |
| IfIntNe|  IfIntNe:var,2 |  if (var != 2). 特别要注意的是如果条件参数var的数据类型不匹配, 那么If条件是成立的; 而其它几个IfIntXx当条件参数var的数据类型不匹配时, If条件不成立 |
| IfIntGt|  IfIntGt:var,0 |  if (var > 0) |
| IfIntLt|  IfIntLt:var,1 |  if (var < 1) |
| IfIntGe|  IfIntGe:var,6 |  if (var >= 6) |
| IfIntLe|  IfIntLe:var,8 |  if (var <= 8) |
| IfIntIn|  IfIntIn:var,2,3,5,7 |  如果var的值等于2,3,5,7中的某一个 |
| IfIntNotIn|  IfIntNotIn:var,2,3,5,7 |  如果var的值不等于2,3,5,7中的任何一个 |
| IfLongEq|  IfLongEq:var,1 |  if (var == 1L) |
| IfLongNe|  IfLongNe:var,2 |  if (var != 2L). 特别要注意的是如果条件参数var的数据类型不匹配, 那么If条件是成立的; 而其它几个IfLongXx当条件参数var的数据类型不匹配时, If条件不成立 |
| IfLongGt|  IfLongGt:var,0 |  if (var > 0L) |
| IfLongLt|  IfLongLt:var,1 |  if (var < 1L) |
| IfLongGe|  IfLongGe:var,6 |  if (var >= 6L) |
| IfLongLe|  IfLongLe:var,8 |  if (var <= 8L) |
| IfLongIn|  IfLongIn:var,2,3,5,7 |  如果var的值等于2L,3L,5L,7L中的某一个 |
| IfLongNotIn|  IfLongNotIn:var,2,3,5,7 |  如果var的值不等于2L,3L,5L,7L中的任何一个 |
| IfStrEq|  IfStrEq:var,waiting |  if ("waiting".equals(var)) |
| IfStrNe|  IfStrNe:var,editing |  if (!"editing".equals(var)). 特别要注意的是如果条件参数var的数据类型不匹配, 那么If条件是成立的; 而其它几个IfStrXx当条件参数var的数据类型不匹配时, If条件不成立 |
| IfStrGt|  IfStrGt:var,a |  if (var.compareTo("a") > 0) |
| IfStrLt|  IfStrLt:var,z |  if (var.compareTo("z") < 0) |
| IfStrGe|  IfStrGe:var,A |  if (var.compareTo("A") >= 0) |
| IfStrLe|  IfStrLe:var,Z |  if (var.compareTo("Z") <= 0) |
| IfStrIn|  IfStrIn:var,normal,warning,error |  如果var的值等于"normal", "warning", "error"中的某一个 |
| IfStrNotIn|  IfStrNotIn:var,warning,error |  如果var的值不等于"normal", "warning", "error"中的任何一个 |

### A.11 其它验证器

| 其它验证器 | 示例 | 说明 |
| :------| :------ | :------ |
| Required | Required | 待验证的参数是必需的。如果验证器串联，除了条件型验证器外，必须为第一个验证器 |
| Alias | Alias:参数名称 | 自定义错误提示文本中的参数名称（必须是最后一个验证器） |
| \>>> | \>>>:这是自定义错误提示文本 | 自定义错误提示文本（与Alias验证器二选一，必须是最后一个验证器） |
| 自定义Java函数，Lambda表达式 |  | 暂不提供该机制，因为如果遇到本工具不支持的复杂参数验证，你可以直接写Java代码来验证，不需要再经由本工具来验证（否则就是脱裤子放屁，多此一举） |
