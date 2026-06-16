# Selenium WebUI Automation Framework

> **企业级 Selenium 自动化测试框架** — 从设计模式到生产就绪的全栈解决方案。  
> 基于 Java 17 + Selenium 4.30 + TestNG + Allure，集成 8 项 AI 能力与事件驱动架构。

[![Java 17](https://img.shields.io/badge/Java-17-blue)](https://adoptium.net/)
[![Selenium 4.30](https://img.shields.io/badge/Selenium-4.30-green)](https://www.selenium.dev/)
[![Allure](https://img.shields.io/badge/Allure-2.29-orange)](https://allurereport.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

---

## 目录

- [一、核心设计理念](#一核心设计理念)
- [二、系统架构](#二系统架构)
- [三、快速开始](#三快速开始)
- [四、项目结构](#四项目结构)
- [五、配置体系](#五配置体系)
- [六、核心能力详解](#六核心能力详解)
- [七、AI 能力清单](#七ai-能力清单)
- [八、Mock 学习模式](#八mock-学习模式)
- [九、CI/CD 与 DevOps](#九cicd-与-devops)
- [十、设计模式一览](#十设计模式一览)
- [十一、版本历史](#十一版本历史)

---

## 一、核心设计理念

### 架构原则

| 原则 | 体现 |
|---|---|
| **单一职责 (SRP)** | ConfigManager / AILifecycleManager / BrowserFactory / WaitStrategyFactory 各司其职 |
| **开闭原则 (OCP)** | 新增浏览器只加 Provider；新增分析维度只加 Hook，TestListener 零改动 |
| **依赖倒置 (DIP)** | EventBus 事件总线解耦发布者与订阅者；ElementHealingStrategy 接口抽象自愈 |
| **组合优于继承** | CompositeHealingStrategy 组合 Healenium + AI; 责任链模式串联自愈策略 |
| **策略模式** | WaitStrategy 支持切换等待策略；BrowserProvider 统一浏览器创建 |
| **观察者模式** | TestListener → EventBus → Hook 的完整事件分发链路 |
| **建造者模式** | TestDataBuilder 分步构建测试数据；RetryWithBackoff.Builder 配置重试策略 |
| **模板方法模式** | BaseTest 定义测试骨架，子类只写业务逻辑；BasePage 提供通用页面方法 |
| **工厂模式** | BrowserFactory / ElementFactory / WaitStrategyFactory 统一创建入口 |

### 安全设计

- **DataSanitizer** 自动脱敏 API Key、Token、密码等信息，防止敏感数据泄漏到日志和报告
- **AIClient** API Key 从多级来源解析：config.yaml → 环境变量 → 系统属性，不留硬编码

### 线程安全设计

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  线程 A       │    │  线程 B       │    │  线程 C       │
│ WebDriver #1  │    │ WebDriver #2  │    │ WebDriver #3  │
│ ElementAct#1  │    │ ElementAct#2  │    │ ElementAct#3  │
│ WaitStra#1    │    │ WaitStra#2    │    │ WaitStra#3    │
│ Screenshot#1  │    │ Screenshot#2  │    │ Screenshot#3  │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           ▼
                  ThreadLocal 容器
```

所有线程状态通过 `ThreadLocal` 隔离。DriverManager、ElementActions、WaitStrategy 等均在每个线程中独立存储，支持 maven -DparallelCount=N 并行执行。

---

## 二、系统架构

```
┌────────────────────────────────────────────────────────────────────┐
│                       TestNG Suite                                 │
├────────────────────────────────────────────────────────────────────┤
│  EventBus ─── TestListener ─── HookRegistration                    │
│     │                                                              │
│     ├── ScreenshotHook       (失败截图, 按配置启用)                  │
│     ├── AIFailureAnalysisHook (AI 智能诊断)                         │
│     ├── AIReportHook          (自然语言报告)                        │
│     ├── FlakyDetectionHook    (Flaky 检测)                          │
│     ├── HistoryRecorderHook   (执行历史)                            │
│     ├── AllureEnvironmentHook (Allure 环境注入)                     │
│     ├── AccessibilityHook     (WCAG 无障碍扫描, 按配置启用)           │
│     └── NotificationHook      (钉钉通知, 按配置启用)                 │
├────────────────────────────────────────────────────────────────────┤
│  BaseTest ─── @BeforeSuite / @BeforeMethod / @AfterMethod           │
│    ├── ConfigManager.init()       配置管理                          │
│    ├── HookRegistration.registerAll() 事件订阅注册                   │
│    ├── AILifecycleManager.init()  AI 客户端初始化 (优雅降级)          │
│    ├── AdaptiveRetryAnalyzer.resetHistory() 自适应重试               │
│    ├── DataLifecycleManager       数据生命周期管理                   │
│    ├── DriverManager.startDriver() 浏览器初始化                      │
│    │   └── BrowserFactory.createDriver()                           │
│    │       └── ChromeProvider / FirefoxProvider / ...               │
│    └── ElementActions / ScreenshotHelper / WaitStrategy 注入        │
├────────────────────────────────────────────────────────────────────┤
│  Page Object Model ─── BasePage                                    │
│    ├── LoginPage / DashboardPage / ...  (By 常量模式)               │
│    ├── AnnotatedLoginPage / AnnotatedCartPage  (@FindBy 注解模式)    │
│    └── ElementActions ─── HealingStrategy (可插拔)                  │
│          ├── HealeniumHealingStrategy  (历史数据, 毫秒级)            │
│          ├── AIHealingStrategy         (LLM 推理, 秒级)              │
│          └── CompositeHealingStrategy  (组合: Healenium → AI)       │
├────────────────────────────────────────────────────────────────────┤
│  BrowserFactory ─── Provider 注册表 (ConcurrentHashMap)             │
│    ├── ChromeProvider   │  FirefoxProvider                          │
│    └── EdgeProvider     │  SafariProvider                           │
│    (支持运行时注册: BrowserFactory.register(new BraveProvider()))     │
├────────────────────────────────────────────────────────────────────┤
│  ConfigManager (单例) ─── 分层配置                                  │
│    config.yaml → config-{env}.yaml → 系统属性                       │
│    ├── FrameworkConfig (顶层 POJO)                                  │
│    │   ├── TimeoutConfig / BrowserOptionsConfig                     │
│    │   ├── ReportingConfig / ExecutionConfig                        │
│    │   ├── AIConfig (含子配置) / AccessibilityConfig                │
│    └── DataSanitizer (日志脱敏过滤)                                  │
├────────────────────────────────────────────────────────────────────┤
│  Mock 基础设施 ─── 零依赖学习模式                                    │
│    ├── MockWebDriver  (完整 WebDriver 接口实现)                     │
│    ├── MockWebElement (元素模拟)                                    │
│    └── MockBaseTest   (测试基类)                                    │
└────────────────────────────────────────────────────────────────────┘
```

### 事件驱动流程

```
TestNG 事件                     EventBus                    Hook 订阅者
───────────                   ─────────                   ────────────
Suite Started  ─────────────  post()  ────────  HistoryRecorderHook.onSuiteStart()
                                                            AccessibilityHook  (启用时)
                                                            NotificationHook   (启用时)

Test Started   ─────────────  post()  ────────  HistoryRecorderHook.onTestStart()

Test Failure   ─────────────  post()  ────────  ScreenshotHook.onTestFailure()     (按配置)
                                                AIFailureAnalysisHook.onTestFailure() (AI启用时)
                                                FlakyDetectionHook (AI启用时)

Suite Finished ─────────────  post()  ────────  AIReportHook.onSuiteFinish()  (AI启用时)
                                                NotificationHook.onSuiteFinish() (按配置)
```

---

## 三、快速开始

### 前置要求

- **Java 17+** (推荐 Adoptium Temurin)
- **Maven 3.8+** (推荐 3.9.x)
- **Docker Desktop** (可选, 用于 Selenium Grid 集群)
- **浏览器**: Chrome / Firefox / Edge (默认使用本地已安装浏览器)

### 安装 & 运行

```bash
# 1. 编译
mvn clean compile

# 2. Mock 测试（无需浏览器，零依赖）
mvn test -Dtest="mock.*"

# 3. 实浏览器测试（默认 Chrome）
mvn test

# 4. 指定浏览器
mvn test -Dbrowser=firefox

# 5. 指定环境（读取 config-qa.yaml 覆盖）
mvn test -Denv=qa

# 6. 并行执行
mvn test -DparallelCount=4

# 7. Docker Grid 集群（需要 Docker）
docker compose up -d
mvn test -DexecutionMode=REMOTE

# 8. 生成 Allure 报告
mvn allure:serve

# 9. 运行 Demo 测试（展示 AI 全功能）
mvn test -Dtest="com.framework.demo.AIAllFeaturesTest"
```

### 快速编写测试

```java
public class LoginTest extends BaseTest {

    // 方式 A: By 常量模式的 Page Object
    @Test(description = "有效凭据登录成功")
    public void testSuccessfulLogin() {
        DashboardPage dashboard = new LoginPage().navigateTo()
                .login("admin", "password123");
        Assert.assertTrue(dashboard.isAt(), "应跳转 Dashboard");
    }

    // 方式 B: @FindBy 注解模式
    @Test(description = "无效凭据登录失败")
    public void testLoginInvalid() {
        AnnotatedLoginPage loginPage = new AnnotatedLoginPage().navigateTo();
        loginPage.loginExpectingFailure("invalid", "wrong");
        Assert.assertTrue(loginPage.hasError(), "应显示错误提示");
    }
}
```

---

## 四、项目结构

```
src/
├── main/java/com/framework/
│   ├── ai/
│   │   ├── client/           # AI 客户端 (AIClient, AIConfig)
│   │   ├── analysis/         # 失败诊断 / Flaky检测 / 代码审查 / 历史记录
│   │   ├── assertion/        # Visual AI 断言
│   │   ├── generator/        # AI 报告生成 / 测试用例生成
│   │   ├── healing/          # 元素自愈 (Locator自愈 / DOM变更检测)
│   │   └── lifecycle/        # AI 生命周期管理
│   ├── annotations/          # 自定义注解 (@TestCaseInfo, @RetryConfig, @SkipIf, @Desc)
│   ├── browser/              # 浏览器驱动管理
│   │   └── provider/         # Chrome / Firefox / Edge / Safari 提供者
│   ├── builder/              # 测试数据建造者模式
│   ├── config/               # 配置 POJO + YAML加载
│   │   ├── browser/          # 超时 / 浏览器选项
│   │   ├── execution/        # 执行配置
│   │   ├── reporting/        # 报告配置
│   │   └── accessibility/    # WCAG 无障碍配置
│   ├── core/                 # 核心
│   │   ├── hook/             # 7 个事件 Hook
│   │   ├── listener/         # HookRegistration + TestListener
│   │   ├── retry/            # 自适应 / 固定 / 指数退避重试
│   │   └── security/         # DataSanitizer 脱敏
│   ├── data/                 # 数据模型 + 工厂 + 生命周期
│   │   ├── model/            # Customer / Order / Product
│   │   └── lifecycle/        # DataLifecycleManager
│   ├── element/              # 元素操作
│   │   ├── actions/          # ElementActions (点击/输入/选择)
│   │   ├── factory/          # ElementFactory
│   │   └── healing/          # Healenium / AI / 组合自愈策略
│   ├── enums/                # BrowserType / EnvironmentType / ExecutionMode
│   ├── event/                # EventBus 事件总线
│   ├── framework/            # BaseTest / ScreenshotHelper / VerificationUtils / WaitHelper
│   ├── mock/                 # MockWebDriver / MockWebElement / MockBaseTest
│   ├── notification/         # 钉钉通知 Hook + 配置
│   ├── pages/                # Page Object 示例
│   ├── utils/                # LogUtils / DateUtils / 数据格式解析
│   └── wait/                 # WaitStrategy (接口 + 工厂 + 显式等待实现)
│
├── test/java/com/framework/
│   ├── testcases/            # 业务测试用例 (LoginTest, DashboardTest 等)
│   │   └── mock/             # Mock 快速学习测试
│   └── demo/                 # AI 全功能集成演示 (不参与 CI 主流程)
│
└── test/resources/
    ├── config/               # 多环境配置 (dev / qa / prod)
    ├── testdata/              # 数据驱动测试文件 (CSV / JSON / YAML)
    └── testng.xml             # TestNG Suite 配置
```

---

## 五、配置体系

### 配置加载优先级

```
低优先级                   高优先级
───────────────────────────────────────────────►
config.yaml  →  config-{env}.yaml  →  系统属性
                                           (-Dbrowser=firefox)
```

### 完整配置 (config.yaml)

```yaml
# 浏览器
browser: CHROME                    # CHROME / FIREFOX / EDGE / SAFARI
environment: DEV                   # DEV / QA / STAGING / PRODUCTION
executionMode: LOCAL               # LOCAL / REMOTE / GRID / DOCKER / CLOUD
baseUrl: "https://example.com"
hubUrl: "http://localhost:4444/wd/hub"

# 超时
timeouts:
  implicitWait: 10
  explicitWait: 30
  pageLoadWait: 60
  pollingInterval: 500

# 浏览器选项
browserOptions:
  headless: false
  maximize: true
  arguments: ["--disable-notifications"]
  preferences:
    download.default_directory: "${user.dir}/downloads"

# 报告
reporting:
  screenshotOnFailure: true
  retryOnFailure: true
  retryCount: 2
  adaptiveRetry: true
  maxRetryCount: 5

# 执行
execution:
  parallelCount: 2
  waitStrategy: "explicit"         # 等待策略类型

# AI 功能 (默认关闭)
ai:
  enabled: false
  provider: "deepseek"             # deepseek / openai
  model: "deepseek-chat"
  apiKey: ""                       # 或环境变量 DEEPSEEK_API_KEY
  failureAnalysis: { enabled: true }
  selfHealing: { enabled: true, maxCandidates: 3 }
  report: { enabled: true }
  visual: { enabled: true }
  domChange: { enabled: true, snapshotDir: "dom-snapshots" }

# 无障碍检测
accessibility:
  enabled: true
  minImpactLevel: "serious"
  maxViolations: 50
```

### 多环境配置示例 (config-qa.yaml)

```yaml
# 只写需要覆盖的字段
baseUrl: "https://qa.example.com"
browser: FIREFOX
timeouts:
  explicitWait: 45
```

### 环境变量

| 变量 | 用途 | 说明 |
|---|---|---|
| `OPENAI_API_KEY` | OpenAI API Key | provider=openai 时使用 |
| `DEEPSEEK_API_KEY` | DeepSeek API Key | provider=deepseek 时使用 |
| `DINGTALK_WEBHOOK` | 钉钉 Webhook URL | 未在 notification.yaml 配置时使用 |
| `TEST_ENV` | 当前环境 | 等于 -Denv 或 config-{env}.yaml |

---

## 六、核心能力详解

### 1. 多浏览器管理 ─── BrowserFactory + Provider 注册表

```java
// 创建浏览器 — 根据 config.yaml 自动选择
WebDriver driver = BrowserFactory.createDriver();

// 运行时动态注册第三方浏览器
BrowserFactory.register(new BraveProvider());

// 查看已注册的浏览器
Map<BrowserType, BrowserProvider> providers = BrowserFactory.registeredProviders();
```

底层通过 `Strategy + Factory` 组合模式，每个 `BrowserProvider` 接口统一创建本地和远程 WebDriver：

```java
// ChromeProvider 示例
public class ChromeProvider implements BrowserProvider {
    @Override
    public BrowserType browserType() { return BrowserType.CHROME; }

    @Override
    public WebDriver createLocal(ConfigManager cfg) {
        return new ChromeDriver(buildOptions(cfg));
    }

    @Override
    public WebDriver createRemote(URL hubUrl, ConfigManager cfg) {
        return new RemoteWebDriver(hubUrl, buildOptions(cfg));
    }
}
```

### 2. 元素操作 ─── ElementActions + WaitStrategy

ElementActions 封装了所有元素交互，遵循**链式调用 + 统一等待 + 日志 + Allure Step**：

```java
ElementActions.with(driver)
    .type(By.id("username"), "admin")
    .click(By.cssSelector("button[type='submit']"))
    .scrollTo(By.id("footer"))
    .waitForText(By.cssSelector(".msg"), "成功");
```

等待策略通过 `WaitStrategyFactory` 创建，默认使用 `ExplicitWaitStrategy`（FluentWait），忽略 StaleElementReferenceException 和 NoSuchElementException：

```java
// 自动从 config.yaml 读取 explicitWait(秒) 和 pollingInterval(毫秒)
WaitStrategy wait = WaitStrategyFactory.getStrategy(driver);
wait.waitForClickable(By.id("btn"));
wait.waitForPageLoad(driver);
```

### 3. 元素自愈 ─── 双引擎组合策略

当原始定位器失败时，自愈引擎按优先级尝试：

```
元素查找失败
    │
    ▼
HealeniumHealingStrategy  ──── 查询历史成功率 (毫秒级, 离线, 无 API 成本)
    │ 成功?                           ← Healenium 为可选依赖
    ├── 是 ─── 返回元素
    │
    ▼ 否
AIHealingStrategy  ──── 页面 DOM → SelfHealingLocator → LLM 推导备选 (秒级)
    │ 成功?
    ├── 是 ─── 返回元素
    │
    ▼ 否
NoSuchElementException ──── 所有策略失败
```

使用方式：

```java
// 带自愈的点击
actions.clickWithHeal(By.id("old-button"), "登录按钮");

// 带自愈的输入
actions.typeWithHeal(By.id("username"), "admin", "用户名输入框");

// 带自愈的元素查找
WebElement el = actions.findElementWithHeal(By.name("email"), "邮箱输入框");
```

Healenium 为**可选依赖**(pom.xml 中 `<optional>true</optional>`)，不在 classpath 时自动跳过，不会影响框架运行。

### 4. 自适应重试 ─── AdaptiveRetryAnalyzer

超越简单固定次数重试，具备以下能力：

- **历史感知**：跟踪每个测试方法最近 10 次执行结果
- **动态调整**：失败率 > 60% 时增加 3 次重试；连续 5 次成功时减少重试
- **异常类型感知**：断言失败不重试（无意义）；超时/StaleElement 重试（可恢复）
- **指数退避**：500ms → 1000ms → 2000ms → ... → 10s 上限

### 5. 事件驱动 Hook ─── EventBus 架构

```java
// 注册方式 A: Lambda
EventBus.register(TestEvent.Failure.class, event -> {
    ScreenshotHook.capture(event.getResult());
});

// 注册方式 B: @Subscribe 注解
EventBus.register(new MyCustomHook());

// 注册方式 C: HookRegistration (框架已集成)
// 见 HookRegistration.java

// 发布事件
EventBus.post(new TestEvent.Failure(result));
```

7 个内置 Hook 各司其职：

| Hook | 触发事件 | 是否可配置 |
|---|---|---|
| ScreenshotHook | TestEvent.Failure | config.yaml reporting.screenshotOnFailure |
| AIFailureAnalysisHook | TestEvent.Failure | AI 启用 + ai.failureAnalysis 配置 |
| AIReportHook | SuiteEvent.Finished | AI 启用 + ai.report 配置 |
| FlakyDetectionHook | SuiteEvent.Finished | AI 启用 + 统计数据分析 |
| HistoryRecorderHook | 全部测试事件 | 始终启用 |
| AllureEnvironmentHook | SuiteEvent.Started | 始终启用 |
| AccessibilityHook | TestEvent.Success/Failure | accessibility.enabled 配置 |
| NotificationHook | SuiteEvent.Finished | notification.yaml enabled 配置 |

### 6. 配置驱动的条件开关

| 配置开关 | 控制行为 | 默认 |
|---|---|---|
| `reporting.screenshotOnFailure` | 失败时自动截图 | true |
| `notification.enabled` | 钉钉群通知 | true |
| `accessibility.enabled` | WCAG 无障碍检测 | true |
| `ai.enabled` | 全部 AI 功能 | false |
| `ai.failureAnalysis.enabled` | AI 失败诊断 | true (需 ai.enabled) |
| `ai.selfHealing.enabled` | AI 定位器自愈 | true (需 ai.enabled) |
| `ai.report.enabled` | AI 自然语言报告 | true (需 ai.enabled) |
| `reporting.adaptiveRetry` | 自适应重试 | true |

### 7. 日志脱敏 ─── DataSanitizer

自动过滤 API Key、Token、密码等信息，防止敏感数据泄漏：

```java
// API Key 匹配：sk-xxx... / F-xxx...
String safe = DataSanitizer.sanitize(rawResponse);
LogUtils.info(getClass(), "AI 响应: {}", safe);

// 检测是否包含敏感信息
if (DataSanitizer.containsSensitiveData(input)) {
    LogUtils.warn(getClass(), "检测到敏感信息被记录!");
}
```

### 8. 软断言 ─── VerificationUtils

```java
VerificationUtils.softAssertionsBegin();

VerificationUtils.softAssertEquals(actual1, expected1, "价格");
VerificationUtils.softAssertTrue(condition2, "状态");
VerificationUtils.softAssertEquals(actual3, expected3, "数量");

// 汇总所有失败，没有失败则不抛异常
VerificationUtils.softAssertionsAssertAll();
```

**安全机制**: 未调用 `softAssertionsBegin()` 直接调用 `softAssertionsAssertAll()` 会抛出明确错误提示。

---

## 七、AI 能力清单

| # | 能力 | 类 | 触发方式 | 依赖 AI | 备注 |
|---|---|---|---|---|---|
| 1 | 失败智能诊断 | FailureAnalyzer | 自动 (Hook) + 手动 API | ✅ | 发送 DOM + 堆栈给 LLM |
| 2 | 定位器自愈 | SelfHealingLocator | 手动调用 | ✅ | 双引擎: Healenium + AI |
| 3 | 自然语言报告 | AIReportGenerator | 自动 (Hook) + 手动 API | ✅ | 纯文本摘要 |
| 4 | 需求→用例生成 | AITestGenerator | 手动 API | ✅ | 生成 PageObject + TestNG |
| 5 | Flaky Test 检测 | FlakyTestAnalyzer | 自动 (Hook) + 手动 API | ⚡ 可选 | 统计 → AI 诊断 |
| 6 | 代码审查 | TestCodeReviewer | 手动 API | ✅ | 8 类问题自动识别 |
| 7 | 视觉 AI 测试 | VisualAIAssertion | 手动 API | ✅ | 语义理解代替像素 |
| 8 | DOM 变更分析 | DOMChangeDetector | 手动 API | ✅ | 三步: 基准→检测→预测 |

### 优雅降级设计

所有 AI 功能遵循**三态设计**：

```
AI 状态                  功能表现
─────────────           ──────────
enabled=true + API正常    → AI 真实推理 (全功能)
enabled=true + API异常    → 返回 null / fallback (不阻塞测试)
enabled=false            → 跳过所有 AI 调用 (零成本)
```

这意味着即使 AI 服务不可用，所有测试仍可正常运行。

---

## 八、Mock 学习模式

### 设计目标

- 零环境依赖：无需安装任何浏览器或驱动
- 快速学习：新人可以在 30 秒内跑通第一个测试
- 无缝切换：改一行 `extends MockBaseTest → extends BaseTest` 即切换到真实浏览器

### MockWebDriver 模拟能力

```
模拟页面映射:
/login        → 用户登录
/dashboard    → 控制台
/products     → 商品列表
/product/N    → 商品详情
/cart         → 购物车
/checkout     → 结算确认
/order/success → 下单成功
/orders       → 我的订单
```

页面 URL 从 `ConfigManager.baseUrl()` 读取，默认 `https://example.com`。

### 使用方式

```java
public class MockLoginUITest extends MockBaseTest {

    @Test
    public void testLoginSuccess() {
        AnnotatedLoginPage loginPage = new AnnotatedLoginPage(mockDriver);

        AnnotatedDashboardPage dashboard = loginPage.navigateTo()
                .login("admin", "password123");

        // Mock 验证：检查关键操作是否执行
        Assert.assertTrue(mockDriver.hasPerformed("admin"), "应输入用户名");
        Assert.assertTrue(mockDriver.hasPerformed("password"), "应输入密码");
        Assert.assertTrue(mockDriver.getCurrentUrl().contains("dashboard"), "应跳转");
    }
}
```

### 操作验证

```java
// 获取全部操作历史
List<String> history = mockDriver.getActionHistory();

// 查看是否包含某操作
mockDriver.hasPerformed("登录按钮");

// 打印完整时间线
mockDriver.printTimeline();
```

---

## 九、CI/CD 与 DevOps

### Docker Selenium Grid

```bash
# 启动 1 Chrome + 1 Edge + 1 Firefox 集群
docker compose up -d

# 水平扩展 Chrome 节点
docker compose up -d --scale chrome=4

# 运行分布式测试
mvn test -DexecutionMode=GRID -DhubUrl=http://localhost:4444/wd/hub -DparallelCount=6
```

### Dockerfile

基于 Maven 的多阶段构建：

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build
COPY . /app
RUN mvn -f /app clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre
COPY --from=build /app/target/*.jar /app/
```

### Helm Chart (Kubernetes)

`k8s/helm-chart/` 包含：

| 模板 | 功能 |
|---|---|
| `grid-deployment.yaml` | Selenium Grid Hub + Node 部署 |
| `pvc.yaml` | Allure 报告持久卷 |
| `test-runner-job.yaml` | 测试执行 Job |
| `values.yaml` | 参数化配置 |

### CI Pipeline (GitHub Actions)

```yaml
# .github/workflows/ci.yml
jobs:
  quality:     mvn compile + checkstyle
  mock-tests:  mvn test -Dtest="mock.*"       # 零依赖快速反馈
  browser-matrix:                             # Chrome + Firefox + Edge
    strategy:
      matrix:
        browser: [chrome, firefox, edge]
    steps: mvn test -Dbrowser=${{ matrix.browser }}
  allure-report:   allure:report + GitHub Pages
```

### Pre-commit Hook

```yaml
# .pre-commit-config.yaml
repos:
  - repo: local
    hooks:
      - id: checkstyle
        name: Checkstyle
        entry: mvn checkstyle:check
        language: system
        pass_filenames: false
```

---

## 十、设计模式一览

| 模式 | 使用位置 | 说明 |
|---|---|---|
| **单例 (Singleton)** | ConfigManager, EventBus, AIClient, DataLifecycleManager | 全局唯一实例，双重检查锁定 |
| **工厂方法 (Factory)** | BrowserFactory, ElementFactory, WaitStrategyFactory, TestDataFactory | 统一创建入口 |
| **策略 (Strategy)** | WaitStrategy, BrowserProvider, ElementHealingStrategy, RetryAnalyzer | 算法族可互换 |
| **模板方法 (Template)** | BaseTest, BasePage | 定义骨架，子类实现细节 |
| **建造者 (Builder)** | TestDataBuilder, RetryWithBackoff.Builder | 分步构建复杂对象 |
| **观察者 (Observer)** | EventBus → Hook 订阅者 | 事件分发，完全解耦 |
| **组合 (Composite)** | CompositeHealingStrategy | 将多个自愈策略组合为一条责任链 |
| **责任链 (Chain of Resp.)** | Healenium → AI 自愈流程 | 每层尝试，命中即返回 |
| **适配器 (Adapter)** | HookRegistration 将 TestEventHook 适配到 EventBus | 接口兼容转换 |
| **线程局部存储 (ThreadLocal)** | DriverManager, ElementActions 等 | 线程安全隔离 |
| **依赖注入 (DI)** | BasePage(WebDriver), ElementActions(WebDriver) | 通过构造器注入依赖 |

---

## 十一、版本历史

| 版本 | 日期 | 变更 |
|---|---|---|
| **3.2.1** | 2026-06-08 | **代码审查后修复**: HookRegistration 改用 AtomicBoolean 线程安全注册；ConfigLoader BrowserOptionsConfig 默认值 null-safe 合并；testng.xml XML 格式清洗；AIAllFeaturesTest 去重；MockWebDriver 线程安全双重检查锁定；WaitStrategyFactory 超时方法语义澄清；40+ 预存编译错误全量修复，`mvn compile` + `mvn test-compile` 零错误通过 |

| **3.2.0** | 2026-06-07 | **全面修复增强**: 修复 `ExplicitWaitStrategy` 静态初始化时机 (改为延迟加载)；Healenium 改为 optional 依赖 (非注释)；`ConfigLoader.merge()` 完整覆盖所有配置字段；截图/通知开关按配置生效 (不再始终注册)；`MockWebDriver` 支持动态 baseUrl (从 ConfigManager 读取)；`WaitStrategyFactory` 支持扩展注册；AIAllFeaturesTest 移至 demo 目录 (不参与 CI)；软断言增加模式检查；ElementActions 消除 highlight 方法重名冲突 |
| **3.1.0** | 2026-06-02 | 企业级架构: EventBus 事件驱动 + 自适应重试 + 安全加固 + CI/CD |
| **3.0.0** | 2026-05-25 | SOLID 重构 + Provider 注册表 + Healenium 集成 |

---

## 更多文档

| 文档 | 说明 |
|---|---|
| [UI_AUTOMATION_GUIDE.md](UI_AUTOMATION_GUIDE.md) | 从零开始的 UI 自动化入门指南 |
| [CHANGELOG.md](CHANGELOG.md) | 完整版本变更日志 |
| [INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md) | 自动化测试面试指南 |
| [checkstyle.xml](checkstyle.xml) | 代码规范配置 |
| [SAMPLE_REPORT.md](SAMPLE_REPORT.md) | Allure 报告示例 |
| 钉钉通知配置 | `notification.yaml` |

## 许可证

MIT License
