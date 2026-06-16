package com.framework.data.lifecycle;

import com.framework.utils.LogUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ===========================================
 * 🔄 测试数据生命周期管理器 (DataLifecycleManager)
 * ===========================================
 *
 * 统一管理测试数据的创建、清理和隔离。
 * 确保不同测试之间数据不相互干扰，测试完成后数据按策略清理。
 *
 * 设计原则：
 *   - 每个测试或 suite 拥有独立的数据上下文
 *   - 支持按 scope（SUITE / CLASS / METHOD）自动清理
 *   - 可扩展的 DataSetupHandler 接口，支持不同数据源
 *   - 支持回调：测试前后执行数据准备和清理
 *
 * 使用方式：
 *   DataLifecycleManager manager = DataLifecycleManager.getInstance();
 *
 *   // 注册清理器
 *   manager.registerHandler("user", userId -> deleteUser(userId));
 *
 *   // 创建数据
 *   String userId = manager.createData("user", () -> createTestUser());
 *
 *   // 按 scope 清理
 *   manager.cleanup(MethodScope.INSTANCE);
 */
public final class DataLifecycleManager {

    private static volatile DataLifecycleManager instance;

    /** 数据清理处理器注册表 */
    private final ConcurrentHashMap<String, List<DataCleanupHandler>> cleanupHandlers = new ConcurrentHashMap<>();

    /** 按 scope 追踪创建的数据 */
    private final Map<String, Set<String>> suiteData = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> classData = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> methodData = new ConcurrentHashMap<>();

    /** 数据创建时的回调（用于 setup） */
    private final List<Runnable> preSuiteHooks = new ArrayList<>();
    private final List<Runnable> postSuiteHooks = new ArrayList<>();

    private DataLifecycleManager() {}

    public static DataLifecycleManager getInstance() {
        if (instance == null) {
            synchronized (DataLifecycleManager.class) {
                if (instance == null) {
                    instance = new DataLifecycleManager();
                    LogUtils.info(DataLifecycleManager.class, "测试数据生命周期管理器已初始化");
                }
            }
        }
        return instance;
    }

    // ========== 数据创建与跟踪 ==========

    /**
     * 创建数据并自动跟踪其生命周期。
     *
     * @param category 数据类别（如 "user", "order", "product"）
     * @param supplier 数据创建逻辑
     * @param <T>      数据类型
     * @return 创建的数据标识符
     */
    public <T> String createData(String category, DataSupplier<T> supplier) {
        T data = supplier.create();
        String dataId = generateDataId(category, data);

        // 确定当前 scope 并注册
        trackData(category, dataId, resolveCurrentScope());

        LogUtils.debug(getClass(), "已创建数据 [{}]: {}", category, dataId);
        return dataId;
    }

    /**
     * 创建数据并指定 scope 跟踪。
     */
    public <T> String createData(String category, DataSupplier<T> supplier, DataScope scope) {
        T data = supplier.create();
        String dataId = generateDataId(category, data);
        trackData(category, dataId, scope);
        return dataId;
    }

    // ========== 清理器注册 ==========

    /**
     * 注册数据清理处理器。
     *
     * @param category 数据类别
     * @param handler  清理逻辑
     */
    public void registerHandler(String category, DataCleanupHandler handler) {
        cleanupHandlers.computeIfAbsent(category, k -> new ArrayList<>())
                .add(handler);
        LogUtils.debug(getClass(), "已注册清理处理器: {}", category);
    }

    // ========== 生命周期钩子 ==========

    public void addPreSuiteHook(Runnable hook) {
        preSuiteHooks.add(hook);
    }

    public void addPostSuiteHook(Runnable hook) {
        postSuiteHooks.add(hook);
    }

    public void runPreSuiteHooks() {
        preSuiteHooks.forEach(Runnable::run);
    }

    public void runPostSuiteHooks() {
        postSuiteHooks.forEach(Runnable::run);
    }

    // ========== 清理 ==========

    /**
     * 按 scope 清理数据。
     */
    public void cleanup(DataScope scope) {
        String currentOwner = getCurrentOwner(scope);
        if (currentOwner == null) return;

        Map<String, Set<String>> targetMap = getScopeMap(scope);
        if (targetMap == null) return;

        Set<String> dataIds = targetMap.remove(currentOwner);
        if (dataIds == null || dataIds.isEmpty()) return;

        for (String dataId : dataIds) {
            cleanupData(dataId);
        }

        LogUtils.info(getClass(), "已清理 [{}] 数据: {} 条", scope, dataIds.size());
    }

    /**
     * 清理所有数据（AfterSuite 调用）。
     */
    public void cleanupAll() {
        cleanup(DataScope.SUITE);
        cleanup(DataScope.CLASS);
        cleanup(DataScope.METHOD);
        LogUtils.info(getClass(), "所有测试数据已清理完毕");
    }

    // ========== 内部方法 ==========

    private void trackData(String category, String dataId, DataScope scope) {
        String owner = getCurrentOwner(scope);
        if (owner == null) return;

        Map<String, Set<String>> targetMap = getScopeMap(scope);
        if (targetMap == null) return;

        targetMap.computeIfAbsent(owner, k -> ConcurrentHashMap.newKeySet())
                .add(dataId);
    }

    private void cleanupData(String dataId) {
        for (Map.Entry<String, List<DataCleanupHandler>> entry : cleanupHandlers.entrySet()) {
            for (DataCleanupHandler handler : entry.getValue()) {
                try {
                    handler.cleanup(dataId);
                } catch (Exception e) {
                    LogUtils.warn(getClass(), "清理数据失败 [{}]: {} - {}",
                            entry.getKey(), dataId, e.getMessage());
                }
            }
        }
    }

    private String generateDataId(String category, Object data) {
        return category + "_" + System.currentTimeMillis() + "_" + (data.hashCode() & 0xFFFF);
    }

    private DataScope resolveCurrentScope() {
        // 默认推断：从调用栈判断（简化实现）
        return DataScope.METHOD;
    }

    private String getCurrentOwner(DataScope scope) {
        return switch (scope) {
            case SUITE  -> "default_suite";
            case CLASS  -> Thread.currentThread().getName();
            case METHOD -> Thread.currentThread().getName() + "_" + System.nanoTime();
        };
    }

    private Map<String, Set<String>> getScopeMap(DataScope scope) {
        return switch (scope) {
            case SUITE  -> suiteData;
            case CLASS  -> classData;
            case METHOD -> methodData;
        };
    }

    // ========== 枚举 & 接口 ==========

    public enum DataScope {
        SUITE,
        CLASS,
        METHOD
    }

    @FunctionalInterface
    public interface DataSupplier<T> {
        T create();
    }

    @FunctionalInterface
    public interface DataCleanupHandler {
        void cleanup(String dataId);
    }

    // ========== 重置 ==========

    public void reset() {
        cleanupAll();
        cleanupHandlers.clear();
        preSuiteHooks.clear();
        postSuiteHooks.clear();
        LogUtils.info(getClass(), "DataLifecycleManager 已重置");
    }
}
