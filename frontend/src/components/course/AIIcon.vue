<template>
    <div class="ai-container" @click="handleInteraction">
        <canvas ref="canvasRef"></canvas>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive, defineEmits } from 'vue';

const emit = defineEmits(['interaction', 'toggle']);

const CONFIG = {
    size: 1,
    particleCount: 100,
    connectionDistance: 50,
    headRadius: 62,
    eyeWidth: 11,
    eyeHeight: 16,
    eyeSpacing: 23,

    particleSpeedIdle: 0.3,
    particleSpeedActive: 1.5,

    colors: {
        idle: { line: [0, 180, 255], node: [0, 220, 255], glow: [0, 200, 255] },
        generating: { line: [255, 100, 0], node: [255, 150, 0], glow: [255, 120, 0] },
        angry: { line: [255, 0, 50], node: [255, 50, 50], glow: [255, 0, 0] }
    },

    gravity: 1.5,
    jumpForce: -10,
    recoilScale: 0.35
};

const SIZES = reactive({
    headRadius: CONFIG.headRadius * CONFIG.size,
    connectionDistance: CONFIG.connectionDistance * CONFIG.size,
    eyeWidth: CONFIG.eyeWidth * CONFIG.size,
    eyeHeight: CONFIG.eyeHeight * CONFIG.size,
    eyeSpacing: CONFIG.eyeSpacing * CONFIG.size,
    particleRadius: { min: 0.5 * CONFIG.size, max: 2.0 * CONFIG.size }
});

const updateSizes = () => {
    SIZES.headRadius = CONFIG.headRadius * CONFIG.size;
    SIZES.connectionDistance = CONFIG.connectionDistance * CONFIG.size;
    SIZES.eyeWidth = CONFIG.eyeWidth * CONFIG.size;
    SIZES.eyeHeight = CONFIG.eyeHeight * CONFIG.size;
    SIZES.eyeSpacing = CONFIG.eyeSpacing * CONFIG.size;
    SIZES.particleRadius.min = 0.5 * CONFIG.size;
    SIZES.particleRadius.max = 2.0 * CONFIG.size;

    if (particles.length > 0) {
        particles.forEach(p => {
            p.radius = (Math.random() * (SIZES.particleRadius.max - SIZES.particleRadius.min) + SIZES.particleRadius.min);
        });
    }
};

const canvasRef = ref(null);
let ctx = null;
let animationFrameId = null;
let width = 0;
let height = 0;

let particles = [];
let isGenerating = ref(false);
let currentSpeed = CONFIG.particleSpeedIdle;

let lastFrameTime = 0;
const targetFPS = 60;
const frameInterval = 1000 / targetFPS;

const headPose = reactive({
    x: 0, y: 0,
    targetX: 0, targetY: 0,
    perspective: 0,
    targetPerspective: 0
});

const recoilPhysics = reactive({
    y: 0,
    vy: 0,
    scale: 1.0,
    targetScale: 1.0,
    isJumping: false,
    hasLanded: false
});

const anger = reactive({
    level: 0,
    lastClickTime: 0,
    clickCount: 0
});

let isBlinking = false;
let blinkTimeout = null;
let lookTimeout = null;

class Particle {
    constructor() {
        this.reset();
        this._distSq = 0;
        this._dx = 0;
        this._dy = 0;
    }

    reset() {
        const angle = Math.random() * Math.PI * 2;
        const radius = Math.random() * SIZES.headRadius;
        this.x = Math.cos(angle) * radius;
        this.y = Math.sin(angle) * radius;
        const speedAngle = Math.random() * Math.PI * 2;
        this.baseSpeed = (Math.random() * 0.5 + 0.5);
        this.vx = Math.cos(speedAngle);
        this.vy = Math.sin(speedAngle);
        this.radius = Math.random() * (SIZES.particleRadius.max - SIZES.particleRadius.min) + SIZES.particleRadius.min;
    }

    update(speedMultiplier) {
        this.x += this.vx * this.baseSpeed * speedMultiplier;
        this.y += this.vy * this.baseSpeed * speedMultiplier;

        const distSq = this.x * this.x + this.y * this.y;
        const maxDistSq = SIZES.headRadius * SIZES.headRadius;

        if (distSq > maxDistSq) {
            this.vx *= -1;
            this.vy *= -1;
            const scale = 0.95;
            this.x *= scale;
            this.y *= scale;
        }
    }
}

const init = () => {
    const canvas = canvasRef.value;
    if (!canvas) return;
    ctx = canvas.getContext('2d');
    resize();
    updateSizes();
    particles = Array.from({ length: CONFIG.particleCount }, () => new Particle());
    scheduleLook();
    animate();
};

const resize = () => {
    const canvas = canvasRef.value;
    width = CONFIG.headRadius * 2;
    height = canvas.parentElement.offsetHeight;
    canvas.width = width;
    canvas.height = height;
};

const toggleGenerating = () => { isGenerating.value = !isGenerating.value; };

const handleInteraction = () => {
    const now = Date.now();
    if (now - anger.lastClickTime < 400) {
        anger.clickCount++;
        if (anger.clickCount > 2) anger.level = Math.min(1, anger.level + 0.4);
    } else {
        anger.clickCount = 1;
    }
    anger.lastClickTime = now;

    headPose.targetX = 0;
    headPose.targetY = 0;
    headPose.targetPerspective = 0;

    if (!recoilPhysics.isJumping) {
        recoilPhysics.isJumping = true;
        recoilPhysics.hasLanded = false;
        recoilPhysics.scale = 1.0;
        recoilPhysics.targetScale = CONFIG.recoilScale;
    }

    const maxJumpHeight = -200 * CONFIG.size;
    if (recoilPhysics.y > maxJumpHeight) {
        recoilPhysics.vy += CONFIG.jumpForce * CONFIG.size;
    } else {
        recoilPhysics.vy = 0;
    }

    if (!isBlinking) {
        isBlinking = true;
        blinkTimeout = setTimeout(() => { isBlinking = false; }, 100);
    }
    setTimeout(() => {
        emit('interaction', {
            angerLevel: anger.level
        })
    }, 300)

};

const scheduleLook = () => {
    const delay = 6000 + Math.random() * 3000;
    lookTimeout = setTimeout(() => {
        const dir = Math.random() > 0.5 ? 1 : -1;
        headPose.targetX = dir * 20 * CONFIG.size;
        headPose.targetY = -10 * CONFIG.size;
        headPose.targetPerspective = dir;

        setTimeout(() => {
            headPose.targetX = 0;
            headPose.targetY = 0;
            headPose.targetPerspective = 0;
        }, 20000 + Math.random() * 10000);

        scheduleLook();
    }, delay);
};

const lerp = (a, b, n) => a + (b - a) * n;

const animate = (currentTime) => {
    animationFrameId = requestAnimationFrame(animate);

    const delta = currentTime - lastFrameTime;
    if (delta < frameInterval) return;
    lastFrameTime = currentTime - (delta % frameInterval);

    if (!ctx) return;

    const targetSpeed = isGenerating.value ? CONFIG.particleSpeedActive : CONFIG.particleSpeedIdle;
    currentSpeed = lerp(currentSpeed, targetSpeed, 0.05);

    if (anger.level > 0) anger.level = Math.max(0, anger.level - 0.003);

    headPose.x = lerp(headPose.x, headPose.targetX, 0.05);
    headPose.y = lerp(headPose.y, headPose.targetY, 0.05);
    headPose.perspective = lerp(headPose.perspective, headPose.targetPerspective, 0.05);

    if (recoilPhysics.isJumping) {
        recoilPhysics.vy += CONFIG.gravity * CONFIG.size;
        recoilPhysics.y += recoilPhysics.vy;
        recoilPhysics.scale = lerp(recoilPhysics.scale, recoilPhysics.targetScale, 0.15);

        const maxJumpHeight = -200 * CONFIG.size;
        if (recoilPhysics.y < maxJumpHeight) {
            recoilPhysics.y = maxJumpHeight;
            if (recoilPhysics.vy < 0) recoilPhysics.vy = 0;
        }

        if (recoilPhysics.y >= 0) {
            recoilPhysics.y = 0;
            recoilPhysics.vy = 0;

            if (!recoilPhysics.hasLanded) {
                recoilPhysics.hasLanded = true;
                recoilPhysics.targetScale = 1.0;
            }

            if (recoilPhysics.hasLanded && Math.abs(recoilPhysics.scale - 1.0) < 0.01) {
                recoilPhysics.scale = 1.0;
                recoilPhysics.isJumping = false;
            }
        }
    }

    let colorScheme = CONFIG.colors.idle;
    if (anger.level > 0.1) colorScheme = CONFIG.colors.angry;
    else if (isGenerating.value) colorScheme = CONFIG.colors.generating;

    ctx.clearRect(0, 0, width, height);
    ctx.save();

    const cx = width / 2;
    const cy = height / 2 + 90 * CONFIG.size;

    if (recoilPhysics.y < SIZES.headRadius) {
        ctx.translate(cx, cy + recoilPhysics.y);
    }
    ctx.scale(recoilPhysics.scale, recoilPhysics.scale);
    ctx.translate(-cx, -cy);

    particles.forEach(p => p.update(currentSpeed));

    ctx.lineWidth = 1 * CONFIG.size;
    const connDistSq = SIZES.connectionDistance * SIZES.connectionDistance;

    const linesByAlpha = new Map();

    for (let i = 0; i < particles.length; i++) {
        for (let j = i + 1; j < particles.length; j++) {
            const dx = particles[i].x - particles[j].x;
            const dy = particles[i].y - particles[j].y;
            const distSq = dx * dx + dy * dy;

            if (distSq < connDistSq) {
                const alpha = Math.floor((1 - (distSq / connDistSq)) * 0.4 * 100) / 100;
                if (!linesByAlpha.has(alpha)) {
                    linesByAlpha.set(alpha, []);
                }
                linesByAlpha.get(alpha).push({
                    x1: cx + particles[i].x,
                    y1: cy + particles[i].y,
                    x2: cx + particles[j].x,
                    y2: cy + particles[j].y
                });
            }
        }
    }

    linesByAlpha.forEach((lines, alpha) => {
        ctx.strokeStyle = `rgba(${colorScheme.line.join(',')},${alpha})`;
        ctx.beginPath();
        lines.forEach(line => {
            ctx.moveTo(line.x1, line.y1);
            ctx.lineTo(line.x2, line.y2);
        });
        ctx.stroke();
    });

    ctx.shadowColor = `rgba(${colorScheme.glow.join(',')}, 0.8)`;
    ctx.shadowBlur = 5 * CONFIG.size;
    ctx.fillStyle = `rgba(${colorScheme.node.join(',')}, 0.9)`;

    particles.forEach(p => {
        ctx.beginPath();
        ctx.arc(cx + p.x, cy + p.y, p.radius, 0, Math.PI * 2);
        ctx.fill();
    });
    ctx.shadowBlur = 0;

    drawEyes(cx, cy, colorScheme);

    ctx.restore();
};

const drawEyes = (cx, cy, colorScheme) => {
    const p = headPose.perspective;

    const offsetX = headPose.x;
    const offsetY = headPose.y;

    const leftScale = 1.0 + p * 0.15;
    const rightScale = 1.0 - p * 0.15;
    const currentSpacing = SIZES.eyeSpacing - Math.abs(p) * 10 * CONFIG.size;

    drawSingleEye(
        cx - currentSpacing + offsetX,
        cy + offsetY,
        SIZES.eyeWidth,
        SIZES.eyeHeight,
        leftScale,
        1,
        colorScheme
    );

    drawSingleEye(
        cx + currentSpacing + offsetX,
        cy + offsetY,
        SIZES.eyeWidth,
        SIZES.eyeHeight,
        rightScale,
        -1,
        colorScheme
    );
};

const drawSingleEye = (x, y, w, h, scale, side, colorScheme) => {
    ctx.save();
    ctx.translate(x, y);
    ctx.scale(scale, isBlinking ? 0.05 : 1);

    const angryPercent = anger.level;

    ctx.fillStyle = `rgba(255, 255, 255, 0.9)`;
    ctx.shadowColor = `rgba(${colorScheme.glow.join(',')}, 0.7)`;
    ctx.shadowBlur = 15 * CONFIG.size;

    ctx.beginPath();

    if (angryPercent < 0.1) {
        ctx.ellipse(0, 0, w, h, 0, 0, Math.PI * 2);
        ctx.fill();
    } else {
        const outerY = 15 * CONFIG.size - h - angryPercent * 10 * CONFIG.size;
        const innerY = 15 * CONFIG.size - h + angryPercent * 15 * CONFIG.size;
        const leftX = -w;
        const rightX = w;

        let p1_y, p2_y;
        if (side === 1) {
            p1_y = outerY;
            p2_y = innerY;
        } else {
            p1_y = innerY;
            p2_y = outerY;
        }

        ctx.ellipse(0, 0, w, h, 0, 0, Math.PI);
        ctx.lineTo(leftX, p1_y);
        ctx.lineTo(rightX, p2_y);
        ctx.closePath();
        ctx.fill();

        ctx.shadowBlur = 0;
        ctx.beginPath();
        const browThickness = 4 * angryPercent * CONFIG.size;
        ctx.moveTo(leftX - 3 * CONFIG.size, p1_y - browThickness);
        ctx.lineTo(rightX + 3 * CONFIG.size, p2_y - browThickness);
        ctx.lineTo(rightX + 3 * CONFIG.size, p2_y + 1.5 * CONFIG.size);
        ctx.lineTo(leftX - 3 * CONFIG.size, p1_y + 1.5 * CONFIG.size);
        ctx.closePath();
        ctx.fill();
    }

    ctx.restore();
};

onMounted(() => {
    window.addEventListener('resize', resize);
    init();
});

onUnmounted(() => {
    cancelAnimationFrame(animationFrameId);
    clearTimeout(lookTimeout);
    clearTimeout(blinkTimeout);
    window.removeEventListener('resize', resize);
});

defineExpose({
    setSize: (newSize) => {
        CONFIG.size = Math.max(0.1, Math.min(3.0, newSize));
        updateSizes();
    },
    getSize: () => CONFIG.size
});
</script>

<style scoped>
.ai-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
}

.controls {
    display: flex;
    align-items: center;
    gap: 15px;
    padding: 10px;
}

.ai-container {
    width: 100%;
    max-width: 1000px;
    max-height: 1000px;
    background: transparent;
    position: relative;
    overflow: hidden;
    cursor: pointer;
}

canvas {
    width: 100%;
    height: 100%;
    display: block;
}
</style>