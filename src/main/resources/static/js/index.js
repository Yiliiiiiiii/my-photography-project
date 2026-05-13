/* =================================================
   核心逻辑: 负责交互、WebSocket、图表渲染、弹窗管理
   ================================================= */

// 从 window.AppConfig 获取 Thymeleaf 注入的变量
const csrfToken = window.AppConfig.csrfToken;
const csrfHeader = window.AppConfig.csrfHeader;
let globalLikedIds = window.AppConfig.likedPhotos;
let page = window.AppConfig.page;
let isLast = window.AppConfig.isLast;
let query = window.AppConfig.query;
const currUser = window.AppConfig.currUser;

// 全局状态变量
let nextPage = page + 1;
let loading = false;
let observer;
let stompClient;
let myAlbumsList = [];
let currentModalPhotoId = null;
let currentTargetPhotoId = null;

// DOM 元素引用
const modal = document.getElementById('photo-modal');

// 初始化
document.addEventListener('DOMContentLoaded', () => {
    // 1. 深色模式初始化
    initDarkMode();

    // 2. 加载数据
    connectWebSocket();
    loadStats();
    loadMyAlbums();

    // 3. 绑定观察者和事件
    setupObservers();
    setupDelegation();
    setupEditModeToggle();
    //setupBackToTop();
    setupMobileDashboard();
    setupMobileGestures();

    setupSmartNavbar(); // <--- 加这一行
    setupHeroParallax();
    //setupReadingProgress();

    setupCircularProgress();
    setupCustomContextMenu(); // 【新增】启动自定义右键菜单

    setupDarkSpotlight();

    setupLavaLamp();

    setupVelocitySkew();

    
    // 【新增】启动打字机效果
    const bio = document.querySelector('.hero-bio');
    // 这里的 '耶耶耶' 是备用文本，实际上会读取 HTML 里原本写的内容
    if (bio) typeWriterEffect(bio, bio.textContent.trim() || "记录光影与生活");

    // 【新增修复】如果起步就是最后一页，直接显示页脚
    if (isLast) {
        const endMsg = document.getElementById('gallery-end');
        if (endMsg) endMsg.style.display = 'flex';
        // 确保加载器隐藏
        const loader = document.getElementById('loading-spinner');
        if (loader) loader.style.display = 'none';
    }
});

/* ---------------- 功能模块 ---------------- */

function initDarkMode() {
    const themeBtn = document.getElementById('theme-toggle');
    const iconMoon = document.getElementById('icon-moon');
    const iconSun = document.getElementById('icon-sun');
    const currentTheme = localStorage.getItem('theme');

    // 检查本地存储或系统偏好
    if (currentTheme === 'dark' || (!currentTheme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
        document.body.classList.add('dark-mode');
        updateIcon(true);
    }

    if (themeBtn) {
        themeBtn.addEventListener('click', () => {
            const isDark = document.body.classList.toggle('dark-mode');
            localStorage.setItem('theme', isDark ? 'dark' : 'light');
            updateIcon(isDark);

            // 【新增】动态更新手机状态栏颜色
            const metaThemeColor = document.querySelector('meta[name="theme-color"]');
            // 简单起见，我们移除 media 属性并直接设置当前颜色
            // 注意：这里为了兼容性，我们可能需要查找或创建 meta 标签，或者直接操作现有的
            let metaLight = document.querySelector('meta[name="theme-color"][media*="light"]');
            let metaDark = document.querySelector('meta[name="theme-color"][media*="dark"]');
            
            if (metaLight && metaDark) {
                // 如果有分别定义的 meta，浏览器会自动处理系统级切换
                // 但如果是手动按钮切换，我们需要强制修改 content
                metaLight.content = isDark ? '#000000' : '#f5f5f7';
                metaDark.content = isDark ? '#000000' : '#f5f5f7';
            }
        });
    }

    function updateIcon(isDark) {
        if (isDark) {
            iconMoon.style.display = 'none';
            iconSun.style.display = 'block';
        } else {
            iconMoon.style.display = 'block';
            iconSun.style.display = 'none';
        }
    }
}

/* -------------------------------------------------------
   【体验升级】编辑模式切换 (持久化版)
------------------------------------------------------- */
function setupEditModeToggle() {
    const toggle = document.getElementById('toggle-edit-mode');
    const btnText = document.getElementById('edit-button-text');
    
    // 1. 定义切换逻辑
    const setEditMode = (enable) => {
        if (enable) {
            document.body.classList.add('edit-mode');
            if (btnText) btnText.textContent = '完成';
            localStorage.setItem('isEditMode', 'true'); // 记住状态
        } else {
            document.body.classList.remove('edit-mode');
            if (btnText) btnText.textContent = '编辑';
            localStorage.removeItem('isEditMode');      // 清除状态
            
            // 关闭所有可能打开的内联编辑器
            document.querySelectorAll('.summary-edit-form').forEach(f => {
                if (f.style.display === 'block') f.querySelector('.btn-cancel').click();
            });
        }
    };

    // 2. 初始化检查：如果本地存储里有标记，自动开启
    if (localStorage.getItem('isEditMode') === 'true') {
        setEditMode(true);
    }

    // 3. 绑定点击事件
    if (toggle) {
        toggle.addEventListener('click', () => {
            const isCurrentlyEdit = document.body.classList.contains('edit-mode');
            setEditMode(!isCurrentlyEdit);
        });
    }
}

function setupBackToTop() {
    const backBtn = document.getElementById('back-to-top');
    window.addEventListener('scroll', () => {
        if (window.scrollY > 300) backBtn.classList.add('show');
        else backBtn.classList.remove('show');
    });
    backBtn.addEventListener('click', () => window.scrollTo({ top: 0, behavior: 'smooth' }));
}

function setupMobileDashboard() {
    const statsBtn = document.getElementById('mobile-stats-btn');
    const dashboard = document.querySelector('.dashboard-column');
    const overlay = document.createElement('div');
    overlay.className = 'stats-overlay';
    document.body.appendChild(overlay);

    function toggleStats() {
        const isShow = dashboard.classList.toggle('show');
        overlay.classList.toggle('show');
        document.body.style.overflow = isShow ? 'hidden' : '';
        // 【新增】根据 isShow 状态切换标记类
        if (isShow) {
            document.body.classList.add('modal-open');
        } else {
            document.body.classList.remove('modal-open');
        }
    
    }

    if (statsBtn) statsBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        toggleStats();
    });
    overlay.addEventListener('click', toggleStats);
}

/* ---------------- 数据加载 ---------------- */

function loadStats() {
    if (typeof Chart === 'undefined') return;
    fetch('/api/stats').then(r => r.json()).then(d => {
        // 相机 & 焦段
        drawChart('cameraChart', 'doughnut', d.cameras);
        drawChart('focalChart', 'bar', d.focalLengths);

        // 3. 色彩基因 (甜甜圈图)
        if (d.colors && d.colors.length > 0) {
            // 【高级颜色映射】
            // 这里的颜色是用来代表"那类颜色"的，我们选用了更耐看的色值
            const colorMap = {
                '红色': '#E63946', // 绯红 (非正红)
                '橙色': '#F4A261', // 沙黄
                '黄色': '#E9C46A', // 姜黄
                '绿色': '#2A9D8F', // 蓝绿
                '青色': '#457B9D', // 塞尔维亚蓝
                '蓝色': '#1D3557', // 普鲁士蓝
                '紫色': '#6D597A', // 烟紫
                '粉色': '#D62828', // 胭脂红
                '黑白灰': '#343A40' // 炭灰
            };
            
            const labels = d.colors.map(i => Object.keys(i)[0]);
            const counts = d.colors.map(i => Object.values(i)[0]);
            // 查不到的颜色给一个浅灰
            const bgColors = labels.map(n => colorMap[n] || '#ADB5BD');

            new Chart(document.getElementById('colorChart'), {
                type: 'doughnut',
                data: {
                    labels: labels,
                    datasets: [{
                        data: counts,
                        backgroundColor: bgColors,
                        borderWidth: 0,
                        hoverOffset: 10 // 悬停时扇区稍微放大
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    layout: { padding: 10 },
                    plugins: {
                        legend: {
                            position: 'right',
                            labels: {
                                boxWidth: 12,
                                usePointStyle: true,
                                pointStyle: 'circle',
                                font: { size: 11 },
                                padding: 15
                            }
                        }
                    }
                }
            });
        }

        // 4. 拍摄时段 (曲线图)
        if (d.hours && d.hours.length > 0) {
            new Chart(document.getElementById('hoursChart'), {
                type: 'line',
                data: {
                    labels: Array.from({ length: 24 }, (_, i) => i + '点'),
                    datasets: [{
                        data: d.hours,
                        // 使用高级蓝
                        borderColor: '#457B9D', 
                        backgroundColor: 'rgba(69, 123, 157, 0.15)', // 淡淡的填充
                        borderWidth: 2.5,
                        tension: 0.4, // 平滑曲线
                        fill: true,
                        pointRadius: 0,
                        pointHoverRadius: 6,
                        pointHoverBackgroundColor: '#457B9D'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        x: { grid: { display: false }, ticks: { maxTicksLimit: 8, font: { size: 10 }, color: '#888' } },
                        y: { display: false }
                    },
                    interaction: {
                        mode: 'index',
                        intersect: false,
                    }
                }
            });
        }
    }).catch(e => console.error("加载统计数据失败", e));
}

function drawChart(id, type, data) {
    const ctx = document.getElementById(id);
    if (!ctx || !data || !data.length) return;

    // 【高级配色方案】莫兰迪/复古胶片色系
    // 这组颜色低饱和度，视觉舒适，且互补性强
    const premiumPalette = [
        '#264653', // 深青 (Deep Teal)
        '#2A9D8F', // 丛林绿 (Jungle Green)
        '#E9C46A', // 姜黄 (Maize)
        '#F4A261', // 柔橙 (Sandy Brown)
        '#E76F51', // 赭红 (Burnt Sienna)
        '#6D597A', // 烟紫 (Old Lavender)
        '#B56576', // 豆沙红 (English Red)
        '#E56B6F', // 珊瑚粉 (Candy Pink)
        '#355070', // 钢蓝 (Steel Blue)
        '#EAAC8B'  // 杏色 (Apricot)
    ];

   new Chart(ctx, {
        type: type,
        data: {
            labels: data.map(i => i[0] || '未知'),
            datasets: [{
                data: data.map(i => i[1]),
                backgroundColor: data.map((_, i) => premiumPalette[i % premiumPalette.length]),
                borderWidth: 0,
                
                /* 【视觉升级 1】柱状图变细、变圆 */
                borderRadius: 20, 
                barThickness: 8, 
                
                /* 【视觉升级 2】悬停时稍微放大 */
                hoverOffset: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            
            /* 【视觉升级 3】甜甜圈图变细，留出更多呼吸感 */
            cutout: '85%', 
            
            layout: { padding: 10 },
            plugins: {
                legend: {
                    display: type === 'doughnut',
                    position: 'right', /* 图例放右边，布局更平衡 */
                    labels: {
                        boxWidth: 8, /* 图例点变小 */
                        usePointStyle: true,
                        pointStyle: 'circle',
                        font: { size: 10, family: "sans-serif" },
                        padding: 10,
                        color: '#888'
                    }
                }
            },
            scales: type === 'bar' ? {
                x: { 
                    grid: { display: false }, 
                    border: { display: false }, /* 去掉坐标轴线 */
                    ticks: { font: { size: 9 }, color: '#aaa' } 
                },
                y: { display: false, grid: { display: false } }
            } : {}
        }
    });
}

function loadMyAlbums() {
    if (currUser !== 'anonymousUser') {
        fetch('/api/my-albums').then(r => r.json()).then(d => myAlbumsList = d);
    }
}

/* ---------------- 导航与弹窗逻辑 ---------------- */

function navigateModal(step) {
    const all = Array.from(document.querySelectorAll('.photo-item'));
    if (all.length === 0) return;
    const idx = all.findIndex(el => el.dataset.photoId == currentModalPhotoId);
    if (idx === -1) return;
    const newIdx = idx + step;
    if (newIdx >= 0 && newIdx < all.length) {
        openPhotoModal(all[newIdx].dataset.photoId);
    }
}

// 键盘事件
document.addEventListener('keydown', e => {
    if (document.getElementById('photo-modal').classList.contains('show')) {
        if (e.key === 'ArrowLeft') navigateModal(-1);
        else if (e.key === 'ArrowRight') navigateModal(1);
        else if (e.key === 'Escape') closeModal();
    }
});

// 打开大图弹窗
async function openPhotoModal(id) {
    currentModalPhotoId = id;
    showModal();
    document.getElementById('modal-title').textContent = '加载中...';
    
    // 【体验升级】重置环境光阴影，防止上一张图的颜色残留
    const imgEl = document.getElementById('modal-img');
    imgEl.src = ''; // 先清空，避免显示上一张残影
    imgEl.style.boxShadow = 'none';
    imgEl.classList.add('ambient-shadow'); // 确保 CSS transition 类已添加

    try {
        const res = await fetch(`/api/photo/${id}?_=${Date.now()}`);
        if (!res.ok) throw new Error('Err');
        const d = await res.json();

        // 1. 基础信息填充
        document.getElementById('modal-uploader-avatar').src = d.uploaderAvatarUrl;
        document.getElementById('modal-uploader-name').textContent = d.uploaderUsername;
        document.getElementById('modal-taken-at').textContent = d.takenAt;
        document.getElementById('modal-title').textContent = d.title;
        document.getElementById('modal-description').textContent = d.descriptionLong;
        
        // 2. 图片加载与环境光渲染
        imgEl.src = d.imageUrl;
        
        // 【新增】计算并应用环境光
        let mainColor = 'rgba(0,0,0,0.5)'; // 默认黑色阴影
        if (d.colorPalette) {
            const colors = d.colorPalette.split(',');
            if (colors.length > 0) {
                mainColor = colors[0]; // 提取第一个主色
            }
        }
        
        // 延时一点点执行，等待图片开始渲染，避免光晕闪烁
        setTimeout(() => {
            // 大范围扩散 + 负扩散半径 = 柔和聚焦光晕
            imgEl.style.boxShadow = `0 20px 100px -10px ${mainColor}`; 
        }, 100);

        // 3. EXIF 信息 (带图标的高级版)
        const exif = document.getElementById('modal-exif-data');
        if (exif) {
            exif.innerHTML = '';
            
            // 定义图标 (SVG路径)
            const icons = {
                model: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/><circle cx="12" cy="13" r="4"/></svg>',
                lens:  '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="3"/><line x1="22" y1="12" x2="18" y2="12"/><line x1="6" y1="12" x2="2" y2="12"/><line x1="12" y1="6" x2="12" y2="2"/><line x1="12" y1="22" x2="12" y2="18"/></svg>',
                aperture: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M14.31 8l5.74 9.94M9.69 8h11.48M7.38 12l5.74-9.94M9.69 16L3.95 6.06M14.31 16H2.83M16.62 12l-5.74 9.94"/></svg>',
                shutter: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
                iso: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M7 15h2v-2H7v2zm8-4h-2v6h2V11zm-4 0h-2v6h2V11z"/></svg>'
            };

            // 按顺序渲染，带上对应的图标
            const items = [
                { val: d.cameraModel, icon: icons.model },
                { val: d.focalLength, icon: icons.lens },
                { val: d.aperture,    icon: icons.aperture },
                { val: d.shutterSpeed,icon: icons.shutter },
                { val: d.iso,         icon: icons.iso }
            ];

       

        items.forEach(item => {
                // 获取原始值
                let rawVal = item.val;
                
                // 【核心修复】将 null/undefined 转为空串，然后用正则替换所有空白字符
                // \s 匹配任何空白字符，包括空格、制表符、换页符等
                let safeVal = (rawVal || "").toString().replace(/\s+/g, "");
                
                // 只有当去除所有空白后长度仍 > 0，才渲染
                if (safeVal.length > 0) {
                    exif.innerHTML += `
                        <span style="display:inline-flex; align-items:center; gap:6px;">
                            <span style="width:14px; height:14px; opacity:0.7;">${item.icon}</span>
                            ${rawVal} </span>`;
                }
            });
        }

        // 4. 标签 (Tags)
        const tags = document.getElementById('modal-tags-container');
        if (tags) {
            tags.innerHTML = '';
            if (d.tags) d.tags.forEach(t => tags.innerHTML += `<span class="tag">${t}</span>`);
        }

        // 5. 查看原图链接
        const orgLink = document.getElementById('modal-view-original-link');
        if (d.originalImageUrl) {
            orgLink.href = d.originalImageUrl;
            orgLink.style.display = 'inline-block';
        } else {
            orgLink.style.display = 'none';
        }

        // 6. 所属相册徽章
        const cal = document.getElementById('modal-current-albums-list');
        if (cal) {
            cal.innerHTML = '';
            if (d.containingAlbumIds && d.containingAlbumIds.length && myAlbumsList.length) {
                d.containingAlbumIds.forEach(aid => {
                    const a = myAlbumsList.find(al => al.id === aid);
                    if (a) {
                        const b = document.createElement('span');
                        b.className = 'current-album-badge';
                        b.textContent = a.title;
                        b.style.marginRight = '8px';
                        b.style.marginBottom = '5px';
                        b.style.cursor = 'pointer';
                        b.onclick = () => window.location.href = `/album/${a.id}`;
                        cal.appendChild(b);
                    }
                });
            }
        }

        // 7. 点赞按钮状态
        const lp = document.getElementById('modal-like-button-placeholder');
        const isLiked = globalLikedIds.includes(d.id);
        // SVG 图标定义
        const iconLiked = `<svg class="like-icon" viewBox="0 0 24 24" fill="#ff3b30" stroke="none"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`;
        const iconUnliked = `<svg class="like-icon" viewBox="0 0 24 24" fill="none" stroke="#86868b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`;
        
        if (document.getElementById('modal-comment-form')) {
            lp.innerHTML = `<button class="like-button ${isLiked ? 'liked' : ''}" data-photo-id="${d.id}">${isLiked ? iconLiked : iconUnliked}<span class="like-count">${d.likeCount}</span></button>`;
            bindLikeButton(lp.querySelector('button'));
        } else {
            lp.innerHTML = `<div class="like-button" style="cursor:default">${isLiked ? iconLiked : iconUnliked}<span class="like-count">${d.likeCount}</span></div>`;
        }

        // 8. AI 按钮监听器绑定
        const aiBtn = document.getElementById('modal-generate-ai-btn');
        if (aiBtn) {
            aiBtn.dataset.photoId = d.id;
            // 克隆节点以移除旧的事件监听器
            const newAiBtn = aiBtn.cloneNode(true);
            aiBtn.parentNode.replaceChild(newAiBtn, aiBtn);
            newAiBtn.addEventListener('click', handleAi);
        }

        // 9. 评论表单绑定
        const cForm = document.getElementById('modal-comment-form');
        if (cForm) {
            cForm.dataset.photoId = d.id;
            const newCForm = cForm.cloneNode(true);
            cForm.parentNode.replaceChild(newCForm, cForm);
            newCForm.addEventListener('submit', handleComment);
        }

        // 10. 渲染评论列表
        const cList = document.getElementById('modal-comments-list');
        cList.innerHTML = '';
        d.comments.forEach(c => {
            let rep = (currUser !== 'anonymousUser' && c.username !== currUser) ? `<button class="reply-button" onclick="replyTo('${c.username}',${c.id})">回复</button>` : '';
            let del = document.getElementById('toggle-edit-mode') ? `<button class="admin-delete-comment-button" data-cid="${c.id}" onclick="handleDelComment(this)">删除</button>` : '';
            let bdg = c.parentUsername ? `<div class="reply-badge">回复 @${c.parentUsername}</div>` : '';
            
            cList.innerHTML += `
                <div class="comment-item" id="c-${c.id}">
                    <img src="${c.avatarUrl}" class="comment-avatar">
                    <div class="comment-body">
                        <div style="display:flex;justify-content:space-between;">
                            <strong>${c.username}</strong>
                            <div>${rep}${del}</div>
                        </div>
                        ${bdg}
                        <p>${escapeHtml(c.content)}</p>
                       <span style="font-size:0.75rem;color:#999;">${formatRelativeTime(c.createdAt)}</span>
                    </div>
                </div>`;
        });

    } catch (e) { console.error(e); }
}

/* ---------------- 快速选册 ---------------- */

function openQuickAlbumDialog(pid) {
    if (event) event.stopPropagation();
    currentTargetPhotoId = pid;
    const m = document.getElementById('quick-album-modal'), l = document.getElementById('quick-album-list-container');
    l.innerHTML = '<div style="text-align:center;color:#999;padding:20px;">加载中...</div>';
    m.style.display = 'flex';
    // 【新增】
    document.body.classList.add('modal-open');
    
    if (!myAlbumsList.length) {
        l.innerHTML = '<div style="text-align:center;padding:20px;">暂无相册</div>';
        return;
    }
    
    renderAlbumList(l, []); // 先渲染空列表
    fetch(`/api/photo/${pid}?_=${Date.now()}`).then(r => r.json()).then(d => renderAlbumList(l, d.containingAlbumIds || []));
}

function renderAlbumList(c, ids) {
    c.innerHTML = '';
    myAlbumsList.forEach(a => {
        const add = ids.includes(a.id);
        const div = document.createElement('div');
        div.className = `quick-album-item ${add ? 'active' : ''}`;
        div.innerHTML = `<span style="flex-grow:1">${a.title}</span>${add ? '<span>✓</span>' : ''}`;
        if (!add) div.onclick = () => handleQuickAdd(a.id, a.title);
        else { div.style.cursor = 'default'; div.title = "已加入"; }
        c.appendChild(div);
    });
}

async function handleQuickAdd(aid, title) {
    if (!currentTargetPhotoId) return;
    try {
        const r = await fetch(`/api/album/${aid}/add/${currentTargetPhotoId}`, { method: 'POST', headers: { [csrfHeader]: csrfToken } });
        if (r.ok) { showToast(`已加入《${title}》`); closeQuickAlbumDialog(null, true); } else throw new Error();
    } catch (e) { alert('失败'); }
}

function closeQuickAlbumDialog(e, force) {
    if (force || (e && e.target.id === 'quick-album-modal')) {
        document.getElementById('quick-album-modal').style.display = 'none';
        currentTargetPhotoId = null;

        // 【新增】
        document.body.classList.remove('modal-open');

    }
}

/* ---------------- 分享卡片生成 ---------------- */

async function generateShareCard() {
    console.log("Generating share card for photo:", currentModalPhotoId);
    if (!currentModalPhotoId) {
        console.error("No photo ID found!");
        return;
    }
    
    // 打开弹窗并示加载
    const modal = document.getElementById('share-card-modal');
    const preview = document.getElementById('share-card-preview-container');
    
    // reset display just in case, but mainly use class
    modal.style.display = 'flex'; 
    // Force reflow
    void modal.offsetWidth;
    modal.classList.add('show');
    
    document.body.classList.add('modal-open');
    preview.innerHTML = '<div class="loading-spinner">生成中...</div>';

    try {
        // 1. 获取完整数据
        const res = await fetch(`/api/photo/${currentModalPhotoId}?_=${Date.now()}`);
        if (!res.ok) throw new Error('Network error');
        const d = await res.json();

        // 2. 填充隐藏模板
        const tImg = document.getElementById('share-template-img');
        const tTitle = document.getElementById('share-template-title');
        const tDesc = document.getElementById('share-template-desc');
        const tExif = document.getElementById('share-template-exif');
        const tQr = document.getElementById('share-qr-code');

        // 设置跨域属性 (关键! 否则 Canvas 会污染)
        tImg.crossOrigin = "Anonymous"; 
        tImg.src = d.imageUrl;
        // 等待图片加载完成
        await new Promise((resolve, reject) => {
            if(tImg.complete) resolve();
            else { tImg.onload = resolve; tImg.onerror = reject; }
        });

        tTitle.textContent = d.title;
        tDesc.textContent = d.descriptionLong || d.descriptionShort || "无题";
        
        let exifText = "";
        if(d.cameraModel) exifText += d.cameraModel + " · ";
        if(d.focalLength) exifText += d.focalLength + " · ";
        if(d.aperture) exifText += d.aperture + " · ";
        if(d.shutterSpeed) exifText += d.shutterSpeed + " · ";
        if(d.iso) exifText += d.iso;
        if(d.takenAt) exifText += "\n" + d.takenAt.split("T")[0];
        
        tExif.innerText = exifText;

        // 3. 生成二维码
        tQr.innerHTML = "";
        new QRCode(tQr, {
            text: window.location.origin + "/gallery?photoId=" + d.id, // 或者指向详情页
            width: 64,
            height: 64,
            colorDark : "#000000",
            colorLight : "#ffffff",
            correctLevel : QRCode.CorrectLevel.L
        });

        // 4. 等待一下稍微让 DOM 渲染 (二维码生成是同步的但 DOM 可能需要微气)
        await new Promise(r => setTimeout(r, 100));

        // 5. 截图
        const canvas = await html2canvas(document.getElementById('share-card-template'), {
            useCORS: true, // 允许跨域图片
            scale: 2, // 高清
            backgroundColor: null // 透明背景
        });

        // 6. 显示结果
        preview.innerHTML = "";
        const imgUrl = canvas.toDataURL("image/png");
        const resImg = document.createElement('img');
        resImg.src = imgUrl;
        preview.appendChild(resImg);

        // 7. 设置下载链接
        const dlBtn = document.getElementById('download-share-card-btn');
        dlBtn.href = imgUrl;
        dlBtn.download = `share_${d.id}_${Date.now()}.png`;

    } catch (e) {
        console.error(e);
        preview.innerHTML = '<div style="color:red">生成失败，请重试</div>';
    }
}

function closeShareModal() {
    const m = document.getElementById('share-card-modal');
    m.classList.remove('show');
    setTimeout(() => m.style.display = 'none', 300); // Wait for transition
    document.body.classList.remove('modal-open');
}

/* ---------------- 事件处理 (点赞、评论、删除) ---------------- */

function showModal() { modal.classList.add('show'); document.body.style.overflow = 'hidden';document.body.classList.add('modal-open'); }
function closeModal() { modal.classList.remove('show'); document.body.style.overflow = 'auto'; cancelReply();document.body.classList.remove('modal-open'); }
function escapeHtml(t) { if (!t) return t; return t.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;"); }
function showToast(m) { const t = document.getElementById('notification-toast'); document.getElementById('notification-msg').textContent = m; t.classList.add('show'); setTimeout(() => t.classList.remove('show'), 5000); }
window.hideToast = () => document.getElementById('notification-toast').classList.remove('show');

function connectWebSocket() {
    // 仅登录用户连接
    if (currUser === 'anonymousUser') return;
    if (typeof SockJS === 'undefined') return;
    
    const s = new SockJS('/ws-connect');
    stompClient = Stomp.over(s);
    stompClient.debug = null; // 关闭控制台调试日志
    stompClient.connect({}, f => stompClient.subscribe('/user/queue/notifications', n => showToast(JSON.parse(n.body).message)));
}

/* -------------------------------------------------------
   【终极修正版】loadNextPage 函数
   修复了“加载一次后感应器消失导致无法继续滚动”的 Bug
------------------------------------------------------- */
async function loadNextPage() {
    // 1. 状态检查
    if (isLast || loading) return;

    loading = true;
    const spinner = document.getElementById('loading-spinner');
    
    // 加载开始：确保加载器可见（如果被隐藏了的话）
    if (spinner) spinner.style.display = 'flex'; 

    // 2. 构建请求 URL
    let url = `/gallery?page=${nextPage}&size=9&sort=takenAt,DESC&sort=id,DESC`;
    if (query) url += `&query=${encodeURIComponent(query)}`;
    url += `&_=${Date.now()}`;

    try {
        const res = await fetch(url);
        const html = await res.text();
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');

        // 3. 检查数据有效性
        const newEls = doc.querySelectorAll('#gallery-container > *');
        
        // 如果返回空数据，直接判定为结束
        if (newEls.length === 0 || doc.querySelector('.empty-state')) {
            handleEndOfGallery();
            return;
        }

        // 4. 插入新元素
        const container = document.getElementById('gallery-container');
        let appendList = Array.from(newEls).filter(el => el.id !== 'loading-spinner');

        if (appendList.length > 0) {
            const lastGrid = document.querySelector('.gallery-day-grid:last-of-type');
            const firstNew = appendList[0];

            // 日期合并逻辑
            if (lastGrid && firstNew && firstNew.classList.contains('gallery-day-grid') && firstNew.dataset.mergeGroup === 'true') {
                firstNew.querySelectorAll('.photo-item').forEach(item => {
                    lastGrid.appendChild(item);
                    observer.observe(item); // 重新绑定动效
                    item.querySelectorAll('.like-button').forEach(bindLikeButton);
                });
                appendList.shift();
            }

            // 插入剩余部分
            appendList.forEach(el => {
                // 关键：插入到 spinner 之前，把 spinner 挤下去
                container.insertBefore(el, spinner);

                if (el.classList.contains('date-header-group')) {
                    bindSummary(el);
                    observer.observe(el);
                }
                if (el.classList.contains('gallery-day-grid')) {
                    el.querySelectorAll('.photo-item').forEach(i => {
                        observer.observe(i);
                        i.querySelectorAll('.like-button').forEach(bindLikeButton);
                        // 【新增】为新加载的卡片绑定 3D 特效
                        bindTiltEffect(i);
                    
                    });
                }
            });

            nextPage++;
        }

        // 5. 检查后端是否标记为最后一页
        // 逻辑：如果新页面里没有 loading-spinner 元素，说明后端 Thymeleaf 判定没有下一页了
        const remoteSpinner = doc.getElementById('loading-spinner');
        if (!remoteSpinner || remoteSpinner.style.display === 'none') {
            handleEndOfGallery();
        }

    } catch (e) {
        console.error("加载更多失败:", e);
        // 出错时可以暂时隐藏 spinner 防止一直转，或者保持显示让用户重试
        // 这里选择保持显示，让用户再次滚动触发
    } finally {
        loading = false;
        
        // 【核心修复点】
        // 绝对不要在这里写 spinner.style.display = 'none'！
        // 只要 isLast 为 false，加载器就必须留在底部充当“哨兵”，
        // 等待用户下次滚动到这里触发新的加载。
    }
}

// 辅助函数保持不变
/* -------------------------------------------------------
   【恢复版】handleEndOfGallery 辅助函数
   功能：隐藏加载动画，显示优雅页脚
------------------------------------------------------- */
function handleEndOfGallery() {
    isLast = true;
    
    const spinner = document.getElementById('loading-spinner');
    if (spinner) spinner.style.display = 'none';
    
    const endMsg = document.getElementById('gallery-end');
    if (endMsg) {
        // 【关键】必须是 flex，配合 CSS 里的 align-items: center
        endMsg.style.display = 'flex'; 
    }
}
function handleEndOfGallery() {
    isLast = true;
    
    // 只是简单地把加载圈藏起来
    const spinner = document.getElementById('loading-spinner');
    if (spinner) spinner.style.display = 'none';
}

async function handleComment(e) {
    e.preventDefault();
    if (!csrfToken) return;
    
    const form = e.target;
    const txt = form.querySelector('textarea');
    const pid = document.getElementById('comment-parent-id') ? document.getElementById('comment-parent-id').value : null;
    const btn = form.querySelector('button');
    
    if (!txt.value.trim()) return;
    btn.disabled = true;
    
    try {
        const fd = new FormData();
        fd.append('content', txt.value);
        if (pid) fd.append('parentId', pid);
        
        const r = await fetch(`/api/comment/${form.dataset.photoId}`, { method: 'POST', headers: { [csrfHeader]: csrfToken }, body: fd });
        if (r.ok) {
            openPhotoModal(form.dataset.photoId);
            txt.value = '';
            cancelReply();
        }
    } catch (err) { alert('失败'); } finally { btn.disabled = false; }
}

window.replyTo = function(u, id) {
    document.getElementById('comment-parent-id').value = id;
    document.getElementById('reply-target-text').textContent = `回复 @${u}`;
    document.getElementById('reply-status-bar').style.display = 'flex';
    document.getElementById('modal-comment-content').focus();
}

window.cancelReply = function() {
    const i = document.getElementById('comment-parent-id');
    const b = document.getElementById('reply-status-bar');
    const t = document.getElementById('modal-comment-content');
    if (i) i.value = '';
    if (b) b.style.display = 'none';
    if (t) t.placeholder = '写下你的想法...';
}

function bindLikeButton(btn) {
    btn.addEventListener('click', () => {
        if (!csrfToken) return;
        const id = btn.dataset.photoId;
        fetch(`/like/${id}`, { method: 'POST', headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken } })
            .then(r => r.json()).then(d => {
                if (d.likeCount !== undefined) {
                    document.querySelectorAll(`.like-button[data-photo-id="${id}"]`).forEach(b => {
                        b.querySelector('.like-count').textContent = d.likeCount;
                        b.classList.toggle('liked');
                        const isLiked = b.classList.contains('liked');
                        const oldSvg = b.querySelector('svg'); if (oldSvg) oldSvg.remove();
                        const newSvgHTML = isLiked 
                            ? `<svg class="like-icon" viewBox="0 0 24 24" fill="#ff3b30" stroke="none"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`
                            : `<svg class="like-icon" viewBox="0 0 24 24" fill="none" stroke="#86868b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`;
                        b.insertAdjacentHTML('afterbegin', newSvgHTML);
                    });
                    // 更新全局点赞列表
                    if (btn.classList.contains('liked')) {
                        if (!globalLikedIds.includes(Number(id))) globalLikedIds.push(Number(id));
                    } else {
                        globalLikedIds = globalLikedIds.filter(item => item !== Number(id));
                    }
                    // 【新增】如果是点赞操作（变成红色），触发烟花
                        if (btn.classList.contains('liked')) {
                            // 先移除类（为了支持连续点击）
                            btn.classList.remove('exploding');
                            // 强制重绘 (Reflow)，让动画能重新触发
                            void btn.offsetWidth; 
                            // 添加类，开始播放动画
                            btn.classList.add('exploding');
                        }
                }
            });
    });
}

// --- 通用删除逻辑 ---
let deleteAction = null; // 暂存待执行的删除操作

function showDeleteModal(action) {
    deleteAction = action; // 保存回调函数
    const modal = document.getElementById('delete-confirm-modal');
    modal.classList.add('show');
}

function closeDeleteModal() {
    const modal = document.getElementById('delete-confirm-modal');
    modal.classList.remove('show');
    deleteAction = null;
}

// 绑定确认按钮点击事件 (初始化时调用一次即可，放在 setupDelegation 或 DOMContentLoaded 里)
// 为了简单，我们可以直接在 DOMContentLoaded 里加，或者在这里通过判断绑定
document.addEventListener('DOMContentLoaded', () => {
    // ... 其他初始化 ...
    const confirmBtn = document.getElementById('btn-confirm-delete');
    if (confirmBtn) {
        confirmBtn.onclick = () => {
            if (deleteAction) deleteAction(); // 执行真正的删除
            closeDeleteModal();
        };
    }
});

// --- 修改后的删除照片函数 ---
function handleAjaxDelete(btn) {
    // 不再使用 confirm，改为弹窗
    showDeleteModal(async () => {
        try {
            const r = await fetch(`/api/photo/${btn.dataset.photoId}`, { method: 'DELETE', headers: { [csrfHeader]: csrfToken } });
            if (r.ok) {
                btn.closest('.photo-item').remove();
                showToast('照片已删除');
            }
        } catch (e) { alert('删除失败'); }
    });
}

// --- 修改后的删除评论函数 ---
function handleDelComment(btn) {
    showDeleteModal(async () => {
        try {
            const r = await fetch(`/api/comment/${btn.dataset.cid}`, { method: 'DELETE', headers: { [csrfHeader]: csrfToken } });
            if (r.ok) {
                document.getElementById(`c-${btn.dataset.cid}`).remove();
                showToast('评论已删除');
            }
        } catch (e) { alert('删除失败'); }
    });
}

async function handleAi(e) {
    const btn = e.currentTarget;
    btn.innerHTML = '...';
    btn.disabled = true;
    try {
        const r = await fetch(`/api/photo/${btn.dataset.photoId}/generate-ai`, { method: 'POST', headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken } });
        const d = await r.json();
        if (!r.ok) throw new Error(d.error);
        document.getElementById('modal-description').textContent = d.descriptionLong;
        btn.innerHTML = '生成完毕';
    } catch (er) {
        btn.innerHTML = '重试';
        btn.disabled = false;
        alert(er.message);
    }
}

function setupObservers() {
    observer = new IntersectionObserver(es => es.forEach(e => {
        if (e.isIntersecting) {
            e.target.classList.add('in-view');
            observer.unobserve(e.target);
        }
    }), { threshold: 0.1 });

    document.querySelectorAll('.date-header-group, .photo-item, .empty-state').forEach(el => observer.observe(el));
    document.querySelectorAll('.like-button').forEach(bindLikeButton);
    document.querySelectorAll('.date-header-group').forEach(bindSummary);
    // 【新增】为初始加载的所有卡片绑定 3D 特效
    document.querySelectorAll('.photo-item').forEach(bindTiltEffect);
    const loader = document.getElementById('loading-spinner');
    if (!isLast && loader) {
        const sObs = new IntersectionObserver(es => {
            if (es[0].isIntersecting && !loading) loadNextPage();
        }, { threshold: 0.1 });
        sObs.observe(loader);
    }
}

function setupDelegation() {
    const con = document.getElementById('gallery-container');
    if (con) con.addEventListener('click', e => {
        const item = e.target.closest('.photo-item');
        if (item && e.target.tagName === 'IMG') {
            e.preventDefault();
            openPhotoModal(item.dataset.photoId);
        }
        const del = e.target.closest('.ajax-delete-button');
        if (del) {
            e.preventDefault();
            handleAjaxDelete(del);
        }
    });
}

function bindSummary(el) {
    if (!el) return;
    const btn = el.querySelector('.edit-button');
    const form = el.querySelector('.summary-edit-form');
    
    if (btn) btn.addEventListener('click', () => {
        const p = el.querySelector('.daily-summary');
        if (p) p.style.display = 'none';
        form.style.display = 'block';
        btn.style.display = 'none';
    });
    
    if (form) {
        form.querySelector('.btn-cancel').addEventListener('click', () => {
            const p = el.querySelector('.daily-summary');
            if (p) p.style.display = 'block';
            form.style.display = 'none';
            btn.style.display = 'inline-block';
        });
        
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            try {
                const fd = new FormData();
                fd.append('content', form.querySelector('textarea').value);
                const res = await fetch(`/api/summary/${el.dataset.isoDate}`, { method: 'POST', headers: { [csrfHeader]: csrfToken }, body: fd });
                if (!res.ok) throw new Error();
                
                const d = await res.json();
                let p = el.querySelector('.daily-summary');
                
                if (d.content) {
                    if (!p) {
                        p = document.createElement('p');
                        p.className = 'daily-summary';
                        el.insertBefore(p, form);
                    }
                    p.textContent = d.content;
                    p.style.display = 'block';
                    btn.textContent = '编辑总结';
                } else {
                    if (p) p.style.display = 'none';
                    btn.textContent = '添加总结';
                }
                
                form.style.display = 'none';
                btn.style.display = 'inline-block';
            } catch (err) { alert('保存失败'); }
        });
    }
}


/* -------------------------------------------------------
   【体验升级】移动端手势控制 (Swipe)
   功能：在弹窗大图上左右滑动，切换上一张/下一张
------------------------------------------------------- */
function setupMobileGestures() {
    const modalEl = document.getElementById('photo-modal');
    let touchStartX = 0;
    let touchStartY = 0;
    
    // 监听触摸开始
    modalEl.addEventListener('touchstart', e => {
        touchStartX = e.changedTouches[0].screenX;
        touchStartY = e.changedTouches[0].screenY;
    }, { passive: true });

    // 监听触摸结束
    modalEl.addEventListener('touchend', e => {
        const touchEndX = e.changedTouches[0].screenX;
        const touchEndY = e.changedTouches[0].screenY;
        
        handleSwipeGesture(touchStartX, touchStartY, touchEndX, touchEndY);
    }, { passive: true });
}

function handleSwipeGesture(startX, startY, endX, endY) {
    const diffX = startX - endX;
    const diffY = startY - endY;
    
    // 1. 设定阈值：滑动距离超过 50px 才算有效
    // 2. 角度锁定：水平滑动的距离必须大于垂直滑动，防止滚屏时误触
    if (Math.abs(diffX) > 50 && Math.abs(diffX) > Math.abs(diffY)) {
        if (diffX > 0) {
            // 手指左滑 -> 下一张 (对应右箭头)
            navigateModal(1);
        } else {
            // 手指右滑 -> 上一张 (对应左箭头)
            navigateModal(-1);
        }
    }
}


/* -------------------------------------------------------
   【体验升级】智能导航栏 (Smart Navbar)
   功能：下滑隐藏，上滑显示
------------------------------------------------------- */
function setupSmartNavbar() {
    let lastScrollY = window.scrollY;
    const header = document.querySelector('.header-bar');
    const threshold = 100; // 滚动超过 100px 才开始生效，防止顶部抖动
    
    window.addEventListener('scroll', () => {
        const currentScrollY = window.scrollY;
        
        // 1. 在顶部时，始终显示
        if (currentScrollY < threshold) {
            header.classList.remove('nav-hidden');
            lastScrollY = currentScrollY;
            return;
        }
        
        // 2. 判断滚动方向
        // 向下滚动 (current > last) -> 隐藏
        // 向上滚动 (current < last) -> 显示
        if (currentScrollY > lastScrollY) {
            header.classList.add('nav-hidden');
        } else {
            header.classList.remove('nav-hidden');
        }
        
        lastScrollY = currentScrollY;
    }, { passive: true }); // passive: true 提升滚动性能
}
/* -------------------------------------------------------
   【工具函数】相对时间格式化 (修正版)
   规则：24小时内显示"xx前"，超过24小时显示"yyyy/MM/dd HH:mm"
------------------------------------------------------- */
function formatRelativeTime(timeStr) {
    if (!timeStr) return '';
    
    // 1. 解析 UTC 时间
    // 后端存的是 UTC (e.g. "2025-11-22T10:00:00")，补 'Z' 确保解析为 UTC
    const date = new Date(timeStr.endsWith('Z') ? timeStr : timeStr + 'Z');
    const now = new Date();
    const diff = (now - date) / 1000; // 秒差

    // 2. 24小时内的相对显示
    if (diff < 60) {
        return '刚刚';
    }
    if (diff < 3600) { // 1小时内
        return Math.floor(diff / 60) + '分钟前';
    }
    if (diff < 86400) { // 24小时内
        return Math.floor(diff / 3600) + '小时前';
    }
    
    // 3. 超过24小时，显示具体日期和时间
    // 例如: "2025/11/08 14:30"
    return date.toLocaleString('zh-CN', {
        year: 'numeric', 
        month: '2-digit', 
        day: '2-digit', 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: false // 使用24小时制
    });
}

/* -------------------------------------------------------
   【体验升级 V3】3D 视差 + 镜头内移动 (Lens Parallax)
------------------------------------------------------- */
function bindTiltEffect(card) {
    if (window.matchMedia("(hover: none)").matches) return;

    const img = card.querySelector('img'); // 获取内部图片

    card.addEventListener('mouseenter', () => {
        card.style.transition = 'transform 0.1s ease-out';
        // 图片也需要极速响应
        if (img) img.style.transition = 'transform 0.1s ease-out';
    });

    card.addEventListener('mousemove', (e) => {
        const rect = card.getBoundingClientRect();
        const x = e.clientX - rect.left; 
        const y = e.clientY - rect.top;  
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;
        
        // 1. 卡片旋转 (同之前)
        const rotateX = ((y - centerY) / centerY) * -8; 
        const rotateY = ((x - centerX) / centerX) * 8;  
        
        // 2. 【新增】图片反向位移 (Lens Effect)
        // 卡片向左倾斜时，图片向右移，制造“深坑”或“窗口”的错觉
        const moveX = ((x - centerX) / centerX) * -5; // 最大位移 5px
        const moveY = ((y - centerY) / centerY) * -5;

        // 应用卡片变换
        card.style.transform = `perspective(1000px) translateY(-8px) rotateX(${rotateX}deg) rotateY(${rotateY}deg)`;
        
        // 应用图片变换 (叠加原有的 scale 和 translateZ)
        if (img) {
            img.style.transform = `scale(1.1) translateZ(20px) translateX(${moveX}px) translateY(${moveY}px)`;
        }
    });
    
    card.addEventListener('mouseleave', () => {
        card.style.transition = 'transform 0.5s cubic-bezier(0.23, 1, 0.32, 1)'; 
        card.style.transform = ''; 
        
        // 图片复位
        if (img) {
            img.style.transition = 'transform 0.5s cubic-bezier(0.23, 1, 0.32, 1)';
            img.style.transform = 'translateZ(0) scale(1)'; // 恢复原状
        }
    });
    
    card.addEventListener('touchend', () => {
        card.style.transform = '';
        if (img) img.style.transform = '';
    }, { passive: true });
}

/* -------------------------------------------------------
   【体验升级】磁力吸附按钮 (Magnetic Button)
   让按钮像 iPadOS 图标一样吸附鼠标
------------------------------------------------------- */
function bindMagneticEffect(btn) {
    btn.addEventListener('mousemove', (e) => {
        const rect = btn.getBoundingClientRect();
        const x = e.clientX - rect.left - rect.width / 2;
        const y = e.clientY - rect.top - rect.height / 2;
        
        // 磁力强度：数值越大吸附范围越小 (3 表示移动距离是鼠标位移的 1/3)
        // 移动按钮位置
        btn.style.transform = `translate(${x / 3}px, ${y / 3}px)`;
    });

    btn.addEventListener('mouseleave', () => {
        // 鼠标离开，弹回原位 (配合 CSS transition 使用)
        btn.style.transform = '';
    });
}

// 【重要】初始化调用
// 在 setupObservers 或 DOMContentLoaded 里添加：
// document.querySelectorAll('.nav-btn, .action-btn, .like-button, .back-to-top').forEach(bindMagneticEffect);

/* -------------------------------------------------------
   【体验升级】Hero 区域视差滚动 (Parallax)
------------------------------------------------------- */
function setupHeroParallax() {
    const hero = document.querySelector('.hero-header');
    if (!hero) return;

    window.addEventListener('scroll', () => {
        const scrolled = window.scrollY;
        // 只有在 Hero 还在屏幕内时才计算，节省性能
        if (scrolled < 600) {
            // 核心公式：translateY = 滚动距离 * 0.4 (移动慢一点)
            // opacity = 1 - 滚动距离 / 400 (逐渐变淡)
            hero.style.transform = `translateY(${scrolled * 0.4}px)`;
            hero.style.opacity = 1 - scrolled / 400;
        }
    }, { passive: true });
}

// 【重要】别忘了在 DOMContentLoaded 里调用它：
// setupHeroParallax();
/* -------------------------------------------------------
   【修复版】打字机效果 (Typewriter)
   修复了文字消失的问题：强制元素可见，接管动画控制权
------------------------------------------------------- */
function typeWriterEffect(element, text, speed = 150) {
    if (!element || !text) return;
    
    // 1. 初始化内容
    element.textContent = ''; // 清空原文
    
    // 【核心修复】强制让元素可见，覆盖掉 CSS 里的 opacity: 0
    element.style.opacity = '1';
    element.style.transform = 'translateY(0)'; // 复位位置
    element.style.animation = 'none'; // 停止 CSS 的淡入动画，完全由 JS 接管
    
    // 显示光标
    element.style.borderRight = '2px solid var(--link-blue)';
    
    let i = 0;
    
    // 2. 递归打字函数
    function type() {
        if (i < text.length) {
            element.textContent += text.charAt(i);
            i++;
            
            // 随机速度，模拟真实停顿
            const randomSpeed = speed + Math.random() * 100;
            setTimeout(type, randomSpeed); 
        } else {
            // 3. 打字结束，开启光标闪烁动画
            // 注意：这里重新定义 animation，覆盖了上面的 'none'
            element.style.animation = 'cursorBlink 1s step-end infinite';
        }
    }
    
    // 开始打字
    type();
}

/* -------------------------------------------------------
   【体验升级】阅读进度条 (Reading Progress)
   在页面顶部显示一条极细的蓝色进度条
------------------------------------------------------- */
function setupReadingProgress() {
    // 1. 创建进度条元素
    const bar = document.createElement('div');
    bar.style.position = 'fixed';
    bar.style.top = '0';
    bar.style.left = '0';
    bar.style.height = '3px'; // 极细
    bar.style.backgroundColor = 'var(--link-blue)'; // 品牌蓝
    bar.style.width = '0%';
    bar.style.zIndex = '2000'; // 确保在最上层
    bar.style.transition = 'width 0.1s'; // 平滑过渡
    bar.style.boxShadow = '0 0 10px var(--link-blue)'; // 淡淡的发光
    
    // 仅在 gallery 页面添加
    if (document.getElementById('gallery-container')) {
        document.body.appendChild(bar);
        
        // 2. 监听滚动
        window.addEventListener('scroll', () => {
            const winScroll = document.body.scrollTop || document.documentElement.scrollTop;
            const height = document.documentElement.scrollHeight - document.documentElement.clientHeight;
            const scrolled = (winScroll / height) * 100;
            bar.style.width = scrolled + "%";
        }, { passive: true });
    }
}

// 别忘了在 DOMContentLoaded 里调用它： setupReadingProgress();
/* -------------------------------------------------------
   【体验升级】环形进度回顶逻辑 (Circular Progress Logic)
------------------------------------------------------- */
function setupCircularProgress() {
    const progressPath = document.querySelector('.progress-wrap path');
    if (!progressPath) return;

    const pathLength = progressPath.getTotalLength();
    
    // 初始化 SVG 描边
    progressPath.style.transition = 'none';
    progressPath.style.strokeDasharray = pathLength + ' ' + pathLength;
    progressPath.style.strokeDashoffset = pathLength;
    progressPath.getBoundingClientRect();
    progressPath.style.transition = 'stroke-dashoffset 10ms linear';

    const updateProgress = function () {
        const scroll = window.scrollY;
        const height = document.documentElement.scrollHeight - window.innerHeight;
        const progress = pathLength - (scroll * pathLength / height);
        
        // 更新圆环进度
        progressPath.style.strokeDashoffset = progress;
        
        // 控制显示/隐藏
        const wrap = document.querySelector('.progress-wrap');
        if (scroll > 50) {
            wrap.classList.add('active-progress');
        } else {
            wrap.classList.remove('active-progress');
        }
    };

    // 绑定滚动事件
    window.addEventListener('scroll', updateProgress);
    
    // 绑定点击回顶
    document.querySelector('.progress-wrap').addEventListener('click', function (event) {
        event.preventDefault();
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });
    
    // 初始化运行一次
    updateProgress();
}

// 【重要】在 DOMContentLoaded 里调用： setupCircularProgress();
// 同时请删除旧的 setupBackToTop(); 和 setupReadingProgress(); 以免功能重复

/* -------------------------------------------------------
   【体验升级】网页标题互动 (Page Visibility API)
------------------------------------------------------- */
document.addEventListener('visibilitychange', function() {
    if (document.hidden) {
        document.title = '去哪了！';
    } else {
        document.title = '常回家看看';
        // 2秒后恢复默认标题
        setTimeout(() => {
            document.title = '朝花夕拾'; 
        }, 2000);
    }
});

// 控制台彩蛋
console.log(
    `%c 朝花夕拾 %c v2.0.0 `,
    'background:#007aff; color:#ffffff; padding: 4px 8px; border-radius: 4px 0 0 4px; font-weight:bold;',
    'background:#333; color:#ffffff; padding: 4px 8px; border-radius: 0 4px 4px 0;'
);
console.log(`%c Designed by Yiliiii。`, 'color: #007aff; font-size: 12px; margin-top: 5px;');

/* -------------------------------------------------------
   【体验升级】自定义右键菜单 (Smart Context Menu)
------------------------------------------------------- */
function setupCustomContextMenu() {
    // 1. 创建菜单 DOM
    const menu = document.createElement('div');
    menu.className = 'custom-context-menu';
    document.body.appendChild(menu);

    let currentImgUrl = null; // 暂存当前选中的图片地址

    // 2. 监听右键事件
    document.addEventListener('contextmenu', (e) => {
        e.preventDefault(); // 阻止浏览器默认菜单

        const target = e.target;
        // 判断是否点击了图片 (列表中的图 或 弹窗里的大图)
        const isImage = target.tagName === 'IMG' && (target.closest('.photo-item') || target.id === 'modal-img');
        
        if (isImage) {
            currentImgUrl = target.src;
            // 如果是图片，显示图片相关选项
            menu.innerHTML = `
                <div class="ctx-item" id="ctx-download">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                    下载图片
                </div>
                <div class="ctx-item" id="ctx-copy">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg>
                    复制链接
                </div>
                <div class="ctx-separator"></div>
                <div class="ctx-item" id="ctx-refresh">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
                    刷新页面
                </div>
            `;
        } else {
            currentImgUrl = null;
            // 如果是空白处，显示通用选项
            menu.innerHTML = `
                <div class="ctx-item" id="ctx-refresh">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
                    刷新页面
                </div>
                <div class="ctx-item" id="ctx-top">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor"><path d="M18 15l-6-6-6 6"/></svg>
                    回到顶部
                </div>
            `;
        }

        // 3. 绑定点击事件
        bindMenuEvents(menu, currentImgUrl);

        // 4. 计算位置 (防止溢出屏幕)
        let x = e.clientX;
        let y = e.clientY;
        const winW = window.innerWidth;
        const winH = window.innerHeight;
        
        if (x + 170 > winW) x = winW - 170; // 宽度修正
        if (y + menu.offsetHeight > winH) y = y - menu.offsetHeight; // 高度修正

        menu.style.left = x + 'px';
        menu.style.top = y + 'px';
        menu.classList.add('visible');
    });

    // 5. 点击任意地方关闭菜单
    document.addEventListener('click', () => menu.classList.remove('visible'));
    // 滚动时也关闭
    window.addEventListener('scroll', () => menu.classList.remove('visible'));
}

// 辅助：绑定菜单项功能
function bindMenuEvents(menu, imgUrl) {
    const dlBtn = menu.querySelector('#ctx-download');
    const cpBtn = menu.querySelector('#ctx-copy');
    const rfBtn = menu.querySelector('#ctx-refresh');
    const tpBtn = menu.querySelector('#ctx-top');

    if (dlBtn && imgUrl) {
        dlBtn.onclick = () => {
            const a = document.createElement('a');
            a.href = imgUrl;
            a.download = 'photo'; // 触发下载
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
        };
    }
    if (cpBtn && imgUrl) {
        cpBtn.onclick = () => {
            navigator.clipboard.writeText(imgUrl).then(() => showToast('链接已复制'));
        };
    }
    if (rfBtn) rfBtn.onclick = () => location.reload();
    if (tpBtn) tpBtn.onclick = () => window.scrollTo({ top: 0, behavior: 'smooth' });
}

/* -------------------------------------------------------
   【体验升级】暗夜探照灯追踪逻辑
------------------------------------------------------- */
function setupDarkSpotlight() {
    const body = document.body;
    
    window.addEventListener('mousemove', (e) => {
        // 仅在深色模式下计算，节省性能
        if (body.classList.contains('dark-mode')) {
            body.style.setProperty('--x', `${e.clientX}px`);
            body.style.setProperty('--y', `${e.clientY}px`);
        }
    }, { passive: true });
}

// 【重要】在 DOMContentLoaded 里调用： setupDarkSpotlight();

/* -------------------------------------------------------
   【体验升级】导航栏游标跟随 (Lava Lamp)
------------------------------------------------------- */
function setupLavaLamp() {
    const nav = document.querySelector('.header-nav');
    const pill = document.querySelector('.nav-pill');
    const links = document.querySelectorAll('.nav-link');
    
    if (!nav || !pill) return;

    function movePill(el) {
        // 计算位置和大小
        const rect = el.getBoundingClientRect();
        const navRect = nav.getBoundingClientRect();
        
        const width = rect.width;
        const left = rect.left - navRect.left;
        
        // 应用样式
        pill.style.width = `${width}px`;
        pill.style.transform = `translateX(${left}px)`;
        pill.style.opacity = '1';
    }

    function resetPill() {
        // 回到当前激活的菜单
        const active = document.querySelector('.nav-link.active');
        if (active) {
            movePill(active);
        } else {
            pill.style.opacity = '0'; // 如果没有激活项，隐藏
        }
    }

    // 绑定事件
    links.forEach(link => {
        link.addEventListener('mouseenter', () => movePill(link));
    });
    
    nav.addEventListener('mouseleave', resetPill);
    
    // 初始化位置
    // 延时一点点，等待字体加载完成导致宽度变化
    setTimeout(resetPill, 500);
    // 窗口大小改变时重新计算
    window.addEventListener('resize', resetPill);
}

// 【重要】在 DOMContentLoaded 里调用： setupLavaLamp();
/* -------------------------------------------------------
   【体验升级】动感滚动 (Velocity Skew)
   根据滚动速度让内容产生微小的倾斜形变
------------------------------------------------------- */
function setupVelocitySkew() {
    const container = document.querySelector('.gallery-column'); // 只让左侧画廊倾斜
    if (!container) return;

    let currentPos = window.scrollY;
    let targetPos = window.scrollY;
    let skew = 0;
    let requestID;

    function loop() {
        targetPos = window.scrollY;
        
        // 计算速度差 (目标位置 - 当前帧位置)
        const diff = targetPos - currentPos;
        
        // 缓动跟随
        currentPos += diff * 0.1; 
        
        // 根据速度计算倾斜角度 (最大限制在 5 度以内，防止晕眩)
        // 0.005 是敏感度系数，越小越迟钝
        const newSkew = diff * 0.005;
        
        // 平滑插值，避免抖动
        skew += (newSkew - skew) * 0.1;
        
        // 应用变形
        //toFixed(3) 稍微降低精度以提升性能
        container.style.transform = `skewY(${skew.toFixed(3)}deg)`;
        
        requestID = requestAnimationFrame(loop);
    }
    
    // 启动循环
    loop();
}

// 【重要】在 DOMContentLoaded 里调用： setupVelocitySkew();

/* -------------------------------------------------------
   【沉浸浏览】进入过渡 (Gallery → Immersive)
   点击后先淡出黑色遮罩，再跳转至沉浸式页面
------------------------------------------------------- */
function enterImmersive(photoId) {
  // 1. 创建全屏黑色遮罩
  const overlay = document.createElement('div');
  overlay.style.cssText = `
    position: fixed; top: 0; left: 0; width: 100%; height: 100%;
    background: #000; z-index: 99999; opacity: 0;
    transition: opacity 0.4s ease; pointer-events: none;
  `;
  document.body.appendChild(overlay);

  // 2. 强制回流后触发淡入
  overlay.getBoundingClientRect();
  overlay.style.opacity = '1';

  // 3. 等待过渡完成后再跳转
  setTimeout(() => {
    window.location.href = '/immersive/' + photoId;
  }, 400);
}