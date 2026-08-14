/* ==========================================
   JUMP 登录页面 — JavaScript 交互逻辑
   Canvas 粒子连接器动效 + 表单交互
   ========================================== */

// ============================================
// 1. Canvas 粒子连接器动效系统
// ============================================

class ParticleNetwork {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas.getContext('2d');

        // 粒子配置
        this.config = {
            particleCount: 80,          // 粒子数量
            connectionDistance: 150,     // 粒子连线最大距离
            particleSize: { min: 1, max: 3 },  // 粒子大小范围
            speed: { min: 0.15, max: 0.5 },    // 粒子速度范围
            mouseRadius: 180,           // 鼠标影响半径
            mouseForce: 0.02,           // 鼠标吸引力
            colors: {
                particle: 'rgba(59, 130, 246, 0.8)',         // 粒子颜色
                particleGlow: 'rgba(96, 165, 250, 0.6)',     // 粒子发光色
                connection: 'rgba(59, 130, 246, ',            // 连线颜色（不含透明度）
                connectionActive: 'rgba(96, 165, 250, ',     // 鼠标附近的连线颜色
            }
        };

        this.particles = [];
        this.mouse = { x: null, y: null, isActive: false };
        this.animationId = null;

        this.init();
    }

    // 初始化
    init() {
        this.resize();
        this.createParticles();
        this.bindEvents();
        this.animate();
    }

    // 调整画布大小
    resize() {
        const rect = this.canvas.parentElement.getBoundingClientRect();
        this.canvas.width = rect.width * window.devicePixelRatio;
        this.canvas.height = rect.height * window.devicePixelRatio;
        this.canvas.style.width = rect.width + 'px';
        this.canvas.style.height = rect.height + 'px';
        this.ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
        this.width = rect.width;
        this.height = rect.height;
    }

    // 创建粒子
    createParticles() {
        this.particles = [];
        const { particleCount, particleSize, speed } = this.config;

        for (let i = 0; i < particleCount; i++) {
            this.particles.push({
                x: Math.random() * this.width,
                y: Math.random() * this.height,
                vx: (Math.random() - 0.5) * (speed.max - speed.min) + speed.min * (Math.random() > 0.5 ? 1 : -1),
                vy: (Math.random() - 0.5) * (speed.max - speed.min) + speed.min * (Math.random() > 0.5 ? 1 : -1),
                size: Math.random() * (particleSize.max - particleSize.min) + particleSize.min,
                opacity: Math.random() * 0.5 + 0.3,
                pulseSpeed: Math.random() * 0.02 + 0.01,
                pulseOffset: Math.random() * Math.PI * 2,
                // 连接器插针类型：圆形或方形
                type: Math.random() > 0.7 ? 'square' : 'circle'
            });
        }
    }

    // 事件绑定
    bindEvents() {
        // 鼠标移动
        this.canvas.parentElement.addEventListener('mousemove', (e) => {
            const rect = this.canvas.parentElement.getBoundingClientRect();
            this.mouse.x = e.clientX - rect.left;
            this.mouse.y = e.clientY - rect.top;
            this.mouse.isActive = true;
        });

        // 鼠标离开
        this.canvas.parentElement.addEventListener('mouseleave', () => {
            this.mouse.isActive = false;
        });

        // 窗口大小变化
        window.addEventListener('resize', () => {
            this.resize();
            // 确保粒子在新的画布范围内
            this.particles.forEach(p => {
                if (p.x > this.width) p.x = Math.random() * this.width;
                if (p.y > this.height) p.y = Math.random() * this.height;
            });
        });
    }

    // 更新粒子状态
    updateParticles() {
        const { mouseRadius, mouseForce } = this.config;
        const time = Date.now() * 0.001;

        this.particles.forEach(p => {
            // 基础运动
            p.x += p.vx;
            p.y += p.vy;

            // 脉冲效果（模拟电流流动）
            p.currentOpacity = p.opacity + Math.sin(time * p.pulseSpeed * 60 + p.pulseOffset) * 0.2;

            // 鼠标交互：吸引效果
            if (this.mouse.isActive) {
                const dx = this.mouse.x - p.x;
                const dy = this.mouse.y - p.y;
                const dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < mouseRadius) {
                    const force = (1 - dist / mouseRadius) * mouseForce;
                    p.vx += dx * force * 0.01;
                    p.vy += dy * force * 0.01;
                }
            }

            // 速度阻尼（防止粒子加速过快）
            p.vx *= 0.99;
            p.vy *= 0.99;

            // 边界处理：循环穿越
            if (p.x < -10) p.x = this.width + 10;
            if (p.x > this.width + 10) p.x = -10;
            if (p.y < -10) p.y = this.height + 10;
            if (p.y > this.height + 10) p.y = -10;
        });
    }

    // 绘制连线（模拟电路板走线）
    drawConnections() {
        const { connectionDistance, colors } = this.config;
        const particles = this.particles;

        for (let i = 0; i < particles.length; i++) {
            for (let j = i + 1; j < particles.length; j++) {
                const dx = particles[i].x - particles[j].x;
                const dy = particles[i].y - particles[j].y;
                const dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < connectionDistance) {
                    const opacity = (1 - dist / connectionDistance) * 0.5;

                    // 判断是否在鼠标附近（增强效果）
                    let isNearMouse = false;
                    if (this.mouse.isActive) {
                        const midX = (particles[i].x + particles[j].x) / 2;
                        const midY = (particles[i].y + particles[j].y) / 2;
                        const mouseDist = Math.sqrt(
                            (midX - this.mouse.x) ** 2 + (midY - this.mouse.y) ** 2
                        );
                        isNearMouse = mouseDist < this.config.mouseRadius;
                    }

                    const color = isNearMouse ? colors.connectionActive : colors.connection;
                    const lineOpacity = isNearMouse ? opacity * 1.8 : opacity;

                    this.ctx.beginPath();
                    this.ctx.strokeStyle = color + lineOpacity + ')';
                    this.ctx.lineWidth = isNearMouse ? 1.2 : 0.6;
                    this.ctx.moveTo(particles[i].x, particles[i].y);

                    // 偶尔绘制直角走线（模拟 PCB 电路板走线风格）
                    if ((i + j) % 5 === 0 && dist > connectionDistance * 0.4) {
                        // L 型走线
                        this.ctx.lineTo(particles[j].x, particles[i].y);
                        this.ctx.lineTo(particles[j].x, particles[j].y);
                    } else {
                        this.ctx.lineTo(particles[j].x, particles[j].y);
                    }

                    this.ctx.stroke();

                    // 在连线节点处绘制小焊点
                    if (isNearMouse && dist < connectionDistance * 0.5) {
                        this.ctx.beginPath();
                        this.ctx.fillStyle = `rgba(96, 165, 250, ${lineOpacity * 0.6})`;
                        this.ctx.arc(
                            (particles[i].x + particles[j].x) / 2,
                            (particles[i].y + particles[j].y) / 2,
                            1.5, 0, Math.PI * 2
                        );
                        this.ctx.fill();
                    }
                }
            }
        }
    }

    // 绘制粒子（模拟连接器插针）
    drawParticles() {
        const time = Date.now() * 0.001;

        this.particles.forEach(p => {
            const opacity = Math.max(0.1, Math.min(1, p.currentOpacity));

            // 外层发光
            this.ctx.beginPath();
            this.ctx.fillStyle = `rgba(59, 130, 246, ${opacity * 0.15})`;
            this.ctx.arc(p.x, p.y, p.size * 4, 0, Math.PI * 2);
            this.ctx.fill();

            // 粒子主体
            if (p.type === 'square') {
                // 方形粒子（模拟方形插针）
                const s = p.size * 1.5;
                this.ctx.fillStyle = `rgba(96, 165, 250, ${opacity})`;
                this.ctx.fillRect(p.x - s / 2, p.y - s / 2, s, s);
            } else {
                // 圆形粒子
                this.ctx.beginPath();
                this.ctx.fillStyle = `rgba(96, 165, 250, ${opacity})`;
                this.ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
                this.ctx.fill();
            }
        });
    }

    // 绘制鼠标附近的范围指示
    drawMouseEffect() {
        if (!this.mouse.isActive) return;

        const gradient = this.ctx.createRadialGradient(
            this.mouse.x, this.mouse.y, 0,
            this.mouse.x, this.mouse.y, this.config.mouseRadius
        );
        gradient.addColorStop(0, 'rgba(59, 130, 246, 0.03)');
        gradient.addColorStop(0.5, 'rgba(59, 130, 246, 0.015)');
        gradient.addColorStop(1, 'rgba(59, 130, 246, 0)');

        this.ctx.beginPath();
        this.ctx.fillStyle = gradient;
        this.ctx.arc(this.mouse.x, this.mouse.y, this.config.mouseRadius, 0, Math.PI * 2);
        this.ctx.fill();
    }

    // 动画主循环
    animate() {
        this.ctx.clearRect(0, 0, this.width, this.height);

        this.drawMouseEffect();
        this.updateParticles();
        this.drawConnections();
        this.drawParticles();

        this.animationId = requestAnimationFrame(() => this.animate());
    }

    // 销毁实例
    destroy() {
        cancelAnimationFrame(this.animationId);
    }
}


// ============================================
// 2. 表单交互逻辑
// ============================================

document.addEventListener('DOMContentLoaded', () => {

    // 初始化粒子动效
    const particleNetwork = new ParticleNetwork('particle-canvas');

    // ---- 密码显示/隐藏 ----
    const toggleBtn = document.getElementById('toggle-password');
    const passwordInput = document.getElementById('password');
    const eyeOpen = toggleBtn.querySelector('.eye-open');
    const eyeClosed = toggleBtn.querySelector('.eye-closed');

    toggleBtn.addEventListener('click', () => {
        const isPassword = passwordInput.type === 'password';
        passwordInput.type = isPassword ? 'text' : 'password';
        eyeOpen.style.display = isPassword ? 'none' : 'block';
        eyeClosed.style.display = isPassword ? 'block' : 'none';
    });

    // ---- 表单提交 ----
    const loginForm = document.getElementById('login-form');
    const loginBtn = document.getElementById('login-btn');
    const btnText = loginBtn.querySelector('.btn-text');
    const btnLoader = loginBtn.querySelector('.btn-loader');
    const usernameInput = document.getElementById('username');

    loginForm.addEventListener('submit', (e) => {
        e.preventDefault();

        // 验证
        let hasError = false;

        if (!usernameInput.value.trim()) {
            shakeInput(usernameInput);
            hasError = true;
        }

        if (!passwordInput.value.trim()) {
            shakeInput(passwordInput);
            hasError = true;
        }

        if (hasError) return;

        // 显示 loading 状态
        loginBtn.classList.add('loading');
        btnText.style.display = 'none';
        btnLoader.style.display = 'inline-flex';

        // 模拟登录请求（2秒后恢复）
        setTimeout(() => {
            loginBtn.classList.remove('loading');
            btnText.style.display = 'inline';
            btnLoader.style.display = 'none';

            // 这里可以替换为实际的登录逻辑
            showLoginSuccess();
        }, 2000);
    });

    // 输入框 shake 动画
    function shakeInput(input) {
        const group = input.closest('.input-group');
        group.classList.add('shake');
        setTimeout(() => group.classList.remove('shake'), 500);
    }

    // 登录成功提示（演示用）
    function showLoginSuccess() {
        const btn = document.getElementById('login-btn');
        const btnTextEl = btn.querySelector('.btn-text');
        btnTextEl.textContent = '✓ 登录成功';
        btn.style.background = 'linear-gradient(135deg, #10b981, #059669)';

        setTimeout(() => {
            btnTextEl.textContent = '登 录';
            btn.style.background = '';
        }, 2000);
    }

    // ---- 输入框 focus 动效增强 ----
    const inputs = document.querySelectorAll('.input-group input');

    inputs.forEach(input => {
        // 输入时的微妙反馈
        input.addEventListener('input', () => {
            if (input.value.length > 0) {
                input.closest('.input-group').classList.add('has-value');
            } else {
                input.closest('.input-group').classList.remove('has-value');
            }
        });
    });

    // ---- 键盘快捷键 ----
    document.addEventListener('keydown', (e) => {
        // Enter 键提交表单
        if (e.key === 'Enter' && document.activeElement.tagName !== 'BUTTON') {
            loginForm.dispatchEvent(new Event('submit'));
        }
    });

});
