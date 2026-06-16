package com.framework.ai.generator;

import com.framework.utils.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.framework.ai.client.AIClient;

/**
 * AI 测试用例生成器。
 *
 * 输入：需求描述/PRD 片段/用户故事
 * 输出：TestNG 测试类骨架 + Page Object 桩代码
 *
 * 使用方式：
 *   String req = "用户登录：输入用户名密码，点击登录，成功后跳转首页";
 *   GeneratedCode code = AITestGenerator.generate(req);
 *   code.saveTo("src/test/java/com/app/testcases/");
 */
public class AITestGenerator {

    private static final String SYSTEM_PROMPT = """
            你是一个资深 Java 测试开发工程师，精通 Selenium + TestNG + Page Object 模式。
            你的任务：根据需求描述，生成可执行的测试代码。
            
            输出 JSON 格式：
            {
              "analysis": {
                "test_points": ["测试点1", "测试点2"],
                "edge_cases": ["边界场景1"],
                "risk_areas": ["风险区域1"]
              },
              "page_objects": [
                {
                  "class_name": "LoginPage",
                  "package": "com.app.pages",
                  "methods": [
                    {"name": "login", "return_type": "DashboardPage", "params": [{"name": "username", "type": "String"}, {"name": "password", "type": "String"}]}
                  ],
                  "elements": [
                    {"name": "usernameInput", "locator": "id=username", "description": "用户名输入框"},
                    {"name": "passwordInput", "locator": "id=password", "description": "密码输入框"}
                  ]
                }
              ],
              "test_cases": [
                {
                  "class_name": "LoginTest",
                  "package": "com.app.testcases",
                  "description": "登录功能测试",
                  "methods": [
                    {
                      "name": "testLoginSuccess",
                      "description": "正常登录",
                      "groups": ["smoke", "regression"],
                      "priority": "HIGH",
                      "test_data": [
                        {"username": "admin", "password": "Admin@123"}
                      ],
                      "steps": [
                        "打开登录页",
                        "输入用户名 admin",
                        "输入密码 Admin@123",
                        "点击登录按钮",
                        "验证跳转到 Dashboard 页面"
                      ],
                      "assertions": [
                        "Dashboard 页面的 isAt() 返回 true"
                      ]
                    }
                  ]
                }
              ]
            }
            
            要求：
            1. 测试方法使用 @Test 注解，包含 groups 和 description
            2. Page Object 继承 BasePage，使用 ElementActions
            3. 覆盖正常流程 + 异常流程 + 边界条件
            4. 包含数据驱动测试（@DataProvider）
            5. 只输出 JSON，不要其他内容""";

    private AITestGenerator() {}

    /**
     * 根据需求描述生成测试代码。
     *
     * @param requirement 需求描述（中文/英文均可）
     * @return 生成的代码集合，失败返回 null
     */
    public static GeneratedCode generate(String requirement) {
        return generate(requirement, null);
    }

    /**
     * 根据需求描述 + 已有页面对象生成测试代码。
     *
     * @param requirement    需求描述
     * @param existingPages  已有页面对象描述（如 "已有 LoginPage, HomePage"）
     */
    public static GeneratedCode generate(String requirement, String existingPages) {
        if (!AIClient.isReady()) {
            LogUtils.warn(AITestGenerator.class, "AI 未启用，无法生成用例");
            return null;
        }

        long start = System.currentTimeMillis();
        LogUtils.info(AITestGenerator.class, "🤖 AI 正在根据需求生成测试用例...");

        try {
            StringBuilder userMsg = new StringBuilder();
            userMsg.append("## 需求描述\n").append(requirement).append("\n\n");
            userMsg.append("## 框架信息\n");
            userMsg.append("- 测试框架: TestNG 7.x\n");
            userMsg.append("- 基础类: BaseTest（继承后自动管理 WebDriver）\n");
            userMsg.append("- 页面对象基类: BasePage（提供 navigateTo(), isAt(), actions() 等方法）\n");
            userMsg.append("- 元素操作: ElementActions（提供 click(), type(), selectByText() 等链式操作）\n");
            userMsg.append("- BaseTest 已在 @BeforeMethod 中启动浏览器，@AfterMethod 中关闭\n");
            userMsg.append("- 使用 baseUrl + getPageUrl() 拼接完整 URL\n");
            userMsg.append("\n");

            if (existingPages != null && !existingPages.isBlank()) {
                userMsg.append("## 已有页面对象\n").append(existingPages).append("\n\n");
            }

            userMsg.append("请为这个需求生成测试用例和所需的页面对象代码。");

            String aiResult = AIClient.get().chat(SYSTEM_PROMPT, userMsg.toString());
            if (aiResult == null) {
                LogUtils.warn(AITestGenerator.class, "AI 未返回结果");
                return null;
            }

            GeneratedCode code = parseGeneratedCode(aiResult, requirement);
            long elapsed = System.currentTimeMillis() - start;
            LogUtils.info(AITestGenerator.class, "🤖 用例生成完成 ({}ms): {} 个测试类, {} 个页面对象",
                    elapsed, code.testClasses.size(), code.pageObjects.size());

            return code;
        } catch (Exception e) {
            LogUtils.error(AITestGenerator.class, "用例生成失败: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static GeneratedCode parseGeneratedCode(String aiJson, String requirement) {
        GeneratedCode result = new GeneratedCode();
        try {
            Map<String, Object> root = new com.fasterxml.jackson.databind.ObjectMapper().readValue(aiJson, Map.class);

            // 解析分析结果
            Map<String, Object> analysis = (Map<String, Object>) root.get("analysis");
            if (analysis != null) {
                result.analysis = new AnalysisResult();
                result.analysis.testPoints = (List<String>) analysis.getOrDefault("test_points", List.of());
                result.analysis.edgeCases = (List<String>) analysis.getOrDefault("edge_cases", List.of());
                result.analysis.riskAreas = (List<String>) analysis.getOrDefault("risk_areas", List.of());
            }

            // 解析页面对象
            List<Map<String, Object>> pages = (List<Map<String, Object>>) root.get("page_objects");
            if (pages != null) {
                for (Map<String, Object> p : pages) {
                    result.pageObjects.add(buildPageObjectCode(p, requirement));
                }
            }

            // 解析测试用例
            List<Map<String, Object>> tests = (List<Map<String, Object>>) root.get("test_cases");
            if (tests != null) {
                for (Map<String, Object> t : tests) {
                    result.testClasses.add(buildTestCode(t, requirement));
                }
            }
        } catch (Exception e) {
            LogUtils.warn(AITestGenerator.class, "解析 AI 生成结果失败: {}", e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static String buildPageObjectCode(Map<String, Object> pageData, String requirement) {
        String className = (String) pageData.get("class_name");
        String packageName = (String) pageData.getOrDefault("package", "com.app.pages");

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import com.framework.browser.DriverManager;\n");
        sb.append("import com.framework.pages.BasePage;\n");
        sb.append("import com.framework.element.actions.ElementActions;\n");
        sb.append("import org.openqa.selenium.By;\n");
        sb.append("import org.openqa.selenium.WebDriver;\n\n");
        sb.append("/**\n");
        sb.append(" * ").append(className).append(" - 自动生成\n");
        sb.append(" * 需求: ").append(truncate(requirement, 80)).append("\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append(" extends BasePage {\n\n");

        // 元素定位器
        List<Map<String, Object>> elements = (List<Map<String, Object>>) pageData.getOrDefault("elements", List.of());
        for (Map<String, Object> el : elements) {
            String name = (String) el.get("name");
            String locator = (String) el.get("locator");
            String desc = (String) el.get("description");
            if (name != null && locator != null) {
                sb.append("    // ").append(desc != null ? desc : "").append("\n");
                sb.append("    private final By ").append(toCamelCase(name)).append(" = By.").append(toByMethod(locator)).append(";\n\n");
            }
        }

        // 构造方法
        sb.append("    public ").append(className).append("(WebDriver driver) {\n");
        sb.append("        super(driver);\n");
        sb.append("    }\n\n");
        sb.append("    public ").append(className).append("() {\n");
        sb.append("        super();\n");
        sb.append("    }\n\n");

        // 抽象方法实现
        sb.append("    @Override\n");
        sb.append("    public String getPageUrl() {\n");
        sb.append("        return \"/").append(toKebabCase(className)).append("\"; // TODO: 替换为实际路径\n");
        sb.append("    }\n\n");
        sb.append("    @Override\n");
        sb.append("    public boolean isAt() {\n");
        if (!elements.isEmpty()) {
            Map<String, Object> firstEl = elements.get(0);
            sb.append("        return actions().isDisplayed(").append(toCamelCase((String) firstEl.get("name"))).append(");\n");
        } else {
            sb.append("        return !driver.getTitle().isEmpty();\n");
        }
        sb.append("    }\n\n");

        // 方法
        List<Map<String, Object>> methods = (List<Map<String, Object>>) pageData.getOrDefault("methods", List.of());
        for (Map<String, Object> m : methods) {
            String methodName = (String) m.get("name");
            String returnType = (String) m.getOrDefault("return_type", "void");
            List<Map<String, Object>> params = (List<Map<String, Object>>) m.getOrDefault("params", List.of());

            sb.append("    /** TODO: 添加文档 */\n");
            sb.append("    public ").append(returnType).append(" ").append(methodName).append("(");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) sb.append(", ");
                Map<String, Object> param = params.get(i);
                sb.append(param.get("type")).append(" ").append(param.get("name"));
            }
            sb.append(") {\n");
            for (Map<String, Object> param : params) {
                String pName = (String) param.get("name");
                String pType = (String) param.get("type");
                // 猜测应该填充哪个输入框
                sb.append("        actions().type(/* TODO: 定位器 */ null, ");
                sb.append(pName).append(");\n");
            }
            sb.append("        // TODO: 实现业务逻辑\n");
            if (!"void".equals(returnType)) {
                sb.append("        return new ").append(returnType).append("();\n");
            }
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static String buildTestCode(Map<String, Object> testData, String requirement) {
        String className = (String) testData.get("class_name");
        String packageName = (String) testData.getOrDefault("package", "com.app.testcases");
        String description = (String) testData.getOrDefault("description", "");

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import com.framework.framework.BaseTest;\n");
        sb.append("import org.testng.annotations.Test;\n");
        sb.append("import org.testng.annotations.DataProvider;\n");
        sb.append("import org.testng.Assert;\n\n");
        sb.append("/**\n");
        sb.append(" * ").append(description).append("\n");
        sb.append(" * 需求: ").append(truncate(requirement, 80)).append("\n");
        sb.append(" * 自动生成于: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append(" */\n");
        sb.append("public class ").append(className).append(" extends BaseTest {\n\n");

        // 测试方法
        List<Map<String, Object>> methods = (List<Map<String, Object>>) testData.getOrDefault("methods", List.of());
        for (Map<String, Object> m : methods) {
            String methodName = (String) m.get("name");
            String methodDesc = (String) m.getOrDefault("description", "");
            List<String> groups = (List<String>) m.getOrDefault("groups", List.of());
            String priority = (String) m.getOrDefault("priority", "MEDIUM");
            List<String> steps = (List<String>) m.getOrDefault("steps", List.of());
            List<String> assertions = (List<String>) m.getOrDefault("assertions", List.of());
            List<Map<String, String>> testDataList = (List<Map<String, String>>) m.getOrDefault("test_data", List.of());

            // 注解
            sb.append("    @Test(description = \"").append(methodDesc).append("\"");
            if (!groups.isEmpty()) {
                sb.append(", groups = {");
                sb.append(String.join(", ", groups.stream().map(g -> "\"" + g + "\"").toList()));
                sb.append("}");
            }
            if (testDataList.size() > 1) {
                sb.append(", dataProvider = \"").append(methodName).append("Data\"");
            }
            sb.append(")\n");

            sb.append("    public void ").append(methodName).append("() {\n");
            sb.append("        // TODO: 实现测试逻辑\n");
            sb.append("        // 步骤:\n");
            for (int i = 0; i < steps.size(); i++) {
                sb.append("        // ").append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
            sb.append("\n");
            sb.append("        // 断言:\n");
            for (String assertion : assertions) {
                sb.append("        // Assert.assertTrue(..., \"").append(assertion).append("\");\n");
            }
            sb.append("    }\n\n");

            // DataProvider（如果有多组数据）
            if (testDataList.size() > 1) {
                sb.append("    @DataProvider(name = \"").append(methodName).append("Data\")\n");
                sb.append("    public Object[][] ").append(methodName).append("Data() {\n");
                sb.append("        return new Object[][]{\n");
                for (Map<String, String> td : testDataList) {
                    sb.append("                {");
                    List<String> values = new ArrayList<>(td.values());
                    for (int i = 0; i < values.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append("\"").append(values.get(i)).append("\"");
                    }
                    sb.append("},\n");
                }
                sb.append("        };\n");
                sb.append("    }\n\n");
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    // ========== 工具方法 ==========

    /** 解析 "id=username" 为 By.id("username") */
    private static String toByMethod(String locator) {
        if (locator == null) return "cssSelector(\"TODO\")";
        int eq = locator.indexOf('=');
        if (eq < 0) return "cssSelector(\"" + locator + "\")";
        String strategy = locator.substring(0, eq).trim().toLowerCase();
        String value = locator.substring(eq + 1).trim();
        return switch (strategy) {
            case "id"        -> "id(\"" + value + "\")";
            case "name"      -> "name(\"" + value + "\")";
            case "class","classname" -> "className(\"" + value + "\")";
            case "css"       -> "cssSelector(\"" + value + "\")";
            case "xpath"     -> "xpath(\"" + value + "\")";
            case "tagname"   -> "tagName(\"" + value + "\")";
            case "linktext"  -> "linkText(\"" + value + "\")";
            default          -> "cssSelector(\"" + value + "\")";
        };
    }

    /** "usernameInput" → "usernameInput" (已是 camelCase) */
    private static String toCamelCase(String s) {
        if (s == null) return "TODO";
        // 如果已是 camelCase，直接返回
        return s;
    }

    /** "LoginPage" → "login-page" */
    private static String toKebabCase(String s) {
        if (s == null) return "page";
        return s.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    // ========== 结果类 ==========

    /** 分析结果 */
    public static class AnalysisResult {
        public List<String> testPoints = List.of();
        public List<String> edgeCases = List.of();
        public List<String> riskAreas = List.of();

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("📋 测试点:\n");
            for (String tp : testPoints) sb.append("  • ").append(tp).append("\n");
            sb.append("⚠️ 边界场景:\n");
            for (String ec : edgeCases) sb.append("  • ").append(ec).append("\n");
            sb.append("🔴 风险区域:\n");
            for (String ra : riskAreas) sb.append("  • ").append(ra).append("\n");
            return sb.toString();
        }
    }

    /** 生成的代码集合 */
    public static class GeneratedCode {
        public AnalysisResult analysis;
        public final List<String> pageObjects = new ArrayList<>();
        public final List<String> testClasses = new ArrayList<>();

        /** 保存所有生成的代码到文件系统 */
        public void saveTo(String baseDir) {
            java.io.File dir = new java.io.File(baseDir);
            dir.mkdirs();
            try {
                // 保存页面对象
                for (String code : pageObjects) {
                    String className = extractClassName(code);
                    if (className != null) {
                        java.io.File file = new java.io.File(dir, "pages/" + className + ".java");
                        file.getParentFile().mkdirs();
                        java.nio.file.Files.writeString(file.toPath(), code);
                    }
                }
                // 保存测试类
                for (String code : testClasses) {
                    String className = extractClassName(code);
                    if (className != null) {
                        java.io.File file = new java.io.File(dir, className + ".java");
                        java.nio.file.Files.writeString(file.toPath(), code);
                    }
                }
                LogUtils.info(AITestGenerator.class, "💾 代码已保存到: {}", dir.getAbsolutePath());
            } catch (Exception e) {
                LogUtils.error(AITestGenerator.class, "保存代码失败: {}", e.getMessage());
            }
        }

        private String extractClassName(String code) {
            Matcher m = Pattern.compile("public class (\\w+)").matcher(code);
            return m.find() ? m.group(1) : null;
        }
    }
}
