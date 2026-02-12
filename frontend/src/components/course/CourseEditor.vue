<script>

const mockDatabase = {
    courses: {
        1: {
            id: 1,
            name: 'Введение в машинное обучение',
            steps: [
                {
                    id: 'initial',
                    name: 'Начало',
                    content: 'Введение в курс машинного обучения',
                    type: 'THEORY',
                    x: 0,
                    y: 0,
                    isInitial: true
                },
                {
                    id: 2,
                    name: 'Что такое ML?',
                    content: 'Определение машинного обучения',
                    type: 'THEORY',
                    x: 200,
                    y: -150,
                    isInitial: false
                },
                {
                    id: 3,
                    name: 'Типы ML',
                    content: 'Обучение с учителем, без учителя',
                    type: 'THEORY',
                    x: 200,
                    y: 150,
                    isInitial: false
                },
                {
                    id: 4,
                    name: 'Тест: Основы',
                    content: 'Проверка знаний',
                    type: 'TEST',
                    x: 450,
                    y: 0,
                    isInitial: false
                }
            ],
            edges: [
                { id: 1, fromStepId: 'initial', toStepId: 2 },
                { id: 2, fromStepId: 'initial', toStepId: 3 },
                { id: 3, fromStepId: 2, toStepId: 4 },
                { id: 4, fromStepId: 3, toStepId: 4 }
            ]
        }
    },

    api: {
        async getCourseStructure(courseId) {
            const course = mockDatabase.courses[courseId]
            if (!course) throw new Error('Курс не найден')
            return {
                steps: course.steps.map(s => ({ ...s })),
                edges: course.edges.map(e => ({ ...e }))
            }
        },

        async saveCourseStructure(courseId, data) {
            const course = mockDatabase.courses[courseId]
            if (!course) throw new Error('Курс не найден')
            course.steps = data.steps.map(s => ({ ...s }))
            course.edges = data.edges.map(e => ({ ...e }))
            console.log('Сохранено:', JSON.stringify(data, null, 2))
            return { success: true }
        },
    }
}

export default {
    data() {
        return {
            canvasOffset: { x: 0, y: 0 },
            scale: 1,
            minScale: 0.2,
            maxScale: 3,

            isPanning: false,
            isDraggingStep: false,
            isDraggingEdge: false,
            panStart: { x: 0, y: 0 },
            dragStart: { x: 0, y: 0 },
            draggedStep: null,
            dragEdgeStart: null,
            dragCurrentX: 0,
            dragCurrentY: 0,

            steps: [],
            edges: [],
            selectedStep: null,
            selectedEdge: null,

            contextMenu: {
                visible: false,
                x: 0,
                y: 0,
                step: null,
                canvasPosition: null
            },

            edgeContextMenu: {
                visible: false,
                x: 0,
                y: 0,
                edge: null
            },

            modal: {
                visible: false,
                isEdit: false,
                stepId: null,
                form: {
                    name: '',
                    content: '',
                    type: 'THEORY'
                }
            },

            nextStepId: 10,
            nodeSize: 100,
            initialNodeSize: 120,
            arrowOffset: 15
        }
    },

    methods: {
        autoLayout() {
            if (this.steps.length === 0) return
            const root = this.steps.find(s => s.isInitial) || this.findRoot()
            if (!root) return

            const children = {}
            const parents = {}
            this.steps.forEach(s => {
                children[s.id] = []
                parents[s.id] = []
            })

            this.edges.forEach(edge => {
                if (children[edge.fromStepId]) {
                    children[edge.fromStepId].push(edge.toStepId)
                }
                if (parents[edge.toStepId]) {
                    parents[edge.toStepId].push(edge.fromStepId)
                }
            })

            const levels = {}
            const queue = [{ id: root.id, level: 0 }]
            levels[root.id] = 0

            while (queue.length > 0) {
                const { id, level } = queue.shift()

                children[id].forEach(childId => {
                    if (!levels[childId]) {
                        levels[childId] = level + 1
                        queue.push({ id: childId, level: level + 1 })
                    }
                })
            }
            let maxLevel = Math.max(...Object.values(levels))
            this.steps.forEach(step => {
                if (levels[step.id] === undefined) {
                    maxLevel++
                    levels[step.id] = maxLevel
                }
            })

            const nodesByLevel = {}
            Object.entries(levels).forEach(([id, level]) => {
                if (!nodesByLevel[level]) nodesByLevel[level] = []
                nodesByLevel[level].push(id)
            })
            this.orderNodesByLevel(nodesByLevel, children, parents)
            const levelHeight = 300
            const nodeWidth = 260
            const layout = {}

            Object.entries(nodesByLevel).forEach(([level, nodeIds]) => {
                const levelNum = parseInt(level)
                const y = -levelNum * levelHeight
                const totalWidth = (nodeIds.length - 1) * nodeWidth
                const startX = -totalWidth / 2

                nodeIds.forEach((nodeId, index) => {
                    layout[nodeId] = {
                        x: startX + index * nodeWidth,
                        y: y
                    }
                })
            })

            this.steps.forEach(step => {
                if (layout[step.id]) {
                    this.animateStep(step, step.x, step.y, layout[step.id].x, layout[step.id].y)
                }
            })

            setTimeout(() => this.centerCanvas(), 350)
        },

        findRoot() {
            const hasIncoming = new Set(this.edges.map(e => e.toStepId))
            const root = this.steps.find(s => !hasIncoming.has(s.id))
            return root || this.steps[0]
        },

        orderNodesByLevel(nodesByLevel, children, parents) {
            const levels = Object.keys(nodesByLevel).map(Number).sort((a, b) => a - b)

            levels.forEach(level => {
                const nodeIds = nodesByLevel[level]

                nodeIds.sort((a, b) => {
                    const getConnectedPos = (id) => {
                        const connected = level === 0
                            ? children[id] || []
                            : parents[id] || []

                        if (connected.length === 0) return 0
                        const otherLevel = level === 0 ? 1 : level - 1
                        const otherNodes = nodesByLevel[otherLevel] || []

                        let sumIndex = 0
                        let count = 0

                        connected.forEach(connId => {
                            const idx = otherNodes.indexOf(connId)
                            if (idx !== -1) {
                                sumIndex += idx
                                count++
                            }
                        })

                        return count > 0 ? sumIndex / count : 0
                    }

                    return getConnectedPos(a) - getConnectedPos(b)
                })
            })
        },

        animateStep(step, startX, startY, endX, endY) {
            const duration = 300
            const startTime = performance.now()

            const animate = (currentTime) => {
                const elapsed = currentTime - startTime
                const progress = Math.min(elapsed / duration, 1)
                const ease = 1 - Math.pow(1 - progress, 3)

                step.x = startX + (endX - startX) * ease
                step.y = startY + (endY - startY) * ease

                if (progress < 1) {
                    requestAnimationFrame(animate)
                }
            }

            requestAnimationFrame(animate)
        },
        selectEdge(edge) {
            this.selectedEdge = edge
            this.selectedStep = null
        },

        showEdgeContextMenu(e, edge) {
            this.selectEdge(edge)
            this.edgeContextMenu = {
                visible: true,
                x: e.clientX,
                y: e.clientY,
                edge: edge
            }
        },

        deleteSelectedEdge() {
            if (this.selectedEdge) {
                this.edges = this.edges.filter(e => e.id !== this.selectedEdge.id)
                this.selectedEdge = null
            }
            this.edgeContextMenu.visible = false
        },

        deleteEdge(edge) {
            this.edges = this.edges.filter(e => e.id !== edge.id)
            if (this.selectedEdge?.id === edge.id) {
                this.selectedEdge = null
            }
        },
        getEdgeStart(edge) {
            const from = this.getStepById(edge.fromStepId)
            const to = this.getStepById(edge.toStepId)

            const fromSize = from.isInitial ? this.initialNodeSize : this.nodeSize
            const fromRadius = fromSize / 2

            const angle = Math.atan2(to.y - from.y, to.x - from.x)

            return {
                x: from.x + Math.cos(angle) * (fromRadius - this.arrowOffset),
                y: from.y + Math.sin(angle) * (fromRadius - this.arrowOffset)
            }
        },

        getEdgeEnd(edge) {
            const from = this.getStepById(edge.fromStepId)
            const to = this.getStepById(edge.toStepId)

            const toSize = to.isInitial ? this.initialNodeSize : this.nodeSize
            const toRadius = toSize / 2

            const angle = Math.atan2(to.y - from.y, to.x - from.x)

            return {
                x: to.x - Math.cos(angle) * (toRadius + this.arrowOffset),
                y: to.y - Math.sin(angle) * (toRadius + this.arrowOffset)
            }
        },
        screenToCanvas(screenX, screenY) {
            const rect = this.$refs.canvasContainer.getBoundingClientRect()
            const relativeX = screenX - rect.left
            const relativeY = screenY - rect.top

            return {
                x: (relativeX - this.canvasOffset.x) / this.scale,
                y: (relativeY - this.canvasOffset.y) / this.scale
            }
        },

        handleWheel(e) {
            const delta = e.deltaY > 0 ? 0.9 : 1.1
            const newScale = Math.max(this.minScale, Math.min(this.maxScale, this.scale * delta))

            const rect = this.$refs.canvasContainer.getBoundingClientRect()
            const mouseX = e.clientX - rect.left
            const mouseY = e.clientY - rect.top

            const worldX = (mouseX - this.canvasOffset.x) / this.scale
            const worldY = (mouseY - this.canvasOffset.y) / this.scale

            this.canvasOffset.x = mouseX - worldX * newScale
            this.canvasOffset.y = mouseY - worldY * newScale
            this.scale = newScale
        },

        resetView() {
            this.scale = 1
            this.centerCanvas()
        },

        centerCanvas() {
            const container = this.$refs.canvasContainer
            if (!container || this.steps.length === 0) return

            const rect = container.getBoundingClientRect()

            const xs = this.steps.map(s => s.x)
            const ys = this.steps.map(s => s.y)
            const minX = Math.min(...xs)
            const maxX = Math.max(...xs)
            const minY = Math.min(...ys)
            const maxY = Math.max(...ys)
            const centerX = (minX + maxX) / 2
            const centerY = (minY + maxY) / 2

            this.canvasOffset.x = rect.width / 2 - centerX * this.scale
            this.canvasOffset.y = rect.height / 2 - centerY * this.scale
        },

        handleCanvasMouseDown(e) {
            if (e.target === this.$refs.canvasContainer ||
                e.target.classList.contains('transform-wrapper')) {
                this.isPanning = true
                this.panStart = {
                    x: e.clientX - this.canvasOffset.x,
                    y: e.clientY - this.canvasOffset.y
                }
                this.selectedStep = null
                this.selectedEdge = null
            }
        },

        handleStepMouseDown(e, step) {
            if (e.button !== 0) return

            this.isDraggingStep = true
            this.draggedStep = step
            this.selectedStep = step
            this.selectedEdge = null

            const canvasPos = this.screenToCanvas(e.clientX, e.clientY)
            this.dragStart = {
                x: canvasPos.x - step.x,
                y: canvasPos.y - step.y
            }

            e.stopPropagation()
        },

        handleStepRightMouseDown(e, step) {
            this.isDraggingEdge = true
            this.dragEdgeStart = step
            this.updateDragEdgePosition(e)
            e.stopPropagation()
        },

        handleStepRightMouseUp(e, targetStep) {
            if (this.isDraggingEdge && this.dragEdgeStart && this.dragEdgeStart.id !== targetStep.id) {
                const exists = this.edges.some(edge =>
                    edge.fromStepId === this.dragEdgeStart.id && edge.toStepId === targetStep.id
                )

                if (!exists) {
                    this.createEdge(this.dragEdgeStart.id, targetStep.id)
                }
            }

            this.isDraggingEdge = false
            this.dragEdgeStart = null
            e.stopPropagation()
        },

        handleMouseMove(e) {
            if (this.isPanning) {
                this.canvasOffset.x = e.clientX - this.panStart.x
                this.canvasOffset.y = e.clientY - this.panStart.y
            }

            if (this.isDraggingStep && this.draggedStep) {
                const canvasPos = this.screenToCanvas(e.clientX, e.clientY)
                this.draggedStep.x = canvasPos.x - this.dragStart.x
                this.draggedStep.y = canvasPos.y - this.dragStart.y
            }

            if (this.isDraggingEdge) {
                this.updateDragEdgePosition(e)
            }
        },

        updateDragEdgePosition(e) {
            const canvasPos = this.screenToCanvas(e.clientX, e.clientY)
            this.dragCurrentX = canvasPos.x
            this.dragCurrentY = canvasPos.y
        },

        handleMouseUp() {
            this.isPanning = false
            this.isDraggingStep = false
            this.draggedStep = null

            if (this.isDraggingEdge) {
                this.isDraggingEdge = false
                this.dragEdgeStart = null
            }
        },

        handleDoubleClick(e) {
            const canvasPos = this.screenToCanvas(e.clientX, e.clientY)

            this.contextMenu.canvasPosition = {
                x: canvasPos.x,
                y: canvasPos.y
            }

            this.showContextMenuAt(e.clientX, e.clientY, null)
        },

        showContextMenu(e, step) {
            this.showContextMenuAt(e.clientX, e.clientY, step)
        },

        showContextMenuAt(x, y, step) {
            const adjustedX = Math.min(x, window.innerWidth - 180)
            const adjustedY = Math.min(y, window.innerHeight - 100)

            this.contextMenu = {
                visible: true,
                x: adjustedX,
                y: adjustedY,
                step: step,
                canvasPosition: this.contextMenu.canvasPosition
            }
        },

        hideContextMenus() {
            this.contextMenu.visible = false
            this.contextMenu.canvasPosition = null
            this.edgeContextMenu.visible = false
        },

        openCreateModal() {
            this.modal = {
                visible: true,
                isEdit: false,
                stepId: null,
                form: {
                    name: '',
                    content: '',
                    type: 'THEORY'
                }
            }
            this.hideContextMenus()

            this.$nextTick(() => {
                const input = this.$el.querySelector('.modal-content input')
                if (input) input.focus()
            })
        },

        openEditModal(step) {
            this.modal = {
                visible: true,
                isEdit: true,
                stepId: step.id,
                form: {
                    name: step.name,
                    content: step.content,
                    type: step.type
                }
            }
            this.hideContextMenus()
        },

        closeModal() {
            this.modal.visible = false
        },

        saveStep() {
            if (!this.modal.form.name.trim()) {
                alert('Введите название шага')
                return
            }

            if (this.modal.isEdit) {
                const step = this.steps.find(s => s.id === this.modal.stepId)
                if (step) {
                    step.name = this.modal.form.name
                    step.content = this.modal.form.content
                    step.type = this.modal.form.type
                }
            } else {
                const position = this.contextMenu.canvasPosition || { x: 0, y: 0 }

                const newStep = {
                    id: this.nextStepId++,
                    name: this.modal.form.name,
                    content: this.modal.form.content,
                    type: this.modal.form.type,
                    x: position.x,
                    y: position.y,
                    isInitial: false
                }
                this.steps.push(newStep)
                this.selectedStep = newStep
            }

            this.closeModal()
        },

        deleteStep(step) {
            if (confirm(`Удалить шаг "${step.name}" и все связи с ним?`)) {
                this.steps = this.steps.filter(s => s.id !== step.id)
                this.edges = this.edges.filter(e =>
                    e.fromStepId !== step.id && e.toStepId !== step.id
                )

                if (this.selectedStep?.id === step.id) {
                    this.selectedStep = null
                }
            }
            this.hideContextMenus()
        },

        createEdge(fromId, toId) {
            if (this.wouldCreateCycle(fromId, toId)) {
                alert('Хватит баловаться. В дереве не может быть циклов!')
                return
            }

            const newEdge = {
                id: Date.now(),
                fromStepId: fromId,
                toStepId: toId
            }
            this.edges.push(newEdge)
        },

        wouldCreateCycle(fromId, toId) {
            const visited = new Set()
            const stack = [toId]

            while (stack.length > 0) {
                const current = stack.pop()
                if (current === fromId) return true
                if (visited.has(current)) continue
                visited.add(current)

                this.edges
                    .filter(e => e.fromStepId === current)
                    .forEach(e => stack.push(e.toStepId))
            }

            return false
        },

        getStepById(id) {
            return this.steps.find(s => s.id === id) || { x: 0, y: 0, isInitial: false }
        },

        getStepStyle(step) {
            const size = step.isInitial ? this.initialNodeSize : this.nodeSize
            return {
                left: step.x + 'px',
                top: step.y + 'px',
                width: size + 'px',
                height: size + 'px',
                marginLeft: -size / 2 + 'px',
                marginTop: -size / 2 + 'px',
                zIndex: step.isInitial ? 10 : 1
            }
        },

        getStepTypeLabel(type) {
            const labels = {
                'THEORY': 'Теория',
                'TEST': 'Тест',
                'PRACTICE': 'Практика'
            }
            return labels[type] || type
        },

        handleKeyDown(e) {
            if (e.key === 'Escape') {
                this.closeModal()
                this.hideContextMenus()

                if (this.isDraggingEdge) {
                    this.isDraggingEdge = false
                    this.dragEdgeStart = null
                }
                if (this.isDraggingStep) {
                    this.isDraggingStep = false
                    this.draggedStep = null
                }
            }

            if (e.key === 'Delete' && !this.modal.visible) {
                if (this.selectedEdge) {
                    this.deleteEdge(this.selectedEdge)
                } else if (this.selectedStep) {
                    this.deleteStep(this.selectedStep)
                }
            }

            if ((e.key === 'l' || e.key === 'L') && !this.modal.visible) {
                this.autoLayout()
            }
        },

        async loadCourseData() {
            try {
                const data = await mockDatabase.api.getCourseStructure(this.courseId)
                this.steps = data.steps.map(s => ({ ...s }))
                this.edges = data.edges.map(e => ({ ...e }))

                const maxId = Math.max(...this.steps
                    .map(s => typeof s.id === 'number' ? s.id : 0), 0)
                this.nextStepId = maxId + 1
            } catch (error) {
                this.steps = [
                    {
                        id: 'initial',
                        name: 'Начало',
                        content: 'Начальный шаг курса',
                        type: 'THEORY',
                        x: 0,
                        y: 0,
                        isInitial: true
                    }
                ]
                this.edges = []
            }
        }
    },
    computed: {
        canvasTransform() {
            return {
                transform: `translate(${this.canvasOffset.x}px, ${this.canvasOffset.y}px) scale(${this.scale})`,
                transformOrigin: '0 0'
            }
        }
    },

    async mounted() {
        await this.loadCourseData()
        this.centerCanvas()

        document.addEventListener('click', this.hideContextMenus)
        window.addEventListener('keydown', this.handleKeyDown)
    },

    beforeDestroy() {
        document.removeEventListener('click', this.hideContextMenus)
        window.removeEventListener('keydown', this.handleKeyDown)
    }

}
</script>

<template>
    <div class="course-editor">
        <div class="toolbar">
            <div class="toolbar-info">
                <span>Масштаб: {{ Math.round(scale * 100) }}%</span>
                <span>Шагов: {{ steps.length }}</span>
                <span v-if="selectedEdge" class="selected-info">Выбрана связь</span>
            </div>
            <div class="toolbar-actions">
                <button @click="autoLayout" class="btn-layout" title="Автоматическое выравнивание (L)">
                    Выравнивание 
                </button>
                <button @click="resetView" class="btn-reset">Сбросить вид</button>
            </div>
        </div>

        <div ref="canvasContainer" class="canvas-container" @mousedown.left="handleCanvasMouseDown"
            @mousemove="handleMouseMove" @mouseup="handleMouseUp" @mouseleave="handleMouseUp"
            @wheel.prevent="handleWheel" @dblclick="handleDoubleClick">
            <div class="transform-wrapper" :style="canvasTransform">
                <svg class="edges-layer">
                    <defs>
                        <marker id="arrowhead" markerWidth="12" markerHeight="8" refX="10" refY="4" orient="auto">
                            <polygon points="0 0, 12 4, 0 8" fill="#666" />
                        </marker>
                    </defs>
                    <g v-for="edge in edges" :key="'edge-' + edge.id">
                        <line :x1="getEdgeStart(edge).x" :y1="getEdgeStart(edge).y" :x2="getEdgeEnd(edge).x"
                            :y2="getEdgeEnd(edge).y" :class="{ 'selected-edge': selectedEdge?.id === edge.id }"
                            stroke="#666" stroke-width="2.5" marker-end="url(#arrowhead)"
                            @mousedown.left.stop="selectEdge(edge)"
                            @contextmenu.prevent.stop="showEdgeContextMenu($event, edge)" />
                    </g>
                    <line v-if="isDraggingEdge && dragEdgeStart" :x1="dragEdgeStart.x" :y1="dragEdgeStart.y"
                        :x2="dragCurrentX" :y2="dragCurrentY" stroke="#999" stroke-width="2.5" stroke-dasharray="5,5" />
                </svg>

                <div class="steps-layer">
                    <div v-for="step in steps" :key="step.id" class="step-node" :class="{
                        'initial-step': step.isInitial,
                        'selected': selectedStep?.id === step.id,
                        'edge-source': isDraggingEdge && dragEdgeStart?.id === step.id,
                        'dragging': draggedStep?.id === step.id
                    }" :style="getStepStyle(step)" @mousedown.left="handleStepMouseDown($event, step)"
                        @mousedown.right.prevent="handleStepRightMouseDown($event, step)"
                        @mouseup.right.prevent="handleStepRightMouseUp($event, step)"
                        @contextmenu.prevent="showContextMenu($event, step)">
                        <div class="step-content">
                            <span class="step-name">{{ step.name }}</span>
                            <span class="step-type">{{ getStepTypeLabel(step.type) }}</span>
                        </div>

                        <div class="connection-point top"></div>
                        <div class="connection-point right"></div>
                        <div class="connection-point bottom"></div>
                        <div class="connection-point left"></div>
                    </div>
                </div>
            </div>
        </div>

        <div v-if="contextMenu.visible && !contextMenu.edge" class="context-menu"
            :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
            <div v-if="!contextMenu.step" class="menu-item" @click="openCreateModal">
                Добавить шаг
            </div>
            <template v-else>
                <div class="menu-item" @click="openEditModal(contextMenu.step)">
                    Изменить шаг
                </div>
                <div class="menu-item delete" @click="deleteStep(contextMenu.step)">
                    Удалить шаг
                </div>
            </template>
        </div>

        <div v-if="edgeContextMenu.visible" class="context-menu"
            :style="{ left: edgeContextMenu.x + 'px', top: edgeContextMenu.y + 'px' }">
            <div class="menu-item delete" @click="deleteSelectedEdge">
                Удалить связь
            </div>
        </div>

        <div v-if="modal.visible" class="modal-overlay" @click="closeModal">
            <div class="modal-content" @click.stop>
                <h3>{{ modal.isEdit ? 'Изменить шаг' : 'Создать шаг' }}</h3>

                <div class="form-group">
                    <label>Название шага:</label>
                    <input v-model="modal.form.name" type="text" placeholder="Введите название"
                        @keyup.enter="saveStep" />
                </div>

                <div class="form-group">
                    <label>Описание шага:</label>
                    <textarea v-model="modal.form.content" rows="4" placeholder="Введите описание"></textarea>
                </div>

                <div class="form-group">
                    <label>Тип шага:</label>
                    <select v-model="modal.form.type">
                        <option value="THEORY">Теория</option>
                        <option value="TEST">Тест</option>
                        <option value="PRACTICE">Практика</option>
                    </select>
                </div>

                <div class="modal-actions">
                    <button @click="closeModal" class="btn-cancel">Отмена</button>
                    <button @click="saveStep" class="btn-confirm">
                        {{ modal.isEdit ? 'Сохранить' : 'Создать' }}
                    </button>
                </div>
            </div>
        </div>

        <div class="help-panel">
            <h4>Управление:</h4>
            <ul>
                <li><b>ЛКМ даблклик по холсту</b> — создание шага</li>
                <li><b>ЛКМ + drag</b> — перемещение шага</li>
                <li><b>ПКМ + drag</b> — создание связи</li>
                <li><b>ЛКМ по связи</b> — выбрать связь</li>
                <li><b>ПКМ по связи</b> — удалить связь</li>
                <li><b>Delete</b> — удалить выбранное</li>
                <li><b>L</b> — авто-выравнивание</li>
            </ul>
        </div>
    </div>
</template>

<style scoped>
.course-editor {
    width: 100%;
    height: 100vh;
    display: flex;
    flex-direction: column;
    background: #f0f2f5;
    overflow: hidden;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.toolbar {
    height: 60px;
    background: white;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    z-index: 100;
}

.toolbar h2 {
    margin: 0;
    font-size: 18px;
    color: #1a1a1a;
    font-weight: 600;
}

.toolbar-info {
    display: flex;
    align-items: center;
    gap: 20px;
    color: #666;
    font-size: 14px;
}

.selected-info {
    color: #9C27B0;
    font-weight: 600;
}

.toolbar-actions {
    display: flex;
    gap: 10px;
}

.btn-layout {
    padding: 8px 16px;
    background: #9C27B0;
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s;
}

.btn-layout:hover {
    background: #7B1FA2;
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(156, 39, 176, 0.3);
}

.btn-icon {
    font-size: 16px;
}

.btn-reset {
    padding: 8px 16px;
    border: 1px solid #ddd;
    background: white;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    color: #666;
    font-weight: 500;
}

.btn-reset:hover {
    background: #f5f5f5;
}

.canvas-container {
    flex: 1;
    position: relative;
    overflow: hidden;
    cursor: grab;
    background-color: #f8f9fa;
    background-image:
        radial-gradient(circle, #d0d0d0 1.5px, transparent 1.5px);
    background-size: 24px 24px;
}

.canvas-container:active {
    cursor: grabbing;
}

.transform-wrapper {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    transform-origin: 0 0;
    will-change: transform;
}

.edges-layer {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    pointer-events: none;
    overflow: visible;
}

.edges-layer line {
    pointer-events: stroke;
    cursor: pointer;
}

.edges-layer line:hover {
    stroke: #2196F3;
    stroke-width: 4;
}

.edges-layer line.selected-edge {
    stroke: #9C27B0;
    stroke-width: 4;
    filter: drop-shadow(0 0 3px rgba(156, 39, 176, 0.5));
}

.steps-layer {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
}

.step-node {
    position: absolute;
    border-radius: 50%;
    background: white;
    border: 3px solid #2196F3;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: grab;
    box-shadow: 0 3px 12px rgba(0, 0, 0, 0.12);
    transition: box-shadow 0.2s, border-color 0.2s;
    user-select: none;
}

.step-node:hover {
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.18);
    border-color: #1976D2;
}

.step-node:active {
    cursor: grabbing;
}

.step-node.dragging {
    opacity: 0.9;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25);
    z-index: 1000 !important;
}

.step-node.initial-step {
    border-color: #FF9800;
    background: linear-gradient(135deg, #FFF3E0 0%, #FFE0B2 100%);
}

.step-node.selected {
    border-color: #9C27B0;
    box-shadow: 0 0 0 4px rgba(156, 39, 176, 0.25);
}

.step-node.edge-source {
    border-color: #4CAF50;
    box-shadow: 0 0 0 4px rgba(76, 175, 80, 0.3);
}

.step-content {
    text-align: center;
    padding: 12px;
    pointer-events: none;
}

.step-name {
    display: block;
    font-weight: 600;
    font-size: 13px;
    color: #1a1a1a;
    margin-bottom: 4px;
    max-width: 90px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    line-height: 1.3;
}

.step-type {
    display: block;
    font-size: 10px;
    color: #666;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: 500;
}

.connection-point {
    position: absolute;
    width: 12px;
    height: 12px;
    background: #2196F3;
    border-radius: 50%;
    opacity: 0;
    transition: opacity 0.2s;
    border: 2px solid white;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.step-node:hover .connection-point {
    opacity: 0.6;
}

.connection-point.top {
    top: -6px;
    left: 50%;
    transform: translateX(-50%);
}

.connection-point.right {
    right: -6px;
    top: 50%;
    transform: translateY(-50%);
}

.connection-point.bottom {
    bottom: -6px;
    left: 50%;
    transform: translateX(-50%);
}

.connection-point.left {
    left: -6px;
    top: 50%;
    transform: translateY(-50%);
}

.context-menu {
    position: fixed;
    background: white;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    z-index: 1000;
    min-width: 180px;
    padding: 6px 0;
    animation: menuAppear 0.15s ease;
}

@keyframes menuAppear {
    from {
        opacity: 0;
        transform: scale(0.95);
    }

    to {
        opacity: 1;
        transform: scale(1);
    }
}

.menu-item {
    padding: 10px 16px;
    cursor: pointer;
    font-size: 14px;
    color: #333;
    display: flex;
    align-items: center;
    gap: 10px;
    transition: background 0.15s;
}

.menu-item:hover {
    background: #f5f7fa;
}

.menu-item.delete {
    color: #d32f2f;
}

.menu-item.delete:hover {
    background: #ffebee;
}

.icon {
    font-size: 16px;
    width: 20px;
    text-align: center;
}

.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 2000;
    animation: fadeIn 0.2s ease;
}

.modal-content {
    background: white;
    padding: 28px;
    border-radius: 12px;
    width: 420px;
    max-width: 90%;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
    animation: slideUp 0.3s ease;
}

.modal-content h3 {
    margin: 0 0 24px 0;
    color: #1a1a1a;
    font-size: 20px;
    font-weight: 600;
}

.form-group {
    margin-bottom: 18px;
}

.form-group label {
    display: block;
    margin-bottom: 6px;
    color: #555;
    font-size: 13px;
    font-weight: 500;
}

.form-group input,
.form-group textarea,
.form-group select {
    width: 100%;
    padding: 10px 14px;
    border: 1px solid #e0e0e0;
    border-radius: 6px;
    font-size: 14px;
    box-sizing: border-box;
    transition: border-color 0.2s, box-shadow 0.2s;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
    outline: none;
    border-color: #2196F3;
    box-shadow: 0 0 0 3px rgba(33, 150, 243, 0.1);
}

.modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 24px;
}

.btn-cancel,
.btn-confirm {
    padding: 10px 18px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
}

.btn-cancel {
    border: 1px solid #e0e0e0;
    background: white;
    color: #666;
}

.btn-confirm {
    background: #2196F3;
    color: white;
    border: none;
}

.help-panel {
    position: fixed;
    bottom: 20px;
    right: 20px;
    background: white;
    padding: 16px 20px;
    border-radius: 8px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
    font-size: 12px;
    color: #666;
    max-width: 260px;
    z-index: 50;
}

.help-panel h4 {
    margin: 0 0 10px 0;
    color: #333;
    font-size: 13px;
}

.help-panel ul {
    margin: 0;
    padding-left: 16px;
    line-height: 1.8;
}

.help-panel li b {
    color: #2196F3;
    font-weight: 600;
}
</style>