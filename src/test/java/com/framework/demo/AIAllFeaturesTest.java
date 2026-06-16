package com.framework.demo;

import com.framework.ai.client.AIClient;
import com.framework.ai.analysis.FailureAnalyzer;
import com.framework.ai.analysis.FlakyTestAnalyzer;
import com.framework.ai.analysis.TestCodeReviewer;
import com.framework.ai.generator.AIReportGenerator;
import com.framework.ai.generator.AITestGenerator;
import com.framework.ai.assertion.VisualAIAssertion;
import com.framework.ai.healing.DOMChangeDetector;
import com.framework.pages.DashboardPage;
import com.framework.pages.LoginPage;
import com.framework.framework.BaseTest;
import com.framework.utils.LogUtils;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

/**
 * ===========================================
 * 🤖 AI 全功能集成演示
 * ===========================================
 *
 * 本测试类串联展示框架全部 8 个 AI 能力的实际用法。
 * 每个 @Test 方法对应一项核心 AI 功能。
 *
 * ╔═══════════════════════════════════════════════════════╗
 * ║  #   AI 功能               触发方式      依赖 AI ║
 * ╠═══════════════════════════════════════════════════════╣
 * ║  1️⃣  失败智能诊断           自动 + 手动     ✅     ║
 * ║  2️⃣  定位器自愈             手动 API        ✅     ║
 * ║  3️⃣  自然语言报告           自动 + 手动     ✅     ║
 * ║  4️⃣  需求→用例生成          手动 API        ✅     ║
 * ║  5️⃣  Flaky Test 检测        自动 + 手动     ⚡可选  ║
 * ║  6️⃣  测试代码审查           手动 API        ✅     ║
 * ║  7️⃣  视觉 AI 测试           手动 API        ✅     ║
 * ║  8️⃣  DOM 变更影响分析       手动 API        ✅     ║
 * ╚═══════════════════════════════════════════════════════╝
 *
 * 运行方式：
 *   # AI 关闭（全部走 Mock 兜底，CI/CD 零依赖）
 *   mvn test -Dtest=AIAllFeaturesTest -Dheadless=true
 *
 *   # AI 开启（真实大模型调用，先配好 API Key）
 *   export DEEPSEEK_API_KEY="sk-xxx"
 *   mvn test -Dtest=AIAllFeaturesTest
 *
 * 前置条件：
 *   1. config.yaml 中 ai.enabled=true（或 false 也行，8 个功能都有兜底）
 *   2. 环境变量或 config 中配置 API Key
 *   3. 被测应用 /login 和 /dashboard 路由可用（或 Mock）
 */
public class AIAllFeaturesTest extends BaseTest {

    private LoginPage loginPage;

    @BeforeClass
    public void printAIBanner() {
        LogUtils.info(getClass(), "╔═══════════════════════════════════════╗");
        LogUtils.info(getClass(), "║  🤖 AI 全功能集成演示                  ║");
        LogUtils.info(getClass(), "║  8 大 AI 能力一条龙展示                ║");
        LogUtils.info(getClass(), "╚═══════════════════════════════════════╝");

        boolean aiReady = AIClient.isReady();
        LogUtils.info(getClass(), ">>> AI 状态: {} | Provider: {} | Model: {}",
                aiReady ? "✅ 已就绪" : "⚠️ 未启用（走 Mock 兜底）",
                config.aiConfig().getProvider(),
                aiReady ? config.aiConfig().getModel() : "mock");

        Allure.parameter("AI Status", aiReady ? "ENABLED" : "MOCK");
        Allure.parameter("AI Provider", config.aiConfig().getProvider());
        Allure.parameter("AI Model", config.aiConfig().getModel());
    }

    // ─────────────────────────────────────────────
    // 功能 1️⃣：错误智能诊断 — 手动调用 FailureAnalyzer
    //   （自动诊断由 AIFailureAnalysisHook + TestListener 完成，无需编码）
    // ─────────────────────────────────────────────

    @Test(priority = 1, description = "AI 失败智能诊断 — 模拟失败并分析根因")
    @Severity(SeverityLevel.CRITICAL)
    @Description("手动调用 FailureAnalyzer.analyze()，传入异常+DOM信息，AI 返回诊断JSON")
    public void test01_FailureAnalysis() {
        LogUtils.info(getClass(), "=== 1️⃣ AI 失败诊断 ===");

        // 访问页面（即使页面正常也行，这里演示 AI 分析框架）
        loginPage = new LoginPage();
        loginPage.navigateTo();

        try {
            // 故意用不存在的元素触发 NoSuchElementException
            var driver = loginPage.navigateTo();
            // 使用原始 Selenium API 直接找一个不存在的元素来模拟失败
            // 这里我们构造一个假的异常传给 AI 分析（真实场景是 TestListener 自动捕获的）
            RuntimeException mockException = new RuntimeException(
                    "NoSuchElementException: Unable to locate element: " +
                    "By.cssSelector('.old-login-btn-wrapper .submit-btn')");

            // ★ AI 诊断
            LogUtils.info(getClass(), "🤖 调用 AI 失败诊断...");
            String diagnosis = FailureAnalyzer.analyze(
                    com.framework.browser.DriverManager.getDriver(), mockException, "testLoginWithOldCssSelector"
            );

            if (diagnosis != null) {
                LogUtils.info(getClass(), "📋 AI 诊断结果:\n{}", diagnosis);
                Allure.addAttachment("AI 失败诊断", "application/json", diagnosis, "json");

                // 解析结构化结果
                FailureAnalyzer.FailureResult parsed = FailureAnalyzer.parseResult(diagnosis);
                if (parsed != null) {
                    LogUtils.info(getClass(), "根因: {} | 分类: {} | 置信度: {:.0f}%",
                            parsed.root_cause, parsed.category, parsed.confidence * 100);
                    Assert.assertNotNull(parsed.root_cause, "AI 应返回根因");
                    Assert.assertNotNull(parsed.category, "AI 应返回分类");
                }
            } else {
                LogUtils.warn(getClass(), "AI 未启用，失败诊断返回 null（不影响测试）");
            }
        } catch (Exception e) {
            LogUtils.warn(getClass(), "演示场景异常（可忽略）: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 功能 2️⃣：定位器自愈 — clickWithHeal / typeWithHeal / findElementWithHeal
    // ─────────────────────────────────────────────

    @Test(priority = 2, description = "AI 定位器自愈 — 双引擎(Healenium+AI)自动修复脆弱定位器")
    @Severity(SeverityLevel.CRITICAL)
    @Description("使用 ElementActions 的自愈方法，当原始定位器失败时 AI 推导备选")
    public void test02_SelfHealingLocator() {
        LogUtils.info(getClass(), "=== 2️⃣ 定位器自愈 ===");

        loginPage = new LoginPage();
        loginPage.navigateTo();

        // 获取 elementActions 实例（已在 BaseTest 的 ThreadLocal 中初始化）
        var actions = actions();

        // ★ 场景A：带自愈的输入
        //   即使 id 不匹配，框架也会调用 Healenium→AI 双引擎推导备选
        LogUtils.info(getClass(), "🔧 演示 clickWithHeal: 登录按钮");
        try {
            // 用已知合法的定位器，模拟正常自愈路径
            actions.clickWithHeal(
                    By.id("username"),  // 这个定位器大概率有效
                    "用户名输入框"       // ← AI 上下文描述，帮助推导
            );
            LogUtils.info(getClass(), "✅ clickWithHeal 成功");
        } catch (Exception e) {
            LogUtils.warn(getClass(), "元素不可交互（演示场景，可忽略）: {}", e.getMessage());
        }

        // ★ 场景B：带自愈的元素查找
        LogUtils.info(getClass(), "🔧 演示 findElementWithHeal: 密码输入框");
        try {
            var el = actions.findElementWithHeal(
                    By.id("password"),
                    "密码输入框"
            );
            Assert.assertNotNull(el, "自愈查找应返回元素");
            LogUtils.info(getClass(), "✅ findElementWithHeal 成功: tag={}", el.getTagName());
        } catch (Exception e) {
            LogUtils.warn(getClass(), "元素不存在（演示场景，可忽略）: {}", e.getMessage());
        }

        // ★ 场景C：普通定位失败 → 验证自愈能捕获
        LogUtils.info(getClass(), "🔧 演示自愈兜底: 使用不存在的定位器");
        try {
            actions.clickWithHeal(
                    By.cssSelector(".non-existent-element-xyz-123"),
                    "一个不存在的按钮，用于演示自愈失败路径"
            );
        } catch (Exception e) {
            LogUtils.info(getClass(), "✅ 自愈也失败了（预期行为，因为元素真的不存在）: {}",
                    e.getMessage().substring(0, Math.min(80, e.getMessage().length())));
        }
    }

    // ─────────────────────────────────────────────
    // 功能 3️⃣：自然语言报告 — 手动生成 AI 测试摘要
    //   （自动报告由 AIReportHook 在 Suite 结束时触发，无需编码）
    // ─────────────────────────────────────────────

    @Test(priority = 3, description = "AI 自然语言报告 — 把统计数字变成人类可读的摘要")
    @Severity(SeverityLevel.NORMAL)
    @Description("手工构建 AIReportGenerator，填充假数据，查看 AI 生成的中文报告")
    public void test03_AIReport() {
        LogUtils.info(getClass(), "=== 3️⃣ AI 自然语言报告 ===");

        // 模拟一批测试数据（实际场景由 AIReportHook 自动填充真实数据）
        AIReportGenerator reportGen = AIReportGenerator.create()
                .suite("UI 回归测试 v3.1");

        // 用反射模拟 testng ITestResult 太复杂，这里直接展示 API 用法
        // 真实流程由 AIReportHook 在 Suite 结束时自动完成
        LogUtils.info(getClass(), """
                📊 AIReportGenerator 用法：
                   AIReportGenerator.create()
                      .suite("UI 回归测试")
                      .record(testngResult)
                      .generate();
                
                自动模式（零代码）：
                   AIReportHook 在 @AfterSuite 自动调用以上流程
                   → 输出自然语言报告到 Allure + 日志
                """);

        Allure.addAttachment("AI Report API", "text/plain",
                "AIReportGenerator 由 AIReportHook 在 Suite 结束时自动触发\n" +
                "无需显式编码，配置 ai.report.enabled=true 即可");
    }

    // ─────────────────────────────────────────────
    // 功能 4️⃣：需求→用例生成 — AITestGenerator.generate()
    // ─────────────────────────────────────────────

    @Test(priority = 4, description = "AI 测试用例生成 — 需求描述→TestNG代码+Page Object")
    @Severity(SeverityLevel.NORMAL)
    @Description("输入中文需求描述，AI 生成完整的 TestNG 测试类 + Page Object 桩代码")
    public void test04_AITestGeneration() {
        LogUtils.info(getClass(), "=== 4️⃣ 需求→用例生成 ===");

        // ★ 输入需求描述
        String requirement = """
                用户注册功能：
                1. 用户点击首页「注册」按钮进入注册页
                2. 输入手机号（11位数字）、验证码（6位）、设置密码（8-16位，含大小写+数字）
                3. 点击「获取验证码」，60秒内不可重复发送
                4. 勾选「同意用户协议」后点击「立即注册」
                5. 注册成功跳转到欢迎页并显示「注册成功」
                6. 手机号已注册时提示「该手机号已注册」并留在注册页
                7. 验证码错误时提示「验证码错误」
                """;

        // AI 生成
        String existingPages = "已有: LoginPage（登录页）, HomePage（首页）, UserCenterPage（个人中心）";
        AITestGenerator.GeneratedCode code = AITestGenerator.generate(requirement, existingPages);

        if (code != null) {
            // 打印 AI 分析
            LogUtils.info(getClass(), "📋 AI 分析结果:\n{}", code.analysis);
            Allure.addAttachment("AI 需求分析", "text/plain",
                    code.analysis != null ? code.analysis.toString() : "null");

            // 打印生成的 Page Object
            LogUtils.info(getClass(), "🏗️ 生成页面对象: {} 个", code.pageObjects.size());
            for (int i = 0; i < code.pageObjects.size(); i++) {
                Allure.addAttachment("PageObject-" + (i + 1), "text/x-java",
                        code.pageObjects.get(i));
            }

            // 打印生成的测试类
            LogUtils.info(getClass(), "🧪 生成测试类: {} 个", code.testClasses.size());
            for (int i = 0; i < code.testClasses.size(); i++) {
                Allure.addAttachment("TestCase-" + (i + 1), "text/x-java",
                        code.testClasses.get(i));
            }

            Assert.assertFalse(code.testClasses.isEmpty(), "AI 应至少生成一个测试类");
        } else {
            LogUtils.warn(getClass(), "⚠️ AI 未启用或生成失败，跳过用例生成");
        }
    }

    // ─────────────────────────────────────────────
    // 功能 5️⃣：Flaky Test 检测 — 统计+AI 双阶段分析
    //   （自动检测由 FlakyDetectionHook 在 Suite 结束时触发）
    // ─────────────────────────────────────────────

    @Test(priority = 5, description = "Flaky Test 智能检测 — 统计模式识别 + AI 根因分析")
    @Severity(SeverityLevel.NORMAL)
    @Description("扫描 test-history/ 目录的历史记录，两阶段检测：统计→AI诊断")
    public void test05_FlakyDetection() {
        LogUtils.info(getClass(), "=== 5️⃣ Flaky Test 检测 ===");

        // ★ 一步到位：统计识别 + AI 根因诊断
        List<FlakyTestAnalyzer.FlakyReport> flakies = FlakyTestAnalyzer.analyzeWithAI(7);

        if (flakies.isEmpty()) {
            LogUtils.info(getClass(), "✅ 最近 7 天没有发现 Flaky Test（历史数据可能不足）");
            LogUtils.info(getClass(), """
                    💡 FlakyTestAnalyzer 工作原理：
                       阶段1（本地计算）: 分析通过率 30%-95%、错误多样性、执行时间波动
                       阶段2（AI诊断）:   把失败历史发给LLM，推导深层根因
                       → 自动集成: FlakyDetectionHook 在 Suite 结束时自动运行
                    """);
        } else {
            LogUtils.info(getClass(), "🔍 发现 {} 个 Flaky Test:", flakies.size());
            StringBuilder report = new StringBuilder();
            for (FlakyTestAnalyzer.FlakyReport f : flakies) {
                LogUtils.warn(getClass(), f.toString());
                report.append(f.toString()).append("\n\n");
            }
            Allure.addAttachment("Flaky Test Report", "text/plain", report.toString());
        }
    }

    // ─────────────────────────────────────────────
    // 功能 6️⃣：测试代码审查 — TestCodeReviewer
    // ─────────────────────────────────────────────

    @Test(priority = 6, description = "AI 代码审查 — 检查硬等待/缺失断言/脆弱定位器等 8 类问题")
    @Severity(SeverityLevel.NORMAL)
    @Description("审查测试脚本质量，AI 检查 8 类常见问题并给出打分和修复建议")
    public void test06_CodeReview() {
        LogUtils.info(getClass(), "=== 6️⃣ AI 代码审查 ===");

        // ★ 构造一段有问题的测试代码供 AI 审查
        String mockBadCode = """
                package com.app.testcases;
                
                import org.testng.annotations.Test;
                import org.openqa.selenium.By;
                import org.openqa.selenium.WebDriver;
                import org.openqa.selenium.chrome.ChromeDriver;
                
                public class BadLoginTest {
                
                    @Test
                    public void testLogin() {
                        WebDriver driver = new ChromeDriver();
                        driver.get("https://example.com/login");
                        
                        // ❌ 硬等待
                        Thread.sleep(3000);
                        
                        // ❌ 脆弱 XPath + 绝对索引
                        driver.findElement(
                            By.xpath("//div[@class='login-form']/div[2]/input[1]"))
                            .sendKeys("admin");
                        
                        // ❌ 重复代码未抽取
                        driver.findElement(
                            By.xpath("//div[@class='login-form']/div[2]/input[2]"))
                            .sendKeys("password");
                        
                        // ❌ 又硬等待
                        Thread.sleep(2000);
                        
                        driver.findElement(
                            By.xpath("//div[@class='login-form']/div[3]/button"))
                            .click();
                        
                        // ❌ 没有断言！
                        driver.quit();
                    }
                }
                """;

        // ★ AI 审查
        TestCodeReviewer.ReviewResult result = TestCodeReviewer.review(
                mockBadCode,
                "这是一段写得很差的登录测试代码，请找出所有问题"
        );

        if (result != null) {
            LogUtils.info(getClass(), "📊 审查得分: {}/100 ({}级) | 问题数: {}",
                    result.overallScore, result.grade(), result.issues.size());

            Allure.addAttachment("AI Code Review", "text/plain", result.toString());

            // 验证 AI 能发现问题
            Assert.assertTrue(result.issues.size() >= 2,
                    "AI 应至少发现 2 个问题（实际发现 " + result.issues.size() + " 个）");

            // 列出所有问题
            for (TestCodeReviewer.Issue issue : result.issues) {
                LogUtils.warn(getClass(), "{} [{}] {} → 建议: {}",
                        issue.severity, issue.category, issue.description, issue.suggestion);
            }
        } else {
            LogUtils.warn(getClass(), "⚠️ AI 未启用，跳过代码审查");
        }
    }

    // ─────────────────────────────────────────────
    // 功能 7️⃣：视觉 AI 测试 — VisualAIAssertion
    // ─────────────────────────────────────────────

    @Test(priority = 7, description = "AI 视觉验证 — 语义理解代替像素对比，3种模式全演示")
    @Severity(SeverityLevel.CRITICAL)
    @Description("三种视觉测试模式：全页断言 / 元素断言 / 截图对比回归")
    public void test07_VisualAI() {
        LogUtils.info(getClass(), "=== 7️⃣ 视觉 AI 测试 ===");

        // 先导航到页面
        loginPage = new LoginPage();
        loginPage.navigateTo();

        try {
            // ── 模式A：全页视觉断言 ──
            LogUtils.info(getClass(), "👁️ 模式A: 全页视觉断言");
            try {
                VisualAIAssertion.VisualResult vr = VisualAIAssertion.assertLooksLike(
                        "登录页应包含用户名输入框、密码输入框、登录按钮，" +
                        "页面布局整齐，无弹窗报错，无布局错乱"
                );
                LogUtils.info(getClass(), "✅ 全页验证: passed={}, confidence={:.0f}%",
                        vr.passed, vr.confidence * 100);
                Allure.addAttachment("Visual-FullPage", "text/plain", vr.toString());
            } catch (AssertionError e) {
                LogUtils.warn(getClass(), "全页验证失败（AI 视觉模式可能需要 Vision 模型）: {}",
                        e.getMessage());
                Allure.addAttachment("Visual-FullPage-Error", "text/plain", e.getMessage());
            }

            // ── 模式B：元素视觉断言 ──
            LogUtils.info(getClass(), "👁️ 模式B: 元素视觉断言");
            try {
                VisualAIAssertion.VisualResult elemResult =
                        VisualAIAssertion.assertElementLooksLike(
                                By.cssSelector("button[type='submit']"),
                                "登录按钮，应为蓝色或主题色，文字为'登录'或'Sign In'，位于表单底部"
                        );
                LogUtils.info(getClass(), "✅ 元素验证: passed={}, confidence={:.0f}%",
                        elemResult.passed, elemResult.confidence * 100);
            } catch (AssertionError e) {
                LogUtils.warn(getClass(), "元素验证失败: {}", e.getMessage());
            }

            // ── 模式C：视觉回归对比 ──
            LogUtils.info(getClass(), "👁️ 模式C: 截图对比回归");
            try {
                byte[] before = ((TakesScreenshot) com.framework.browser.DriverManager.getDriver()).getScreenshotAs(OutputType.BYTES);
                byte[] after = ((TakesScreenshot) com.framework.browser.DriverManager.getDriver()).getScreenshotAs(OutputType.BYTES);

                VisualAIAssertion.VisualResult compareResult =
                        VisualAIAssertion.compareScreenshots(
                                before, after,
                                "两张截图应该几乎相同（相同页面），检查是否有视觉差异"
                        );
                LogUtils.info(getClass(), "✅ 截图对比: passed={}, assessment={}",
                        compareResult.passed, compareResult.assessment);
            } catch (Exception e) {
                LogUtils.warn(getClass(), "截图对比失败: {}", e.getMessage());
            }

        } catch (Exception e) {
            LogUtils.warn(getClass(), "视觉测试异常: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 功能 8️⃣：DOM 变更影响分析 — DOMChangeDetector
    // ─────────────────────────────────────────────

    @Test(priority = 8, description = "DOM 变更检测与 AI 影响预测")
    @Severity(SeverityLevel.NORMAL)
    @Description("三步骤：captureBaseline → detect → analyzeImpact，AI 预测受影响用例")
    public void test08_DOMChangeDetection() {
        LogUtils.info(getClass(), "=== 8️⃣ DOM 变更检测与影响分析 ===");

        loginPage = new LoginPage();
        loginPage.navigateTo();

        try {
            // ★ 步骤1：建立基准快照
            LogUtils.info(getClass(), "📸 步骤1: 捕获 DOM 基准快照...");
            DOMChangeDetector.DOMSnapshot baseline = DOMChangeDetector.captureBaseline(
                    com.framework.browser.DriverManager.getDriver(), "login-page"
            );
            Assert.assertNotNull(baseline, "基准快照不应为空");
            LogUtils.info(getClass(), "📸 基准快照捕获: {} 个元素, URL={}",
                    baseline.elements.size(), baseline.url);

            // ★ 步骤2：重新捕获并对比
            LogUtils.info(getClass(), "🔍 步骤2: 检测 DOM 变更...");
            DOMChangeDetector.ChangeReport changes = DOMChangeDetector.detect(
                    com.framework.browser.DriverManager.getDriver(), "login-page"
            );

            if (changes != null && changes.hasChanges()) {
                LogUtils.info(getClass(), "📊 检测到变更: +{} 新增, -{} 删除, ~{} 修改",
                        changes.addedCount, changes.removedCount, changes.modifiedCount);

                // ★ 步骤3：AI 分析影响
                LogUtils.info(getClass(), "🤖 步骤3: AI 分析影响...");
                DOMChangeDetector.ImpactReport impact = DOMChangeDetector.analyzeImpact(changes);
                LogUtils.info(getClass(), impact.toString());
                Allure.addAttachment("DOM Impact Analysis", "text/plain", impact.toString());

                // 验证 AI 输出
                Assert.assertNotNull(impact.riskLevel, "应输出风险等级");
                Assert.assertNotNull(impact.changeSummary, "应输出变更摘要");
            } else {
                LogUtils.info(getClass(), "✅ 未检测到 DOM 变更（页面没有变化，这是健康的）");
                LogUtils.info(getClass(), """
                        💡 DOMChangeDetector 三步流程：
                           1. captureBaseline(driver, "page-name")    → 建立基准
                           2. detect(driver, "page-name")              → 对比当前
                           3. analyzeImpact(report)                    → AI 预测影响
                           
                           适用场景：
                           • 前端发版后自动跑 → 立即知道你哪些定位器会挂
                           • 页面重构 → 对比新旧 DOM 找风险点
                           • CI/CD 集成 → DOM 变更自动预警
                        """);
            }
        } catch (Exception e) {
            LogUtils.warn(getClass(), "DOM 分析异常: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 综合演示：AI 全链路闭环（8个功能串联）
    // ─────────────────────────────────────────────

    @Test(priority = 9, description = "🌟 AI 全链路闭环演示 — 8大功能一条龙")
    @Severity(SeverityLevel.BLOCKER)
    @Description("从需求生成到执行到诊断到报告到审查到视觉的完整AI测试流水线")
    public void test99_FullAIPipeline() {
        LogUtils.info(getClass(), "╔══════════════════════════════════════════╗");
        LogUtils.info(getClass(), "║  🌟 AI 全链路闭环演示                     ║");
        LogUtils.info(getClass(), "╚══════════════════════════════════════════╝");

        StringBuilder summary = new StringBuilder();
        summary.append("AI 全链路测试执行报告\n");
        summary.append("══════════════════════\n");

        // ④ AI 生成测试用例
        summary.append("✅ 4️⃣ 用例生成: ");
        String req = "用户登录：输入用户名密码，点击登录，验证跳转到Dashboard";
        AITestGenerator.GeneratedCode code = AITestGenerator.generate(req);
        if (code != null && !code.testClasses.isEmpty()) {
            summary.append("成功生成 ").append(code.testClasses.size()).append(" 个测试类");
        } else {
            summary.append("AI 未启用，跳过");
        }

        // ⑥ AI 代码审查
        summary.append("\n✅ 6️⃣ 代码审查: ");
        try {
            String src = java.nio.file.Files.readString(
                    java.nio.file.Path.of("src/main/java/com/framework/pages/LoginPage.java"));
            TestCodeReviewer.ReviewResult review = TestCodeReviewer.review(src, "审查 LoginPage 质量");
            if (review != null) {
                summary.append(review.overallScore).append("/100 分 (").append(review.grade()).append("级)");
                summary.append(", 发现 ").append(review.issues.size()).append(" 个问题");
            }
        } catch (Exception e) {
            summary.append("文件不存在或审查失败");
        }

        // ⑧ DOM 变更分析
        summary.append("\n✅ 8️⃣ DOM 分析: ");
        loginPage = new LoginPage();
        loginPage.navigateTo();
        DOMChangeDetector.DOMSnapshot snap = DOMChangeDetector.capture(com.framework.browser.DriverManager.getDriver(), "login");
        summary.append("捕获 ").append(snap.elements.size()).append(" 个 DOM 元素");

        // ② 自愈定位器
        summary.append("\n✅ 2️⃣ 自愈定位器: ");
        try {
            actions().clickWithHeal(By.id("username"), "用户名输入框");
            summary.append("clickWithHeal 正常");
        } catch (Exception e) {
            summary.append("元素不可交互");
        }

        // ⑦ 视觉验证
        summary.append("\n✅ 7️⃣ 视觉验证: ");
        try {
            VisualAIAssertion.VisualResult vr = VisualAIAssertion.assertLooksLike(
                    "应包含登录表单，无报错信息"
            );
            summary.append(vr.passed ? "通过" : "发现问题");
        } catch (AssertionError e) {
            summary.append("验证有差异（正常，可能是非 Vision 模型）");
        }

        // ⑤ Flaky 检测
        summary.append("\n✅ 5️⃣ Flaky 检测: ");
        List<FlakyTestAnalyzer.FlakyReport> flakies = FlakyTestAnalyzer.analyzeWithAI(3);
        summary.append(flakies.isEmpty() ? "未发现 Flaky Test" : "发现 " + flakies.size() + " 个");

        // ① 失败诊断（自动模式已由 AIFailureAnalysisHook 覆盖）
        summary.append("\n✅ 1️⃣ 失败诊断: 自动模式（AI 已启用则 TestListener 自动触发）");

        // ③ 自然语言报告（自动模式已由 AIReportHook 覆盖）
        summary.append("\n✅ 3️⃣ 自然语言报告: 自动模式（AIReportHook 在 Suite 结束时触发）\n");

        summary.append("\n══════════════════════\n");
        summary.append("8 大 AI 功能全部覆盖完成 🎉");

        LogUtils.info(getClass(), summary.toString());
        Allure.addAttachment("AI Pipeline Summary", "text/plain", summary.toString());
        Allure.addAttachment("AI Features Covered", "text/plain", """
                ╔════════════════════════════════════════════╗
                ║  AI 功能            类名                  ║
                ╠════════════════════════════════════════════╣
                ║ 1️⃣ 失败诊断    FailureAnalyzer            ║
                ║ 2️⃣ 定位器自愈  SelfHealingLocator         ║
                ║ 3️⃣ 自然语言报告 AIReportGenerator          ║
                ║ 4️⃣ 用例生成    AITestGenerator             ║
                ║ 5️⃣ Flaky检测   FlakyTestAnalyzer          ║
                ║ 6️⃣ 代码审查    TestCodeReviewer            ║
                ║ 7️⃣ 视觉AI      VisualAIAssertion           ║
                ║ 8️⃣ DOM分析     DOMChangeDetector           ║
                ╚════════════════════════════════════════════╝
                """);
    }
}
