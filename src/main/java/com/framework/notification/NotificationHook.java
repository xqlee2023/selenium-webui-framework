package com.framework.notification;

import com.framework.core.hook.TestEventHook;
import com.framework.utils.LogUtils;
import org.testng.ISuite;
import org.testng.ITestResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ===========================================
 * 📢 钉钉通知 Hook — 模块级精准通知
 * ===========================================
 *
 * Suite 结束后自动收集各模块测试结果，向钉钉群发送报告，
 * 并 @ 对应模块的负责人。
 *
 * <h3>通知逻辑</h3>
 * <ol>
 *   <li>收集所有用例 → 按模块分组（从 ITestResult 提取类名）</li>
 *   <li>匹配 notification.yaml 中的模块负责人</li>
 *   <li>per_module 模式：每个模块一条消息，@ 该模块负责人</li>
 *   <li>all_owners 模式：发送完整报告，@ 所有负责人</li>
 * </ol>
 *
 * @author Lee
 * @since 3.2.0
 */
public class NotificationHook implements TestEventHook {

    private static final String GREEN_CIRCLE  = "🟢";
    private static final String RED_CIRCLE    = "🔴";
    private static final String YELLOW_CIRCLE = "🟡";

    // 收集每个测试结果
    private final List<ITestResult> allResults = new ArrayList<>();

    @Override
    public void onTestSuccess(ITestResult result) {
        allResults.add(result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        allResults.add(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        allResults.add(result);
    }

    @Override
    public void onSuiteFinish(ISuite suite) {
        NotificationConfig config = NotificationConfig.load();
        if (config == null || !config.enabled) {
            LogUtils.info(getClass(), "📢 通知未启用，跳过");
            return;
        }

        // 判断是否需要通知
        String buildResult = suite.getXmlSuite().getTests().isEmpty() ? "SKIP"
                : allResults.stream().anyMatch(r -> r.getStatus() == ITestResult.FAILURE) ? "FAIL" : "PASS";
        boolean unstable = allResults.stream().anyMatch(r -> r.getStatus() == ITestResult.SKIP);

        if ("PASS".equals(buildResult) && !config.notifyOnSuccess) return;
        if ("FAIL".equals(buildResult) && !config.notifyOnFailure) return;
        if (unstable && !config.notifyOnUnstable) return;

        // 初始化钉钉客户端
        DingTalkNotifier ding = new DingTalkNotifier(config.resolveWebhook());

        // 按模块分组
        Map<String, List<ITestResult>> byModule = groupByModule();

        // 发送总体摘要
        sendSummary(ding, config, buildResult, unstable, byModule);

        // 按通知模式发送模块级报告
        if ("per_module".equals(config.mentionMode)) {
            sendPerModule(ding, config, buildResult, byModule);
        } else {
            sendAllOwners(ding, config, buildResult, byModule);
        }
    }

    // ══════════ 分组 ══════════

    private Map<String, List<ITestResult>> groupByModule() {
        Map<String, List<ITestResult>> map = new LinkedHashMap<>();
        for (ITestResult r : allResults) {
            String className = r.getTestClass().getRealClass().getSimpleName();
            map.computeIfAbsent(className, k -> new ArrayList<>()).add(r);
        }
        return map;
    }

    // ══════════ 总体摘要 ══════════

    private void sendSummary(DingTalkNotifier ding, NotificationConfig config,
                             String buildResult, boolean unstable,
                             Map<String, List<ITestResult>> byModule) {
        long total = allResults.size();
        long passed = allResults.stream().filter(r -> r.getStatus() == ITestResult.SUCCESS).count();
        long failed = allResults.stream().filter(r -> r.getStatus() == ITestResult.FAILURE).count();
        long skipped = allResults.stream().filter(r -> r.getStatus() == ITestResult.SKIP).count();
        double rate = total > 0 ? Math.round(passed * 10000.0 / total) / 100.0 : 0;

        String statusIcon = "FAIL".equals(buildResult) ? RED_CIRCLE
                : unstable ? YELLOW_CIRCLE : GREEN_CIRCLE;

        StringBuilder md = new StringBuilder();
        md.append("## ").append(statusIcon).append(" Selenium WebUI 自动化测试报告\n\n");
        md.append("> **构建:** #${BUILD_NUMBER}  |  **环境:** ${TEST_ENV}  |  **浏览器:** ${BROWSER}\n\n");
        md.append("| 指标 | 值 |\n|------|----|\n");
        md.append("| 总用例 | ").append(total).append(" |\n");
        md.append("| ✅ 通过 | ").append(passed).append(" |\n");
        md.append("| ❌ 失败 | ").append(failed).append(" |\n");
        md.append("| ⏭️ 跳过 | ").append(skipped).append(" |\n");
        md.append("| 📊 通过率 | **").append(rate).append("%** |\n\n");

        // 各模块概况
        md.append("### 📋 模块概况\n\n");
        md.append("| 模块 | 负责人 | 通过/总数 | 状态 |\n");
        md.append("|------|--------|-----------|------|\n");

        for (var entry : byModule.entrySet()) {
            String module = entry.getKey();
            List<ITestResult> results = entry.getValue();
            long p = results.stream().filter(r -> r.getStatus() == ITestResult.SUCCESS).count();
            long f = results.stream().filter(r -> r.getStatus() == ITestResult.FAILURE).count();
            String icon = f > 0 ? RED_CIRCLE : GREEN_CIRCLE;

            var owner = config.getModuleOwner(module);
            md.append("| ").append(owner.name()).append(" | ").append(owner.owner())
                    .append(" | ").append(p).append("/").append(results.size())
                    .append(" | ").append(icon).append(" |\n");
        }

        md.append("\n📎 [查看完整 Allure 报告](${BUILD_URL}allure)\n");

        ding.send("UI 自动化测试报告", md.toString());
    }

    // ══════════ per_module 模式 ══════════

    private void sendPerModule(DingTalkNotifier ding, NotificationConfig config,
                               String buildResult, Map<String, List<ITestResult>> byModule) {
        for (var entry : byModule.entrySet()) {
            String module = entry.getKey();
            List<ITestResult> results = entry.getValue();
            var owner = config.getModuleOwner(module);
            List<String> mobiles = List.of(owner.mobile());

            long p = results.stream().filter(r -> r.getStatus() == ITestResult.SUCCESS).count();
            long f = results.stream().filter(r -> r.getStatus() == ITestResult.FAILURE).count();
            String icon = f > 0 ? RED_CIRCLE : GREEN_CIRCLE;

            StringBuilder md = new StringBuilder();
            md.append("## ").append(icon).append(" ").append(owner.name())
                    .append(" 测试报告\n\n");
            md.append("> 负责人: @").append(owner.mobile()).append("\n\n");
            md.append("| 指标 | 值 |\n|------|----|\n");
            md.append("| 用例总数 | ").append(results.size()).append(" |\n");
            md.append("| ✅ 通过 | ").append(p).append(" |\n");
            md.append("| ❌ 失败 | ").append(f).append(" |\n");

            // 失败用例详情
            if (f > 0) {
                md.append("\n### ❌ 失败用例\n\n");
                results.stream()
                        .filter(r -> r.getStatus() == ITestResult.FAILURE)
                        .forEach(r -> {
                            Throwable t = r.getThrowable();
                            String reason = t != null ? t.getMessage() : "未知原因";
                            if (reason != null && reason.length() > 100) {
                                reason = reason.substring(0, 97) + "...";
                            }
                            md.append("- **").append(r.getMethod().getMethodName())
                                    .append("**\n  > ").append(reason).append("\n");
                        });
            }

            md.append("\n📎 [查看报告](${BUILD_URL}allure)\n");

            ding.send(owner.name() + " 测试报告", md.toString(), mobiles, false);
        }
    }

    // ══════════ all_owners 模式 ══════════

    private void sendAllOwners(DingTalkNotifier ding, NotificationConfig config,
                               String buildResult, Map<String, List<ITestResult>> byModule) {
        List<String> allMobiles = byModule.keySet().stream()
                .map(config::getModuleOwner)
                .map(o -> o.mobile())
                .distinct()
                .collect(Collectors.toList());

        StringBuilder md = new StringBuilder();
        md.append("## 📋 全模块测试报告\n\n");
        md.append("> 请各模块负责人查看\n\n");

        for (var entry : byModule.entrySet()) {
            String module = entry.getKey();
            List<ITestResult> results = entry.getValue();
            var owner = config.getModuleOwner(module);
            long p = results.stream().filter(r -> r.getStatus() == ITestResult.SUCCESS).count();
            long f = results.stream().filter(r -> r.getStatus() == ITestResult.FAILURE).count();

            md.append("**").append(owner.name()).append("** ")
                    .append(f > 0 ? RED_CIRCLE : GREEN_CIRCLE)
                    .append(" ").append(p).append("/").append(results.size())
                    .append(" — @").append(owner.mobile()).append("\n");
        }

        md.append("\n📎 [查看完整报告](${BUILD_URL}allure)\n");

        ding.send("全模块测试报告", md.toString(), allMobiles, false);
    }
}
