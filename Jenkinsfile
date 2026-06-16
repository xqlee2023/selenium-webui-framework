// =====================================================================
// Jenkins Pipeline — Selenium WebUI Framework v3.2.0
// =====================================================================
// 功能：
//   • 参数化构建（环境/浏览器/并行度/AI开关）
//   • Docker 代理 + Selenium Grid 自动启停
//   • 三浏览器并行矩阵
//   • Allure 报告 + JUnit 趋势
//   • AI 测试结果分析（后置）
//   • Flaky 重跑 + 历史跟踪
//   • 多渠道通知（Slack / 邮件 / 钉钉）
// =====================================================================

pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-17'
            args '-v /var/run/docker.sock:/var/run/docker.sock --network host'
        }
    }

    // ==================== 参数 ====================
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'qa', 'prod'],
            description: '测试环境'
        )
        choice(
            name: 'BROWSER',
            choices: ['chrome', 'firefox', 'edge', 'all'],
            description: '浏览器（all = 三浏览器并行）'
        )
        string(
            name: 'PARALLEL_COUNT',
            defaultValue: '4',
            description: 'TestNG 并行线程数'
        )
        booleanParam(
            name: 'HEADLESS',
            defaultValue: true,
            description: '无头模式'
        )
        booleanParam(
            name: 'AI_ENABLED',
            defaultValue: true,
            description: '启用 AI 诊断/报告'
        )
        booleanParam(
            name: 'FLAG_FLAKY',
            defaultValue: true,
            description: '自动标记 Flaky 失败用例'
        )
        booleanParam(
            name: 'GENERATE_ALLURE',
            defaultValue: true,
            description: '生成 Allure 报告'
        )
        string(
            name: 'TESTNG_SUITE',
            defaultValue: 'src/test/resources/testng.xml',
            description: 'TestNG XML 路径'
        )
        booleanParam(
            name: 'SKIP_A11Y',
            defaultValue: false,
            description: '跳过无障碍检测（加速构建）'
        )
    }

    // ==================== 环境变量 ====================
    environment {
        // 基础
        MAVEN_OPTS = '-Xmx2048m -XX:MaxMetaspaceSize=512m'
        // 从参数注入
        TEST_ENV  = "${params.ENVIRONMENT}"
        BROWSER   = "${params.BROWSER}"
        HEADLESS  = "${params.HEADLESS}"
        // Selenium Grid (docker-compose 启动后可用)
        HUB_URL   = "http://localhost:4444/wd/hub"
        // AI
        AI_ENABLED = "${params.AI_ENABLED}"
        DEEPSEEK_API_KEY = credentials('deepseek-api-key')
        // Allure
        ALLURE_RESULTS = 'allure-results'
        // A11y
        ACCESSIBILITY_ENABLED = "${!params.SKIP_A11Y}"
    }

    // ==================== 阶段 ====================

    stages {

        // ──── 1. 启动 Selenium Grid ────
        stage('🚀 Start Selenium Grid') {
            steps {
                script {
                    sh '''
                        echo "Starting Selenium Grid (docker-compose)..."
                        docker compose -f docker-compose.yml up -d selenium-hub chrome-node firefox-node edge-node

                        # 等待 Hub 就绪
                        echo "Waiting for Selenium Hub..."
                        for i in $(seq 1 30); do
                            STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:4444/wd/hub/status 2>/dev/null || echo "000")
                            if [ "$STATUS" = "200" ]; then
                                echo "✅ Selenium Hub is ready"
                                break
                            fi
                            sleep 2
                        done

                        # 健康检查
                        curl -s http://localhost:4444/wd/hub/status | python3 -m json.tool
                    '''
                }
            }
        }

        // ──── 2. Checkout + 编译 ────
        stage('📦 Checkout & Compile') {
            steps {
                checkout scm
                sh 'mvn clean compile -q'
            }
        }

        // ──── 3. 执行测试（矩阵并行） ────
        stage('🧪 Execute Tests') {
            steps {
                script {
                    // 确定需要跑的浏览器列表
                    def browsers = (params.BROWSER == 'all')
                        ? ['chrome', 'firefox', 'edge']
                        : [params.BROWSER]

                    // 并行矩阵
                    def parallelStages = [:]
                    browsers.each { browser ->
                        String stageName = "test-${params.ENVIRONMENT}-${browser}"
                        parallelStages[stageName] = {
                            node {
                                stage(stageName) {
                                    catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                                        sh """
                                            echo "═══════════════════════════════════════"
                                            echo "🧪 Running: ${params.ENVIRONMENT} / ${browser}"
                                            echo "   Headless: ${params.HEADLESS}"
                                            echo "   Parallel: ${params.PARALLEL_COUNT}"
                                            echo "═══════════════════════════════════════"

                                            mvn test \
                                                -P${params.ENVIRONMENT} \
                                                -Dbrowser=${browser} \
                                                -Dheadless=${params.HEADLESS} \
                                                -Dparallel.count=${params.PARALLEL_COUNT} \
                                                -DhubUrl=${HUB_URL} \
                                                -Dtestng.suite=${params.TESTNG_SUITE} \
                                                -Dallure.results.directory=${ALLURE_RESULTS}/${browser} \
                                                | tee test-output/${browser}.log
                                        """
                                    }

                                    // JUnit 报告收集
                                    junit(
                                        allowEmptyResults: true,
                                        testResults: "target/surefire-reports/TEST-*.xml"
                                    )

                                    // 截图存档
                                    archiveArtifacts(
                                        artifacts: "screenshots/${browser}/**/*.png",
                                        allowEmptyArchive: true
                                    )
                                }
                            }
                        }
                    }
                    parallel parallelStages
                }
            }
        }

        // ──── 4. Flaky 重跑 ────
        stage('🔄 Flaky Test Re-run') {
            when {
                expression { params.FLAG_FLAKY && currentBuild.result == 'UNSTABLE' }
            }
            steps {
                script {
                    echo 'Re-running failed tests to detect flaky...'
                    sh """
                        mvn test \
                            -P${params.ENVIRONMENT} \
                            -Dbrowser=${params.BROWSER == 'all' ? 'chrome' : params.BROWSER} \
                            -Dheadless=${params.HEADLESS} \
                            -Dparallel.count=1 \
                            -Dtestng.suite=${params.TESTNG_SUITE} \
                            -DfailIfNoTests=false \
                            -Dsurefire.rerunFailingTestsCount=1 \
                            | tee test-output/rerun.log
                    """
                }
            }
        }

        // ──── 5. Allure 报告 ────
        stage('📊 Allure Report') {
            when {
                expression { params.GENERATE_ALLURE }
            }
            steps {
                script {
                    // 合并多浏览器结果
                    sh """
                        echo "Generating Allure report..."
                        mvn allure:report -Dallure.results.directory=${ALLURE_RESULTS}
                    """

                    allure includeProperties: false,
                           results: [[path: ALLURE_RESULTS]]
                }
            }
        }

        // ──── 6. AI 测试结果分析 ────
        stage('🤖 AI Analysis') {
            when {
                expression { params.AI_ENABLED }
            }
            steps {
                script {
                    sh '''
                        echo "═══════════════════════════════════════"
                        echo "🤖 AI 测试结果分析"
                        echo "═══════════════════════════════════════"

                        # 统计
                        TOTAL=$(grep -c "Tests run:" test-output/*.log 2>/dev/null || echo "N/A")
                        FAILED=$(grep -c "<<< FAILURE" test-output/*.log 2>/dev/null || echo "0")
                        echo "Total suites: $TOTAL | Failures: $FAILED"

                        # AI 报告已在 Suite 结束时自动生成
                        # 此处可作为 hook 发送摘要到通知渠道
                    '''
                }
            }
        }

        // ──── 7. 归档产物 ────
        stage('📦 Archive') {
            steps {
                script {
                    archiveArtifacts artifacts: 'test-output/**/*.log', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'allure-report/**',    allowEmptyArchive: true
                    archiveArtifacts artifacts: 'screenshots/**/*.png', allowEmptyArchive: true
                    archiveArtifacts artifacts: 'test-history/**/*.jsonl', allowEmptyArchive: true

                    // 标记构建保留
                    if (currentBuild.result == 'SUCCESS') {
                        buildDescription "✅ ${params.ENVIRONMENT}/${params.BROWSER}"
                    }
                }
            }
        }
    }

    // ==================== 后置 ====================
    post {
        always {
            script {
                // 停止 Selenium Grid
                sh '''
                    echo "Stopping Selenium Grid..."
                    docker compose -f docker-compose.yml down
                '''
            }

            // 清理工作空间
            cleanWs(
                cleanWhenNotBuilt: false,
                deleteDirs: true,
                patterns: [
                    [pattern: 'target/', type: 'INCLUDE'],
                    [pattern: 'node_modules/', type: 'INCLUDE']
                ]
            )
        }

        success {
            echo '✅ All tests passed!'
            script {
                def msg = "${env.JOB_NAME} #${env.BUILD_NUMBER}\n" +
                          "环境: ${params.ENVIRONMENT} | 浏览器: ${params.BROWSER}\n" +
                          "状态: ✅ 全部通过\n" +
                          "报告: ${env.BUILD_URL}allure"

                // 多渠道通知
                slackSend(
                    channel: '#test-automation',
                    color: 'good',
                    message: msg
                )
            }
        }

        failure {
            echo '❌ Tests failed!'
            script {
                def msg = "${env.JOB_NAME} #${env.BUILD_NUMBER}\n" +
                          "环境: ${params.ENVIRONMENT} | 浏览器: ${params.BROWSER}\n" +
                          "状态: ❌ 失败\n" +
                          "链接: ${env.BUILD_URL}"

                slackSend(
                    channel: '#test-automation',
                    color: 'danger',
                    message: msg
                )

                // 邮件通知（仅 main 分支）
                if (env.BRANCH_NAME == 'main') {
                    emailext(
                        subject: "❌ [FAIL] ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                        body: msg,
                        to: 'qa-team@example.com'
                    )
                }
            }
        }

        unstable {
            echo '⚠️ Build unstable (flaky tests?)'
            script {
                slackSend(
                    channel: '#test-automation',
                    color: 'warning',
                    message: "⚠️ ${env.JOB_NAME} #${env.BUILD_NUMBER} unstable — 可能存在 Flaky 用例"
                )
            }
        }
    }
}
