class VideoTranscriber {
    constructor() {
        this.currentTaskId = null;
        this.eventSource = null;
        // 使用相对路径，假设前端和后端部署在同一来源
        // 如果部署在不同来源，需要替换为后端 API 的实际地址
        this.apiBase = '/api'; // 改为相对路径
        this.currentLanguage = 'en'; // 默认英文

        // 智能进度模拟相关
        this.smartProgress = {
            enabled: false,
            current: 0,           // 当前显示的进度
            target: 0,            // 目标进度
            lastServerUpdate: 0,  // 最后一次服务器更新的进度
            interval: null,       // 定时器
            estimatedDuration: 0, // 预估总时长（秒）
            startTime: null,      // 任务开始时间
            stage: 'preparing'    // 当前阶段
        };

        this.translations = {
            en: {
                title: "Video Transcriber",
                subtitle: "Supports automatic transcription and AI summary for YouTube, Tiktok, Bilibili and other platforms",
                video_url: "Video URL",
                video_url_placeholder: "Enter YouTube, Tiktok, Bilibili or other platform video URLs...",
                summary_language: "Summary Language",
                start_transcription: "Start",
                processing_progress: "Processing Progress",
                preparing: "Preparing...",
                transcription_results: "Results",
                download_transcript: "Download Transcript",
                download_translation: "Download Translation",
                download_summary: "Download Summary",
                transcript_text: "Transcript Text",
                translation: "Translation",
                intelligent_summary: "AI Summary",
                footer_text: "Powered by AI, supports multi-platform video transcription",
                processing: "Processing...",
                downloading_video: "Downloading video...",
                parsing_video: "Parsing video info...",
                transcribing_audio: "Transcribing audio...",
                optimizing_transcript: "Optimizing transcript...",
                generating_summary: "Generating summary...",
                completed: "Processing completed!",
                error_invalid_url: "Please enter a valid video URL",
                error_processing_failed: "Processing failed: ",
                error_task_not_found: "Task not found",
                error_task_not_completed: "Task not completed yet",
                error_invalid_file_type: "Invalid file type",
                error_file_not_found: "File not found",
                error_download_failed: "Download failed: ",
                error_no_file_to_download: "No file available for download"
            },
            zh: {
                title: "视频转录器",
                subtitle: "支持YouTube、Tiktok、Bilibili等平台的视频自动转录和智能摘要",
                video_url: "视频链接",
                video_url_placeholder: "请输入YouTube、Tiktok、Bilibili等平台的视频链接...",
                summary_language: "摘要语言",
                start_transcription: "开始转录",
                processing_progress: "处理进度",
                preparing: "准备中...",
                transcription_results: "转录结果",
                download_transcript: "下载转录",
                download_translation: "下载翻译",
                download_summary: "下载摘要",
                transcript_text: "转录文本",
                translation: "翻译",
                intelligent_summary: "智能摘要",
                footer_text: "由AI驱动，支持多平台视频转录",
                processing: "处理中...",
                downloading_video: "正在下载视频...",
                parsing_video: "正在解析视频信息...",
                transcribing_audio: "正在转录音频...",
                optimizing_transcript: "正在优化转录文本...",
                generating_summary: "正在生成摘要...",
                completed: "处理完成！",
                error_invalid_url: "请输入有效的视频链接",
                error_processing_failed: "处理失败: ",
                error_task_not_found: "任务不存在",
                error_task_not_completed: "任务尚未完成",
                error_invalid_file_type: "无效的文件类型",
                error_file_not_found: "文件不存在",
                error_download_failed: "下载文件失败: ",
                error_no_file_to_download: "没有可下载的文件"
            }
        };

        this.initializeElements();
        this.bindEvents();
        this.initializeLanguage();
    }

    initializeElements() {
        // 表单元素
        this.form = document.getElementById('videoForm');
        this.videoUrlInput = document.getElementById('videoUrl');
        this.summaryLanguageSelect = document.getElementById('summaryLanguage');
        this.submitBtn = document.getElementById('submitBtn');

        // 进度元素
        this.progressSection = document.getElementById('progressSection');
        this.progressStatus = document.getElementById('progressStatus');
        this.progressFill = document.getElementById('progressFill');
        this.progressMessage = document.getElementById('progressMessage');

        // 错误提示
        this.errorAlert = document.getElementById('errorAlert');
        this.errorMessage = document.getElementById('errorMessage');

        // 结果元素
        this.resultsSection = document.getElementById('resultsSection');
        this.scriptContent = document.getElementById('scriptContent');
        this.translationContent = document.getElementById('translationContent');
        this.summaryContent = document.getElementById('summaryContent');
        this.downloadScriptBtn = document.getElementById('downloadScript');
        this.downloadTranslationBtn = document.getElementById('downloadTranslation');
        this.downloadSummaryBtn = document.getElementById('downloadSummary');
        this.translationTabBtn = document.getElementById('translationTabBtn');
        this.translationTab = document.getElementById('translationTab'); // 获取翻译内容 Tab

        // 调试：检查元素是否正确初始化
        console.log('[DEBUG] 🔧 初始化检查:', {
            translationTabBtn: !!this.translationTabBtn,
            translationTab: !!this.translationTab,
            elementIdBtn: this.translationTabBtn ? this.translationTabBtn.id : 'N/A',
            elementIdTab: this.translationTab ? this.translationTab.id : 'N/A'
        });

        // 标签页
        this.tabButtons = document.querySelectorAll('.tab-button');
        this.tabContents = document.querySelectorAll('.tab-content');

        // 语言切换按钮
        this.langToggle = document.getElementById('langToggle');
        this.langText = document.getElementById('langText');
    }

    bindEvents() {
        // 表单提交
        this.form.addEventListener('submit', (e) => {
            e.preventDefault();
            this.startTranscription();
        });

        // 标签页切换
        this.tabButtons.forEach(button => {
            button.addEventListener('click', () => {
                this.switchTab(button.dataset.tab);
            });
        });

        // 下载按钮
        if (this.downloadScriptBtn) {
            this.downloadScriptBtn.addEventListener('click', () => {
                this.downloadFile('script');
            });
        }

        if (this.downloadTranslationBtn) {
            this.downloadTranslationBtn.addEventListener('click', () => {
                this.downloadFile('translation');
            });
        }

        if (this.downloadSummaryBtn) {
            this.downloadSummaryBtn.addEventListener('click', () => {
                this.downloadFile('summary');
            });
        }

        // 语言切换按钮
        this.langToggle.addEventListener('click', () => {
            this.toggleLanguage();
        });
    }

    initializeLanguage() {
        // 可以根据浏览器语言或本地存储设置初始语言
        // 这里简单设置默认语言为英文
        this.switchLanguage('en');
    }

    toggleLanguage() {
        // 切换语言
        this.currentLanguage = this.currentLanguage === 'en' ? 'zh' : 'en';
        this.switchLanguage(this.currentLanguage);
    }

    switchLanguage(lang) {
        this.currentLanguage = lang;

        // 更新语言按钮文本 - 显示当前语言
        this.langText.textContent = lang === 'en' ? 'English' : '中文';

        // 更新页面文本
        this.updatePageText();

        // 更新HTML lang属性
        document.documentElement.lang = lang === 'zh' ? 'zh-CN' : 'en';

        // 更新页面标题
        document.title = this.t('title');
    }

    t(key) {
        // 确保使用当前语言的翻译，如果 key 不存在则返回 key 本身
        return (this.translations[this.currentLanguage] && this.translations[this.currentLanguage][key]) || key;
    }


    updatePageText() {
        // 更新所有带有data-i18n属性的元素
        document.querySelectorAll('[data-i18n]').forEach(element => {
            const key = element.getAttribute('data-i18n');
            element.textContent = this.t(key);
        });

        // 更新placeholder
        document.querySelectorAll('[data-i18n-placeholder]').forEach(element => {
            const key = element.getAttribute('data-i18n-placeholder');
            element.placeholder = this.t(key);
        });
    }

    async startTranscription() {
        // 立即禁用按钮，防止重复点击
        if (this.submitBtn.disabled) {
            return; // 如果按钮已禁用，直接返回
        }

        const videoUrl = this.videoUrlInput.value.trim();
        const summaryLanguage = this.summaryLanguageSelect.value;

        if (!videoUrl) {
            this.showError(this.t('error_invalid_url'));
            return;
        }

        try {
            // 立即禁用按钮和隐藏错误
            this.setLoading(true);
            this.hideError();
            this.hideResults();
            this.showProgress();

            // 发送转录请求 (使用 FormData)
            const formData = new FormData();
            formData.append('url', videoUrl);
            formData.append('summaryLanguage', summaryLanguage); // 后端需要的是 summaryLanguage

            const response = await fetch(`${this.apiBase}/process-video`, {
                method: 'POST',
                body: formData // 发送 FormData
            });

            if (!response.ok) {
                let errorMsg = '请求失败';
                try {
                    const errorData = await response.json();
                    // 尝试解析 Spring Boot 的 Problem Details 或标准错误消息
                    errorMsg = errorData.detail || errorData.message || JSON.stringify(errorData);
                } catch(e) {
                    errorMsg = `HTTP ${response.status}: ${response.statusText}`;
                }
                throw new Error(errorMsg);
            }

            const data = await response.json();
            this.currentTaskId = data.task_id;

            console.log('[DEBUG] ✅ 任务已创建，Task ID:', this.currentTaskId);

            // 启动智能进度模拟
            this.initializeSmartProgress();
            this.updateProgress(5, this.t('preparing'), true);

            // 使用SSE实时接收状态更新
            this.startSSE();

        } catch (error) {
            console.error('启动转录失败:', error);
            this.showError(this.t('error_processing_failed') + error.message);
            this.setLoading(false); // 确保在出错时解除加载状态
            this.hideProgress();
        }
    }

    startSSE() {
        if (!this.currentTaskId) return;

        console.log('[DEBUG] 🔄 启动SSE连接，Task ID:', this.currentTaskId);

        // 创建EventSource连接
        this.eventSource = new EventSource(`${this.apiBase}/task-stream/${this.currentTaskId}`);

        this.eventSource.onmessage = (event) => {
            try {
                const task = JSON.parse(event.data);

                // 忽略心跳消息
                if (task.type === 'heartbeat') {
                    console.log('[DEBUG] 💓 收到心跳');
                    return;
                }

                console.log('[DEBUG] 📊 收到SSE任务状态:', {
                    status: task.status,
                    progress: task.progress,
                    message: task.message
                });

                // 更新进度 (标记为服务器推送)
                console.log('[DEBUG] 📈 更新进度条:', `${task.progress}% - ${task.message}`);
                this.updateProgress(task.progress, task.message, true);

                if (task.status === 'COMPLETED') { // Java 枚举通常是大写
                    console.log('[DEBUG] ✅ 任务完成，显示结果');
                    this.stopSmartProgress(); // 停止智能进度模拟
                    this.stopSSE();
                    this.setLoading(false);
                    this.hideProgress();
                    // 从 task 对象中获取所需的所有数据
                    this.showResults(
                        task.scriptContent, // 假设 TaskInfo 中有 scriptContent 字段
                        task.summaryContent, // 假设 TaskInfo 中有 summaryContent 字段
                        task.videoTitle,
                        task.translationContent, // 假设 TaskInfo 中有 translationContent 字段
                        task.detectedLanguage,
                        task.summaryLanguage
                    );
                } else if (task.status === 'ERROR') { // Java 枚举通常是大写
                    console.log('[DEBUG] ❌ 任务失败:', task.error);
                    this.stopSmartProgress(); // 停止智能进度模拟
                    this.stopSSE();
                    this.setLoading(false);
                    this.hideProgress();
                    this.showError(task.error || '处理过程中发生错误');
                }
            } catch (error) {
                console.error('[DEBUG] 解析SSE数据失败:', error, '原始数据:', event.data);
            }
        };

        this.eventSource.onerror = async (error) => {
            console.error('[DEBUG] SSE连接错误:', error);
            this.stopSSE();

            // 兜底：查询任务最终状态，若已完成则直接渲染结果
            try {
                if (this.currentTaskId) {
                    const resp = await fetch(`${this.apiBase}/task-status/${this.currentTaskId}`);
                    if (resp.ok) {
                        const task = await resp.json();
                        if (task && task.status === 'COMPLETED') {
                            console.log('[DEBUG] 🔁 SSE断开，但任务已完成，直接渲染结果');
                            this.stopSmartProgress();
                            this.setLoading(false);
                            this.hideProgress();
                            // 从 task 对象中获取所需的所有数据
                            this.showResults(
                                task.scriptContent,
                                task.summaryContent,
                                task.videoTitle,
                                task.translationContent,
                                task.detectedLanguage,
                                task.summaryLanguage
                            );
                            return;
                        } else if (task && task.status === 'ERROR') {
                            console.log('[DEBUG] 🔁 SSE断开，任务已失败');
                            this.stopSmartProgress();
                            this.setLoading(false);
                            this.hideProgress();
                            this.showError(task.error || '任务处理失败');
                            return;
                        }
                    }
                }
            } catch (e) {
                console.error('[DEBUG] 兜底查询任务状态失败:', e);
            }

            // 未完成则提示并保持页面状态
            this.showError(this.t('error_processing_failed') + '与服务器的实时连接断开');
            // 保持加载状态或允许用户重试
            // this.setLoading(false); // 可以考虑不解除加载状态，提示用户刷新或等待
        };

        this.eventSource.onopen = () => {
            console.log('[DEBUG] 🔗 SSE连接已建立');
        };
    }

    stopSSE() {
        if (this.eventSource) {
            console.log('[DEBUG] 🔌 关闭SSE连接');
            this.eventSource.close();
            this.eventSource = null;
        }
    }



    updateProgress(progress, message, fromServer = false) {
        console.log('[DEBUG] 🎯 updateProgress调用:', { progress, message, fromServer });

        if (fromServer) {
            // 服务器推送的真实进度
            this.handleServerProgress(progress, message);
        } else {
            // 本地模拟进度
            this.updateProgressDisplay(progress, message);
        }
    }

    handleServerProgress(serverProgress, message) {
        console.log('[DEBUG] 📡 处理服务器进度:', serverProgress);

        // 停止当前的模拟进度
        this.stopSmartProgress();

        // 更新服务器进度记录
        this.smartProgress.lastServerUpdate = serverProgress;
        this.smartProgress.current = serverProgress;

        // 立即显示服务器进度
        this.updateProgressDisplay(serverProgress, message);

        // 确定当前处理阶段和预估目标
        this.updateProgressStage(serverProgress, message);

        // 只有当任务未完成时才重新启动模拟
        if (serverProgress < 100) {
            this.startSmartProgress();
        }
    }


    updateProgressStage(progress, message) {
        // 根据进度和消息确定处理阶段
        if (message.includes('解析') || message.includes('Parsing')) {
            this.smartProgress.stage = 'parsing';
            this.smartProgress.target = 60; // 提升目标以覆盖下载
        } else if (message.includes('下载') || message.includes('Downloading')) {
            this.smartProgress.stage = 'downloading';
            this.smartProgress.target = 60; // 保持目标
        } else if (message.includes('转录') || message.includes('Transcribing')) {
            this.smartProgress.stage = 'transcribing';
            this.smartProgress.target = 80;
        } else if (message.includes('优化') || message.includes('Optimizing')) {
            this.smartProgress.stage = 'optimizing';
            this.smartProgress.target = 90;
        } else if (message.includes('翻译') || message.includes('Translating')) {
            this.smartProgress.stage = 'translating'; // 添加翻译阶段
            this.smartProgress.target = 75; // 假设翻译在优化前
        } else if (message.includes('摘要') || message.includes('Summarizing')) {
            this.smartProgress.stage = 'summarizing';
            this.smartProgress.target = 95;
        } else if (message.includes('完成') || message.includes('Completed')) {
            this.smartProgress.stage = 'completed';
            this.smartProgress.target = 100;
        }

        // 如果当前进度超过预设目标，调整目标
        if (progress >= this.smartProgress.target) {
            this.smartProgress.target = Math.min(progress + 10, 100);
        }

        console.log('[DEBUG] 🎯 阶段更新:', {
            stage: this.smartProgress.stage,
            target: this.smartProgress.target,
            current: progress
        });
    }

    initializeSmartProgress() {
        // 初始化智能进度状态
        this.smartProgress.enabled = false;
        this.smartProgress.current = 0;
        this.smartProgress.target = 15; // 初始目标
        this.smartProgress.lastServerUpdate = 0;
        this.smartProgress.startTime = null; // 在启动时设置
        this.smartProgress.stage = 'preparing';

        console.log('[DEBUG] 🔧 智能进度模拟已初始化');
    }

    startSmartProgress() {
        // 启动智能进度模拟
        if (this.smartProgress.interval) {
            clearInterval(this.smartProgress.interval);
        }

        this.smartProgress.enabled = true;
        // 如果 startTime 未设置或者距离上次更新太久，重置 startTime
        if (!this.smartProgress.startTime || (Date.now() - this.smartProgress.startTime > 60000)) {
            this.smartProgress.startTime = Date.now();
        }


        // 每500ms更新一次模拟进度
        this.smartProgress.interval = setInterval(() => {
            this.simulateProgress();
        }, 500);

        console.log('[DEBUG] 🚀 智能进度模拟已启动');
    }

    stopSmartProgress() {
        if (this.smartProgress.interval) {
            clearInterval(this.smartProgress.interval);
            this.smartProgress.interval = null;
        }
        this.smartProgress.enabled = false;
        console.log('[DEBUG] ⏹️ 智能进度模拟已停止');
    }

    simulateProgress() {
        if (!this.smartProgress.enabled) return;

        const current = this.smartProgress.current;
        const target = this.smartProgress.target;

        // 如果已经达到或超过目标，暂停模拟
        // 保留一点余地，防止完全停止在 99.x
        if (current >= target - 0.1) {
            console.log('[DEBUG] ⏳ 接近目标，暂停模拟');
            return;
        }


        // 计算进度增量（基于阶段的不同速度）
        let increment = this.calculateProgressIncrement();

        // 确保不超过目标进度
        const newProgress = Math.min(current + increment, target);

        if (newProgress > current) {
            this.smartProgress.current = newProgress;
            // 只更新显示，不触发 handleServerProgress
            this.updateProgressDisplay(newProgress, this.getCurrentStageMessage());
        }
    }


    calculateProgressIncrement() {
        const elapsedTime = (Date.now() - (this.smartProgress.startTime || Date.now())) / 1000; // 秒

        // 基于不同阶段的预估速度 (每秒增加的百分比)
        const stageConfig = {
            'parsing': { speedPerSec: 1.0, maxTime: 30 },      // 解析阶段: 30秒内从 ~10% 到 60%
            'downloading': { speedPerSec: 0.5, maxTime: 120 }, // 下载阶段: 2分钟内保持在 60% 附近
            'transcribing': { speedPerSec: 0.2, maxTime: 180 }, // 转录阶段: 3分钟内从 ~60% 到 80%
            'optimizing': { speedPerSec: 0.3, maxTime: 60 },  // 优化阶段: 1分钟内从 ~80% 到 90%
            'translating': { speedPerSec: 0.4, maxTime: 45 }, // 翻译阶段: 45秒
            'summarizing': { speedPerSec: 0.5, maxTime: 30 },   // 摘要阶段: 30秒内从 ~90% 到 95%
            'preparing': { speedPerSec: 2.0, maxTime: 10 }    // 准备阶段: 快速到 15%
        };


        const config = stageConfig[this.smartProgress.stage] || { speedPerSec: 0.5, maxTime: 60 };

        // 每 500ms (0.5秒) 的基础增量
        let baseIncrement = config.speedPerSec * 0.5;

        // 时间惩罚/奖励：如果超过最大时间，加快；如果在预期内，可能减慢
        const timeFactor = elapsedTime > config.maxTime ? 1.5 : (elapsedTime < config.maxTime / 2 ? 0.8 : 1.0);
        baseIncrement *= timeFactor;


        // 距离因子：距离目标越近，速度越慢 (使用指数衰减)
        const remaining = Math.max(0.1, this.smartProgress.target - this.smartProgress.current);
        const distanceFactor = Math.min(1.0, Math.pow(remaining / 20, 0.5)); // 在剩余 20% 时开始显著减速
        baseIncrement *= distanceFactor;


        // 最小增量，防止完全停止
        baseIncrement = Math.max(0.05, baseIncrement);


        console.log('[DEBUG] 🔢 计算增量:', { stage: this.smartProgress.stage, base: baseIncrement, timeFactor, distanceFactor, remaining, elapsed: elapsedTime });

        return baseIncrement;
    }


    getCurrentStageMessage() {
        // 使用 this.t 获取翻译后的消息
        const stageMessages = {
            'parsing': this.t('parsing_video'),
            'downloading': this.t('downloading_video'),
            'transcribing': this.t('transcribing_audio'),
            'optimizing': this.t('optimizing_transcript'),
            'translating': '正在翻译文本...', // 需要添加到 translations
            'summarizing': this.t('generating_summary'),
            'completed': this.t('completed'),
            'preparing': this.t('preparing')
        };

        return stageMessages[this.smartProgress.stage] || this.t('processing');
    }

    updateProgressDisplay(progress, message) {
        // 实际更新UI显示
        const roundedProgress = Math.min(100, Math.max(0, Math.round(progress * 10) / 10)); // 限制在 0-100 之间
        this.progressStatus.textContent = `${roundedProgress}%`;
        this.progressFill.style.width = `${roundedProgress}%`;
        // console.log('[DEBUG] 📏 进度条已更新:', this.progressFill.style.width);

        // 简单的消息映射翻译
        let translatedMessage = message;
        const messageMap = {
            '正在解析视频信息...': this.t('parsing_video'),
            '正在下载视频并转换为音频...': this.t('downloading_video'),
            '音频处理完成，开始转录...': this.t('transcribing_audio'),
            '转录完成，正在优化文本...': this.t('optimizing_transcript'),
            '正在翻译文本...': '正在翻译文本...', // Add to translations
            '正在生成摘要...': this.t('generating_summary'),
            '处理完成！': this.t('completed'),
            '任务创建，准备处理...': this.t('preparing')
            // 可以添加更多后端可能发送的消息
        };
        translatedMessage = messageMap[message] || message; // 如果没有匹配，显示原文


        this.progressMessage.textContent = translatedMessage;
    }

    showProgress() {
        this.progressSection.style.display = 'block';
    }

    hideProgress() {
        this.progressSection.style.display = 'none';
    }

    showResults(scriptContent, summaryContent, videoTitle = null, translationContent = null, detectedLanguage = null, summaryLanguage = null) {

        console.log('[DEBUG] 🔍 showResults 参数:', {
            scriptContent: scriptContent ? scriptContent.substring(0, 50) + '...' : 'null',
            summaryContent: summaryContent ? summaryContent.substring(0, 50) + '...' : 'null',
            videoTitle,
            translationContent: translationContent ? translationContent.substring(0, 50) + '...' : 'null',
            detectedLanguage,
            summaryLanguage
        });


        // 渲染 markdown 内容，确保参数不为 null 或 undefined
        const safeScript = scriptContent || '';
        const safeSummary = summaryContent || '';
        const safeTranslation = translationContent || '';

        // 使用 marked 解析 Markdown
        try {
            this.scriptContent.innerHTML = safeScript ? marked.parse(safeScript) : '';
            this.summaryContent.innerHTML = safeSummary ? marked.parse(safeSummary) : '';
        } catch (e) {
            console.error("Markdown 解析失败:", e);
            this.scriptContent.textContent = safeScript; // 解析失败则显示纯文本
            this.summaryContent.textContent = safeSummary;
        }


        // 处理翻译标签页的显示逻辑
        const shouldShowTranslation = safeTranslation && detectedLanguage && summaryLanguage && detectedLanguage !== summaryLanguage;

        console.log('[DEBUG] 🌐 翻译显示判断:', {
            shouldShowTranslation,
            translationTabBtn: !!this.translationTabBtn,
            translationTab: !!this.translationTab
        });

        if (this.translationTabBtn && this.translationTab) {
            if (shouldShowTranslation) {
                console.log('[DEBUG] ✅ 显示翻译标签页');
                this.translationTabBtn.style.display = 'inline-block'; // 或者 'flex'
                this.translationTab.style.display = 'none'; // 初始隐藏内容，由 switchTab 控制
                try {
                    this.translationContent.innerHTML = marked.parse(safeTranslation);
                } catch (e) {
                    console.error("翻译 Markdown 解析失败:", e);
                    this.translationContent.textContent = safeTranslation;
                }

                // 显示下载翻译按钮
                if (this.downloadTranslationBtn) {
                    this.downloadTranslationBtn.style.display = 'inline-flex';
                }

            } else {
                console.log('[DEBUG] ❌ 隐藏翻译标签页');
                this.translationTabBtn.style.display = 'none';
                this.translationTab.style.display = 'none';
                this.translationContent.innerHTML = ''; // 清空内容

                // 隐藏下载翻译按钮
                if (this.downloadTranslationBtn) {
                    this.downloadTranslationBtn.style.display = 'none';
                }
                // 如果翻译标签当前是激活的，切换回脚本标签
                if (this.translationTabBtn.classList.contains('active')) {
                    this.switchTab('script');
                }
            }
        } else {
            console.warn("[DEBUG] ⚠️ 翻译按钮或内容区域未找到!");
        }

        // 显示结果区域
        this.resultsSection.style.display = 'block';

        // 默认激活第一个标签页 (脚本)
        this.switchTab('script');

        // 滚动到结果区域
        this.resultsSection.scrollIntoView({ behavior: 'smooth' });

        // 高亮代码 (如果需要)
        if (window.Prism) {
            try {
                Prism.highlightAllUnder(this.resultsSection);
            } catch (e) {
                console.warn("Prism 高亮失败:", e);
            }
        }
    }


    hideResults() {
        this.resultsSection.style.display = 'none';
        // 隐藏翻译按钮和下载按钮
        if (this.translationTabBtn) this.translationTabBtn.style.display = 'none';
        if (this.downloadTranslationBtn) this.downloadTranslationBtn.style.display = 'none';
    }


    switchTab(tabName) {
        // 移除所有活动状态
        this.tabButtons.forEach(btn => btn.classList.remove('active'));
        this.tabContents.forEach(content => content.classList.remove('active'));

        // 激活选中的标签页按钮
        // data-tab 属性值可能包含特殊字符，使用 CSS.escape 或手动检查
        // const activeButton = document.querySelector(`.tab-button[data-tab="${CSS.escape(tabName)}"]`);
        let activeButton = null;
        this.tabButtons.forEach(btn => {
            if (btn.dataset.tab === tabName) {
                activeButton = btn;
            }
        });


        // 激活选中的标签页内容
        const activeContent = document.getElementById(`${tabName}Tab`);

        if (activeButton && activeContent) {
            // 只激活可见的按钮
            if (activeButton.style.display !== 'none') {
                activeButton.classList.add('active');
                activeContent.classList.add('active'); // 显示对应内容
                activeContent.style.display = 'block'; // 确保内容可见
            } else {
                // 如果目标按钮不可见（例如翻译按钮被隐藏），则默认激活第一个可见按钮
                const firstVisibleButton = document.querySelector('.tab-button:not([style*="display: none"])');
                if(firstVisibleButton) {
                    this.switchTab(firstVisibleButton.dataset.tab);
                }
            }

        } else if (activeButton && activeButton.style.display === 'none') {
            // 如果目标按钮不可见，则默认激活第一个可见按钮
            const firstVisibleButton = document.querySelector('.tab-button:not([style*="display: none"])');
            if(firstVisibleButton) {
                this.switchTab(firstVisibleButton.dataset.tab);
            }
        } else {
            console.warn(`Tab button or content not found for tabName: ${tabName}`);
            // 回退到第一个可见标签
            const firstVisibleButton = document.querySelector('.tab-button:not([style*="display: none"])');
            if(firstVisibleButton) {
                this.switchTab(firstVisibleButton.dataset.tab);
            }
        }
    }


    async downloadFile(fileType) {
        if (!this.currentTaskId) {
            this.showError(this.t('error_no_file_to_download'));
            return;
        }

        try {
            // 首先获取任务状态，获得实际文件名
            const taskResponse = await fetch(`${this.apiBase}/task-status/${this.currentTaskId}`);
            if (!taskResponse.ok) {
                const errorData = await taskResponse.json();
                throw new Error(errorData.detail || errorData.message || '获取任务状态失败');
            }

            const taskData = await taskResponse.json();
            let filePath; // 后端返回的是文件路径字符串

            // 根据文件类型获取对应的文件路径
            switch(fileType) {
                case 'script':
                    filePath = taskData.scriptPath;
                    break;
                case 'summary':
                    filePath = taskData.summaryPath;
                    break;
                case 'translation':
                    filePath = taskData.translationPath;
                    break;
                // 可以添加 raw script 下载
                // case 'raw_script':
                //     filePath = taskData.rawScriptPath;
                //     break;
                default:
                    throw new Error('未知的文件类型');
            }

            if (!filePath) {
                console.error(`Task data missing path for file type: ${fileType}`, taskData);
                throw new Error(`找不到 ${fileType} 文件路径`);
            }

            // 从完整路径中提取文件名
            const filename = filePath.split(/[\\/]/).pop(); // 兼容 Windows 和 Linux 路径

            if (!filename) {
                throw new Error('无法从路径中提取文件名');
            }

            // 使用相对路径进行下载请求
            const encodedFilename = encodeURIComponent(filename);
            const downloadUrl = `${this.apiBase}/download/${encodedFilename}`;

            console.log(`Attempting to download: ${downloadUrl}`);

            // 创建一个隐藏的 a 标签来触发下载
            const link = document.createElement('a');
            link.href = downloadUrl;
            link.download = filename; // 浏览器将使用这个名字保存文件
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);

        } catch (error) {
            console.error('下载文件失败:', error);
            this.showError(this.t('error_download_failed') + error.message);
        }
    }

    setLoading(loading) {
        this.submitBtn.disabled = loading;

        if (loading) {
            this.submitBtn.innerHTML = `<div class="loading-spinner"></div> ${this.t('processing')}`;
        } else {
            // 恢复按钮的原始图标和文本
            this.submitBtn.innerHTML = `<i class="fas fa-play"></i> <span data-i18n="start_transcription">${this.t('start_transcription')}</span>`;
            // 需要重新查找 span 并设置文本，因为 innerHTML 替换了它
            const span = this.submitBtn.querySelector('span[data-i18n="start_transcription"]');
            if (span) span.textContent = this.t('start_transcription');

        }
    }


    showError(message) {
        // 尝试提取核心错误信息，避免显示过多技术细节
        let displayMessage = message;
        if (message.includes("yt-dlp failed")) {
            const ytDlpErrorMatch = message.match(/ERROR: (.*)/);
            if (ytDlpErrorMatch && ytDlpErrorMatch[1]) {
                displayMessage = `视频下载/解析失败: ${ytDlpErrorMatch[1].trim()}`;
            } else {
                displayMessage = "视频下载或解析时发生错误，请检查 URL 或稍后重试。";
            }
        } else if (message.includes("Transcription failed")) {
            displayMessage = "音频转录失败，请稍后重试。";
        } else if (message.includes("summary failed") || message.includes("translation failed")) {
            displayMessage = "AI 处理（摘要/翻译）失败，请检查 API Key 或稍后重试。";
        } else if (message.includes("Task not found")) {
            displayMessage = this.t('error_task_not_found');
        } else if (message.includes("HTTP") || message.includes("Failed to fetch")) {
            displayMessage = "网络错误或无法连接到服务器。";
        }
        // 可以添加更多特定错误的简化提示

        this.errorMessage.textContent = displayMessage;
        this.errorAlert.style.display = 'block';

        // 滚动到错误提示
        this.errorAlert.scrollIntoView({ behavior: 'smooth', block: 'center' });


        // 考虑增加错误显示时间
        clearTimeout(this.errorTimeout); // 清除之前的隐藏定时器
        this.errorTimeout = setTimeout(() => {
            this.hideError();
        }, 8000); // 8秒后自动隐藏
    }

    hideError() {
        this.errorAlert.style.display = 'none';
        clearTimeout(this.errorTimeout);
    }
}

// 页面加载完成后初始化应用
document.addEventListener('DOMContentLoaded', () => {
    // 确保 marked.js 加载完成
    if (typeof marked === 'undefined') {
        console.error("marked.js 未加载！");
        return;
    }
    // 配置 marked.js (可选)
    // marked.setOptions({ ... });

    window.transcriberApp = new VideoTranscriber();

    // 添加一些示例链接提示
    const urlInput = document.getElementById('videoUrl');
    const placeholderDefault = window.transcriberApp.t('video_url_placeholder'); // 获取当前语言的默认提示
    const placeholderExample = '例如: https://www.youtube.com/watch?v=... 或 https://www.bilibili.com/video/...';

    if (urlInput) {
        urlInput.addEventListener('focus', () => {
            if (!urlInput.value) {
                urlInput.placeholder = placeholderExample;
            }
        });

        urlInput.addEventListener('blur', () => {
            if (!urlInput.value) {
                // 在失焦时恢复对应当前语言的默认提示
                urlInput.placeholder = window.transcriberApp.t('video_url_placeholder');
            }
        });
        // 初始化时设置正确的 placeholder
        urlInput.placeholder = placeholderDefault;
    } else {
        console.error("未找到 videoUrl 输入框");
    }

});

// 处理页面刷新时的清理工作
window.addEventListener('beforeunload', () => {
    if (window.transcriberApp && window.transcriberApp.eventSource) {
        window.transcriberApp.stopSSE();
    }
});
