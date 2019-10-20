# validation

A validation library for java

## Usage

```
// Sample parameters
HashMap<String, Object> params = new HashMap<String, Object>() {{
    put("offset", "0");
    put("count", "10");
}};

// validate
Validation.validate(params, new String[]{
    "offset", "Required|IntGe:0", null,
    "count", "Required|IntGtLe:0,1000", null,
});
```