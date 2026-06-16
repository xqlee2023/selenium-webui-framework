# Changelog

## 3.2.0 (2026-06-08) — 代码审查后修复增强

### 🔴 严重修复

- **HookRegistration 线程安全**: `registerAll()` 从 `volatile boolean` 改为 `AtomicBoolean.compareAndSet()` 原子检查，防止并行测试中 EventBus 订阅者被重复注册
- **ConfigLoader BrowserOptionsConfig 默认值污染**: `headless`/`maximize` 改为 `Boolean` 包装类型 + null 检查，避免环境覆盖文件中未显式指定的字段被 Jackson 默认值覆盖

### 🟡 主要修复

- **testng.xml 清洗**: 删除 `</suite>` 后多余的重复 XML 块（"AI Demo Tests" 声明重复 3 次）
- **AIAllFeaturesTest 去重**: 删除 `testcases` 包下的副本，只保留 `demo` 包版本（后者已被标记为不参与 CI 主流程）
- **WaitStrategyFactory 超时方法**: `getStrategy(driver, timeout)` 明确标注只返回 `ExplicitWaitStrategy`，避免与自定义策略注册混淆
- **MockWebDriver 线程安全懒加载**: `getPages()` 改为双重检查锁定 + `volatile`，防止并行测试中重复构建

### 💭 优化

- **ExplicitWaitStrategy**: 缓存 `ConfigManager.get()` 返回值，减少重复静态方法调用
- **pom.xml**: 删除 `checkstyle.skip` 的重复声明（properties 中已有，plugin configuration 中多余）

### 架构改进方案（待实施）

架构师和资深开发者评估后输出的后续改进方向：

| 改进项 | 优先级 | 工作量 |
|---|---|---|
| P0 — 敏感配置泄漏（API Key/Webhook 仅从环境变量读取） | P0 | ~2h |
| P1 — AIClient 多实例管理（从单例改为多命名客户端） | P1 | ~5h |
| P2 — HealingStrategyProvider（自愈策略从 ElementActions 分离） | P1 | ~2h |
| P3 — 统一 YAML 加载（NotificationConfig 改为 Jackson POJO） | P2 | ~2h |
| P4a — BrowserOptionApplier 重复代码消除 | P3 | ~1h |
| P4b — EventBus 反射异常保留完整 cause 链 | P3 | ~0.5h |
| P4c — AI 调用 Metrics 采集 | P3 | ~1h |
| P4d — 自适应重试历史可选持久化 | P3 | ~1.5h |

---

## 3.1.0 (2026-06-07) — 全面修复增强

### 🔴 关键修复

- **ExplicitWaitStrategy 静态初始化时机**: `ConfigManager.get()` 从 `static final` 字段改为构造器参数传递，三参构造器支持自定义超时
- **Healenium 依赖**: pom.xml 中从完全注释改为 `<optional>true</optional>` + 运行时 `Class.forName` 优雅降级
- **ConfigLoader.merge() 完整化**: 从浅合并改为深合并，覆盖超时/浏览器选项/AI 配置/Accessibility 等全部字段
- **HookRegistration 配置驱动**: 截图按 `reporting.screenshotOnFailure` 注册；通知按 `notification.yaml` 的 `enabled` 注册

### 🟡 架构改进

- **WaitStrategyFactory**: 从硬编码 switch 改为 `ConcurrentHashMap` 注册表模式，支持运行时注册自定义策略
- **MockWebDriver**: 硬编码 `https://example.com` 改为从 `ConfigManager.baseUrl()` 动态读取
- **VerificationUtils**: 软断言增加 `SOFT_MODE` 标记，未调 `begin()` 时直接调 `assertAll()` 抛明确错误
- **ElementActions**: 消除 `highlight(By)` 和 `highlight(WebElement)` 重名冲突

### 🟡 预存编译错误修复

修复全项目 **40+ 处缺失的 import** 及 5 处 API 用法错误：

| 修复范围 | 具体内容 |
|---|---|
| AI 包修复 | AIClient/AIConfig 跨包引用（analysis/assertion/generator/healing 共 8 个文件） |
| 框架核心修复 | ConfigManager/DriverManager/ElementFactory 引用（hook/listener/framework 共 5 个文件） |
| 浏览器管理修复 | BrowserProvider/ChromeProvider/EdgeProvider/FirefoxProvider 共 8 处 import |
| 工具类修复 | DataProviderHelper（ExcelUtils/JsonUtils/YamlUtils/CsvUtils 4 处 import） |
| API 用法修复 | NotificationHook record 字段访问、EventBus int+boolean 运算、MockWebDriver 缺失方法 |
| 测试文件修复 | LoginTest/DashboardTest 类型链错误、AIAllFeaturesTest navigateTo() 修复 |

### 💭 其他

- **AIAllFeaturesTest**: 从 `testcases` 移至 `demo` 包，不参与 CI 主流程
- **testng.xml**: 新增 Demo 测试 section
- **README.md**: 完整重写 735 行，含架构图、设计模式表、AI 能力清单

---

## 3.0.0 (2026-05-25) — SOLID 重构

- Provider 注册表模式管理浏览器创建
- Healenium 集成 + 组合自愈策略
- EventBus 事件驱动架构
- 自适应重试 Analyze
- 安全脱敏（DataSanitizer）
- 完整支持 8 项 AI 能力

---

## 2.0.0 — 初始版本

- Selenium + TestNG + Allure 基础框架
- Page Object 模式
- 多浏览器支持
- 数据驱动测试
