<script>
import { courseApi } from '../../api/course_api.js'
import AIIcon from './AIIcon.vue'
import JsonEditor from './JsonEditor.vue'
import StepEditor from './StepEditor.vue'

const STORAGE_KEY = 'course_editor_data'
const STORAGE_VERSION = '1.0'
const HISTORY_LIMIT = 50

NProgress.configure({ easing: 'linear', speed: 200, trickleSpeed: 200, showSpinner: false });

export default {
    name: 'CourseEditor',
    components: {
        JsonEditor,
        AIIcon,
        StepEditor,
    },
    data() {
        return {
            isRequesting: false,

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
                canvasPosition: { x: 0, y: 0 }
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
                    description: '',
                    type: 'THEORY'
                }
            },

            nextStepId: 10,
            nodeSize: 100,
            initialNodeSize: 120,
            arrowOffset: 15,

            jsonText: '',
            lastAppliedJson: '',
            jsonError: '',

            aiMessage: {
                content: '',
                visible: false,
                timeout: null,
                x: 20,
                y: 200
            },

            autoSaveEnabled: true,
            lastSavedTime: null,
            saveTimeout: null,

            history: [],
            historyIndex: -1,
            isUndoing: false,

            stepEditor: {
                visible: false,
                step: null,
                originPosition: { x: 0, y: 0, width: 100, height: 100 },
                closing: false
            }
        }
    },

    watch: {
        steps: {
            deep: true,
            handler() {
                this.updateJsonFromData()
                if (this.autoSaveEnabled) {
                    this.debouncedSave()
                }
            }
        },

        edges: {
            deep: true,
            handler() {
                this.updateJsonFromData()
                if (this.autoSaveEnabled) {
                    this.debouncedSave()
                }
            }
        },

        scale() {
            if (this.autoSaveEnabled) {
                this.debouncedSave()
            }
        },

        canvasOffset: {
            deep: true,
            handler() {
                if (this.autoSaveEnabled) {
                    this.debouncedSave()
                }
            }
        }
    },

    methods: {
        handleKeyDown(e) {
            let key = e.key.toLowerCase()
            if (this.stepEditor.visible) {
                if (key === 'escape') {
                    this.closeStepEditor()
                }
                return
            }
            if (e.ctrlKey && key === 's' && !this.modal.visible) {
                e.preventDefault()
                this.applyJson()
                return
            }

            const isJsonEditorFocused = this.$refs.jsonEditorRef?.$el?.contains(document.activeElement) ||
                document.activeElement?.classList?.contains('json-input') ||
                document.activeElement?.closest('.json-editor-container')

            if (isJsonEditorFocused) {
                return
            }

            if (e.ctrlKey && key === 'z' && !e.shiftKey && !this.modal.visible) {
                e.preventDefault()
                this.undo()
                return
            }

            if ((e.ctrlKey && key === 'y') || (e.ctrlKey && e.shiftKey && key === 'z')) {
                e.preventDefault()
                this.redo()
                return
            }

            if (key === 'escape') {
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
                return
            }

            if (key === 'delete' && !this.modal.visible) {
                if (this.selectedEdge) {
                    this.deleteEdge(this.selectedEdge)
                } else if (this.selectedStep) {
                    this.deleteStep(this.selectedStep)
                }
                return
            }

            if ((e.ctrlKey && e.altKey && key === 'l') && !this.modal.visible) {
                this.autoLayout()
                return
            }
        },
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
            const nodeWidth = 265
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

            this.saveHistory()
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
            this.saveHistory()
        },

        deleteEdge(edge) {
            this.edges = this.edges.filter(e => e.id !== edge.id)
            if (this.selectedEdge?.id === edge.id) {
                this.selectedEdge = null
            }
            this.saveHistory()
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

            this.dragStartPos = { x: step.x, y: step.y }
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
            if (this.isDraggingStep && this.draggedStep && this.dragStartPos) {
                const step = this.draggedStep
                if (step.x !== this.dragStartPos.x || step.y !== this.dragStartPos.y) {
                    this.saveHistory()
                }
            }

            this.isPanning = false
            this.isDraggingStep = false
            this.draggedStep = null
            this.dragStartPos = null
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
            this.edgeContextMenu.visible = false
        },

        openCreateModal() {
            this.modal = {
                visible: true,
                isEdit: false,
                stepId: null,
                form: {
                    name: '',
                    description: '',
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
                    description: step.description,
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
                    step.description = this.modal.form.description
                    step.type = this.modal.form.type
                }
            } else {
                const newStep = {
                    id: this.nextStepId++,
                    name: this.modal.form.name,
                    description: this.modal.form.description,
                    content: '',
                    type: this.modal.form.type,
                    x: this.contextMenu.canvasPosition.x || 0,
                    y: this.contextMenu.canvasPosition.y || 0,
                    isInitial: false
                }
                this.steps.push(newStep)
                this.selectedStep = newStep

            }
            this.saveHistory()
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
                this.saveHistory()

            }
            this.hideContextMenus()
        },

        createEdge(fromId, toId) {
            if (this.wouldCreateCycle(fromId, toId)) {
                alert('Хватит баловаться. Можно создавать только DAG структуры (без циклов).')
                return
            }

            const newEdge = {
                id: Date.now(),
                fromStepId: fromId,
                toStepId: toId
            }
            this.edges.push(newEdge)
            this.saveHistory()
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

        async loadCourseData() {
            this.isRequesting = true
            try {
                const response = await courseApi.getStructure(this.courseId)
                this.steps = response.data.steps || []
                this.edges = response.data.edges || []

                const maxId = Math.max(...this.steps
                    .map(s => typeof s.id === 'number' ? s.id : 0), 0)
                this.nextStepId = maxId + 1
            } catch (error) {
                console.error('Failed to load course:', error)
                if (!this.loadFromStorage()) {
                    this.steps = [
                        {
                            id: 0,
                            name: 'Введение',
                            description: 'Начальный шаг курса',
                            type: 'THEORY',
                            x: 0,
                            y: 0,
                            isInitial: true
                        }
                    ]
                    this.edges = []
                }
            } finally {
                this.isRequesting = false
            }
        },

        async saveCourse() {
            try {
                this.isRequesting = true
                NProgress.start()
                const payload = {
                    steps: this.steps.map(s => ({ ...s})),
                    edges: this.edges.map(e => ({ ...e}))
                }

                await courseApi.saveStructure(this.courseId, payload)
                alert('Сохранено!')
            } catch (error) {
                alert('Ошибка сохранения: ' + error.message)
            }
            finally {
                NProgress.done()
                this.isRequesting = false
            }
        },

        updateJsonFromData() {
            const data = {
                steps: this.steps.map(s => ({
                    id: s.id,
                    title: s.name,
                    description: s.description,
                    content: s.content,
                    type: s.type,
                    x: Math.round(s.x),
                    y: Math.round(s.y)
                })),
                edges: this.groupEdgesForExport()
            }
            this.jsonText = JSON.stringify(data, null, 2)
            this.lastAppliedJson = this.jsonText
            this.jsonError = ''
        },

        groupEdgesForExport() {
            const grouped = {}
            this.edges.forEach(edge => {
                if (!grouped[edge.fromStepId]) {
                    grouped[edge.fromStepId] = []
                }
                grouped[edge.fromStepId].push(edge.toStepId)
            })

            return Object.entries(grouped).map(([from, to]) => ({
                from: parseInt(from) || from,
                to
            }))
        },

        applyJson() {
            try {
                this.jsonError = ''
                const data = JSON.parse(this.jsonText)

                if (!data.steps || !Array.isArray(data.steps)) {
                    throw new Error('Отсутствует поле "steps" или оно не является массивом')
                }
                if (!data.edges || !Array.isArray(data.edges)) {
                    throw new Error('Отсутствует поле "edges" или оно не является массивом')
                }

                let maxId = 0

                const newSteps = data.steps.map((step, index) => {
                    if (step.id === undefined) {
                        throw new Error(`Шаг ${index}: отсутствует поле "id"`)
                    }
                    if (!step.title && !step.name) {
                        throw new Error(`Шаг ${step.id}: отсутствует поле "title" или "name"`)
                    }
                    if (!step.type) {
                        throw new Error(`Шаг ${step.id}: отсутствует поле "type"`)
                    }

                    const processedStep = {
                        id: parseInt(step.id),
                        name: step.title || step.name,
                        description: step.description || '',
                        type: step.type.toUpperCase(),
                        x: step.x !== undefined ? step.x : null,
                        y: step.y !== undefined ? step.y : null,
                        isInitial: index === 0
                    }
                    if (isNaN(processedStep.id)) {
                        throw new Error(`${index}-й шаг: поле "id" должно быть числом`)
                    }
                    const validTypes = ['THEORY', 'TEST', 'PRACTICE']
                    if (!validTypes.includes(processedStep.type)) {
                        throw new Error(`Шаг ${step.id}: неверный тип "${step.type}". Допустимые: ${validTypes.join(', ')}`)
                    }
                    maxId = Math.max(maxId, processedStep.id)
                    return processedStep
                })



                const ids = newSteps.map(s => s.id)
                const duplicates = ids.filter((id, index) => ids.indexOf(id) !== index)
                if (duplicates.length > 0) {
                    throw new Error(`Дублирующиеся ID шагов: ${duplicates.join(', ')}`)
                }

                const newEdges = []
                data.edges.forEach((edgeGroup, index) => {
                    if (edgeGroup.from === undefined) {
                        throw new Error(`Связь ${index}: отсутствует поле "from"`)
                    }
                    if (!edgeGroup.to || !Array.isArray(edgeGroup.to)) {
                        throw new Error(`Связь ${index}: поле "to" должно быть массивом`)
                    }

                    edgeGroup.to.forEach((toId, index) => {
                        if (index == edgeGroup.to.indexOf(toId)) {
                            if (!newSteps.find(s => s.id == edgeGroup.from)) {
                                throw new Error(`Связь: шаг с id="${edgeGroup.from}" не найден`)
                            }
                            if (!newSteps.find(s => s.id == toId)) {
                                throw new Error(`Связь: шаг с id="${toId}" не найден`)
                            }

                            newEdges.push({
                                id: Date.now() + Math.random(),
                                fromStepId: edgeGroup.from,
                                toStepId: toId
                            })
                        }
                    })

                })

                if (this.hasCycle(newSteps, newEdges)) {
                    throw new Error('Обнаружена циклическая зависимость в структуре')
                }

                const hasMissingCoords = newSteps.some(s => s.x === null || s.y === null)
                if (hasMissingCoords) {
                    if (confirm('У некоторых шагов отсутствуют координаты. Сгенерировать их автоматически? (применится выравнивание)')) {
                        this.$nextTick(() => {
                            this.autoLayout()
                        })
                    }
                    else if (confirm('Заполнить отсутствующие координаты нулями?')) {
                        newSteps.forEach(s => {
                            if (s.x === null) s.x = 0
                            if (s.y === null) s.y = 0
                        })
                    }
                    else {
                        alert('Отмена применения структкры.')
                        return
                    }

                }
                this.steps = newSteps
                this.edges = newEdges

                this.nextStepId = maxId + 1

                this.lastAppliedJson = this.jsonText
                this.selectedStep = null
                this.selectedEdge = null

                this.saveHistory()
                alert('✅ Структура применена!')


            } catch (error) {
                this.jsonError = error.message
                alert('❌ Ошибка: ' + error.message)
                console.error('JSON parse error:', error)
            }
        },

        hasCycle(steps, edges) {
            const adj = {}
            steps.forEach(s => adj[s.id] = [])
            edges.forEach(e => {
                if (adj[e.fromStepId]) adj[e.fromStepId].push(e.toStepId)
            })

            const visited = new Set()
            const recStack = new Set()

            const dfs = (node) => {
                visited.add(node)
                recStack.add(node)

                for (const neighbor of (adj[node] || [])) {
                    if (!visited.has(neighbor)) {
                        if (dfs(neighbor)) return true
                    } else if (recStack.has(neighbor)) {
                        return true
                    }
                }

                recStack.delete(node)
                return false
            }

            for (const step of steps) {
                if (!visited.has(step.id)) {
                    if (dfs(step.id)) return true
                }
            }
            return false
        },

        formatJson() {
            try {
                const parsed = JSON.parse(this.jsonText)
                this.jsonText = JSON.stringify(parsed, null, 2)
                this.jsonError = ''
            } catch (error) {
                this.jsonError = 'Невозможно форматировать: ' + error.message
            }
        },

        showAiMessage(data) {
            this.aiMessage.content = data.angerLevel > 0.3 ? "<div>Хватит!</div>"
                : "<div>Привет! Я ИИ помощник.</div><div>Закинь мне материал —</div><div>Составлю структуру</div>"
            this.aiMessage.visible = true


            if (this.aiMessage.timeout) {
                clearTimeout(this.aiMessage.timeout)
                this.aiMessage.timeout = null
            }
            this.aiMessage.timeout = setTimeout(() => {
                if (this.aiMessage.visible) {
                    this.closeAiMessage()
                }
            }, 5000)
        },

        closeAiMessage() {
            this.aiMessage.visible = false
        },

        saveToStorage() {
            try {
                const data = {
                    version: STORAGE_VERSION,
                    timestamp: Date.now(),
                    courseId: this.courseId,
                    steps: this.steps.map(s => ({
                        id: parseInt(s.id),
                        name: s.name,
                        description: s.description,
                        content: s.content,
                        type: s.type,
                        x: s.x,
                        y: s.y,
                        isInitial: s.isInitial
                    })),
                    edges: this.edges.map(e => ({
                        id: e.id,
                        fromStepId: parseInt(e.fromStepId),
                        toStepId: parseInt(e.toStepId)
                    })),
                    view: {
                        scale: this.scale,
                        offsetX: this.canvasOffset.x,
                        offsetY: this.canvasOffset.y
                    }
                }

                localStorage.setItem(STORAGE_KEY, JSON.stringify(data))
                this.lastSavedTime = new Date().toLocaleTimeString()

                console.log('Сохранено в', this.lastSavedTime)
            } catch (error) {
                console.error('Ошибка сохранения в localStorage:', error)
            }
        },

        loadFromStorage() {
            try {
                const saved = localStorage.getItem(STORAGE_KEY)
                if (!saved) return false

                const data = JSON.parse(saved)

                if (data.version !== STORAGE_VERSION) {
                    console.warn(`localStorage: несоответствие версий (${data.version} и настоящей версии ${STORAGE_VERSION})`)
                    return false
                }

                if (data.courseId && data.courseId !== this.courseId) {
                    return false
                }

                if (data.steps && data.steps.length > 0) {
                    this.steps = data.steps
                    this.edges = data.edges || []

                    if (data.view) {
                        this.scale = data.view.scale || 1
                        this.canvasOffset.x = data.view.offsetX || 0
                        this.canvasOffset.y = data.view.offsetY || 0
                    }

                    const maxId = Math.max(...this.steps
                        .map(s => parseInt(s.id)), 0)
                    this.nextStepId = maxId + 1

                    this.lastSavedTime = new Date(data.timestamp).toLocaleString()
                    console.log('Курс загружен из localStorage в', this.lastSavedTime)
                    return true
                }
            } catch (error) {
                console.error('Ошибка загрузки из localStorage:', error)
            }
            return false
        },

        clearStorage() {
            if (confirm('Очистить сохранённые данные?')) {
                localStorage.removeItem(STORAGE_KEY)
                this.lastSavedTime = null
                alert('Сохранённые данные очищены')
            }
        },

        manualSave() {
            this.saveToStorage()
            alert('Курс сохранён в памяти (для полного сохранения нажмите "Сохранить" справа сверху)')
        },

        saveHistory() {
            if (this.isUndoing) return

            if (this.historyIndex < this.history.length - 1) {
                this.history = this.history.slice(0, this.historyIndex + 1)
            }
            const state = {
                timestamp: Date.now(),
                steps: this.steps.map(s => ({
                    id: parseInt(s.id),
                    name: s.name,
                    content: s.content,
                    type: s.type,
                    x: s.x,
                    y: s.y,
                    isInitial: s.isInitial
                })),
                edges: this.edges.map(e => ({
                    id: e.id,
                    fromStepId: parseInt(e.fromStepId),
                    toStepId: parseInt(e.toStepId)
                })),
                nextStepId: this.nextStepId
            }

            this.history.push(state)

            if (this.history.length > HISTORY_LIMIT) {
                this.history.shift()
            } else {
                this.historyIndex++
            }
        },

        undo() {
            if (this.historyIndex <= 0) return

            this.isUndoing = true
            this.historyIndex--

            const state = this.history[this.historyIndex]

            this.steps = [...state.steps]
            this.edges = [...state.edges]
            this.nextStepId = state.nextStepId

            this.selectedStep = null
            this.selectedEdge = null

            this.updateJsonFromData()

            this.$nextTick(() => {
                this.isUndoing = false
            })
        },

        redo() {
            if (this.historyIndex >= this.history.length - 1) return

            this.historyIndex++

            const state = this.history[this.historyIndex]

            this.steps = [...state.steps]
            this.edges = [...state.edges]
            this.nextStepId = state.nextStepId

            this.selectedStep = null
            this.selectedEdge = null

            this.updateJsonFromData()
        },

        openStepEditor(step) {
            if (this.stepEditor.visible) return

            const stepElement = document.querySelector(`[data-step-id="${step.id}"]`)
            if (stepElement) {
                const rect = stepElement.getBoundingClientRect()
                this.stepEditor.originPosition = {
                    x: rect.left,
                    y: rect.top,
                    width: rect.width,
                    height: rect.height
                }
            }

            this.stepEditor.step = step
            this.stepEditor.visible = true
        },

        closeStepEditor() {
            if (!this.stepEditor.visible) return

            this.stepEditor.closing = true

            setTimeout(() => {
                this.stepEditor.visible = false
                this.stepEditor.closing = false
                this.stepEditor.step = null
            }, 350)
        },

        saveStepContent({ id, content }) {
            const step = this.steps.find(s => s.id === id)
            if (step) {
                step.content = content
                console.log(`Сохранён контент для шага ${id}:`, content)
                this.saveHistory()
            }
        }

    },
    computed: {
        canvasTransform() {
            return {
                transform: `translate(${this.canvasOffset.x}px, ${this.canvasOffset.y}px) scale(${this.scale})`,
                transformOrigin: '0 0'
            }
        },

        hasJsonChanges() {
            return this.jsonText !== this.lastAppliedJson
        },

        courseData() {
            const edges = {};
            for (const { fromStepId, toStepId } of this.edges) {
                (edges[fromStepId] ??= { from: fromStepId, to: [] }).to.push(toStepId);
            }
            return {
                steps: this.steps.map(s => ({
                    id: s.id,
                    title: s.name,
                    description: s.description,
                    content: s.content,
                    type: s.type,
                    x: s.x,
                    y: s.y,
                    isInitial: s.isInitial
                })),
                edges: Object.values(map)
            }
        },
        canUndo() {
            return this.historyIndex > 0
        },

        canRedo() {
            return this.historyIndex < this.history.length - 1
        }
    },

    async mounted() {
        const loaded = this.loadFromStorage()

        if (!loaded) {
            await this.loadCourseData()
        }

        this.updateJsonFromData()
        if (!loaded) {
            this.centerCanvas()
        }

        this.saveHistory()

        document.addEventListener('click', this.hideContextMenus)
        window.addEventListener('keydown', this.handleKeyDown)
        window.addEventListener('beforeunload', () => {
            this.saveToStorage()
        })


    },

    created() {
        this.debouncedSave = () => {
            if (this.saveTimeout) {
                clearTimeout(this.saveTimeout)
            }
            this.saveTimeout = setTimeout(() => {
                this.saveToStorage()
            }, 3000)
        }
    },

    beforeDestroy() {
        if (this.saveTimeout) {
            clearTimeout(this.saveTimeout)
        }
        confirm("Запомнить текущее состояние?")
        this.saveToStorage()
        document.removeEventListener('click', this.hideContextMenus)
        window.removeEventListener('keydown', this.handleKeyDown)
    }

}
</script>

<template>
    <div class="course-editor">
        <div class="toolbar">
            <h2>Редактор курса: {{ courseName }}</h2>
            <div class="toolbar-info">
                <span>Масштаб: {{ Math.round(scale * 100) }}%</span>
                <span>Шагов: {{ steps.length }}</span>
                <span class="history-indicator" :class="{ 'can-undo': canUndo, 'can-redo': canRedo }">
                    <button @click="undo" :disabled="!canUndo" title="Отменить (Ctrl+Z)">↩️</button>
                    <span class="history-count">{{ historyIndex + 1 }}/{{ history.length }}</span>
                    <button @click="redo" :disabled="!canRedo" title="Повторить (Ctrl+Y)">↪️</button>
                </span>
                <span v-if="selectedEdge" class="selected-info">Выбрана связь</span>
            </div>
            <div class="toolbar-actions">
                <label class="toggle-switch" title="Автосохранение">
                    <input type="checkbox" v-model="autoSaveEnabled">
                    <span class="toggle-slider">
                    </span>
                    <span class="toggle-label">Автосохранение</span>
                </label>

                <button @click="manualSave" class="icon-btn" title="Сохранить в память">
                    💾
                </button>

                <button @click="clearStorage" class="icon-btn icon-danger" title="Очистить сохранённые данные">
                    🗑️
                </button>

                <span v-if="lastSavedTime" class="save-time">
                    <p>{{ lastSavedTime }}</p>
                </span>
                <button @click="autoLayout" class="btn-layout"
                    title="Автоматическое выравнивание (Ctrl+Alt+L)">Выравнивание</button>
                <button @click="resetView" class="btn-reset">Сбросить вид</button>
                <button :disabled="isRequesting" @click="saveCourse" class="btn-save">Сохранить</button>
            </div>
        </div>

        <div class="main-content">
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
                            :x2="dragCurrentX" :y2="dragCurrentY" stroke="#999" stroke-width="2.5"
                            stroke-dasharray="5,5" />
                    </svg>

                    <div class="steps-layer">
                        <div v-for="step in steps" :key="step.id" :data-step-id="step.id" class="step-node" :class="[
                            'type-' + (step.type || 'theory').toLowerCase(),
                            {
                                'initial-step': step.isInitial,
                                'selected': selectedStep?.id === step.id,
                                'edge-source': isDraggingEdge && dragEdgeStart?.id === step.id,
                                'dragging': draggedStep?.id === step.id
                            }
                        ]" :style="getStepStyle(step)" @mousedown.left="handleStepMouseDown($event, step)"
                            @mousedown.right.prevent="handleStepRightMouseDown($event, step)"
                            @mouseup.right.prevent="handleStepRightMouseUp($event, step)"
                            @contextmenu.prevent="showContextMenu($event, step)">
                            <div class="step-content">
                                <span class="step-name" title="poegkpoeg">{{ step.name }}</span>
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

            <div class="json-panel">
                <div class="json-header">
                    <h3>JSON</h3>
                    <div class="json-actions">
                        <button @click="applyJson" class="btn-apply" :class="{ 'has-changes': hasJsonChanges }">
                            Применить
                        </button>
                        <button @click="formatJson" class="btn-format">Форматировать</button>
                    </div>
                </div>

                <JsonEditor ref="jsonEditorRef" v-model="jsonText" @apply="applyJson" class="json-editor-wrapper" />

                <div v-if="jsonError" class="json-error">{{ jsonError }}</div>
            </div>
        </div>

        <div v-if="contextMenu.visible && !contextMenu.edge" class="context-menu"
            :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
            <div v-if="!contextMenu.step" class="menu-item" @click="openCreateModal">
                <span class="icon">➕</span> Добавить шаг
            </div>
            <template v-else>
                <div class="menu-item" @click="openEditModal(contextMenu.step)">
                    <span class="icon">✏️</span> Изменить шаг
                </div>
                <div class="menu-item" @click="openStepEditor(contextMenu.step)">
                    <span class="icon">📝</span> Редактировать контент
                </div>
                <div class="menu-item delete" @click="deleteStep(contextMenu.step)">
                    <span class="icon">🗑️</span> Удалить шаг
                </div>
            </template>
        </div>

        <div v-if="edgeContextMenu.visible" class="context-menu"
            :style="{ left: edgeContextMenu.x + 'px', top: edgeContextMenu.y + 'px' }">
            <div class="menu-item delete" @click="deleteSelectedEdge">
                <span class="icon">✂️</span> Удалить связь
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
                    <textarea v-model="modal.form.description" rows="4" placeholder="Введите описание"></textarea>
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


        <div class="ai-panel">
            <AIIcon class="ai-assistant" ref="aiRef" :is-active="isAiActive" @interaction="showAiMessage" />
        </div>
        <div v-if="aiMessage.visible" class="ai-message-popup"
            :style="{ left: aiMessage.x + 'px', bottom: aiMessage.y + 'px' }" @click.stop>
            <div class="ai-message-content">
                <div class="ai-message-header">
                    <span class="ai-title">AI Помощник</span>
                    <button class="ai-close" @click="closeAiMessage">×</button>
                </div>
                <div class="ai-message-text" v-html="aiMessage.content"></div>
                <div class="ai-message-actions">
                    <button class="ai-btn-primary" @click="openAiUpload">Загрузить материал</button>
                </div>
            </div>
            <div class="ai-message-tail"></div>
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
                <li><b>Ctrl+Alt+L</b> — авто-выравнивание</li>
                <li><b>Ctrl+S</b> — применить JSON</li>
            </ul>
        </div>
    </div>
    <StepEditor :visible="stepEditor.visible" :step-id="stepEditor.step?.id"
        :step-name="stepEditor.step?.name" :step-type="stepEditor.step?.type" :content="stepEditor.step?.content"
        :origin-position="stepEditor.originPosition" @close="stepEditor.visible = false" @save="saveStepContent" />
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
    flex-shrink: 0;
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

.btn-save {
    padding: 8px 20px;
    background: #4CAF50;
    color: white;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s;
}


.btn-save:disabled, .btn-save:hover:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    pointer-events: none;
}
.btn-save:hover {
    background: #45a049;
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
}

.main-content {
    flex: 1;
    display: flex;
    overflow: hidden;
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
    border: 3px solid;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: grab;
    box-shadow: 0 3px 12px rgba(0, 0, 0, 0.12);
    user-select: none;
}

.step-node.type-theory {
    border-color: #2196F3;
    background: linear-gradient(135deg, #E3F2FD 0%, #BBDEFB 100%);
}

.step-node.type-theory:hover {
    border-color: #1976D2;
    box-shadow: 0 6px 20px rgba(33, 150, 243, 0.25);
}

.step-node.type-theory .connection-point {
    background: #2196F3;
}

.step-node.type-theory .step-type {
    color: #1976D2;
}

.step-node.type-practice {
    border-color: #4CAF50;
    background: linear-gradient(135deg, #E8F5E9 0%, #C8E6C9 100%);
}

.step-node.type-practice:hover {
    border-color: #388E3C;
    box-shadow: 0 6px 20px rgba(76, 175, 80, 0.25);
}

.step-node.type-practice .connection-point {
    background: #4CAF50;
}

.step-node.type-practice .step-type {
    color: #388E3C;
}

.step-node.type-test {
    border-color: #E91E63;
    background: linear-gradient(135deg, #FCE4EC 0%, #F8BBD9 100%);
}

.step-node.type-test:hover {
    border-color: #C2185B;
    box-shadow: 0 6px 20px rgba(233, 30, 99, 0.25);
}

.step-node.type-test .connection-point {
    background: #E91E63;
}

.step-node.type-test .step-type {
    color: #C2185B;
}

.step-node.initial-step {
    border-color: #FF9800 !important;
    background: linear-gradient(135deg, #FFF3E0 0%, #FFE0B2 100%) !important;
}

.step-node.initial-step .connection-point {
    background: #FF9800 !important;
}

.step-node.initial-step .step-type {
    color: #E65100 !important;
}

.step-node.initial-step:hover {
    box-shadow: 0 6px 20px rgba(255, 152, 0, 0.25) !important;
}

.step-node.selected {
    box-shadow: 0 0 0 4px rgba(156, 39, 176, 0.3) !important;
    border-color: #9C27B0 !important;
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

.json-panel {
    width: 400px;
    background: white;
    border-left: 1px solid #e0e0e0;
    display: flex;
    flex-direction: column;
    box-shadow: -2px 0 8px rgba(0, 0, 0, 0.05);
}

.json-header {
    padding: 15px 20px;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background: #fafafa;
}

.json-header h3 {
    margin: 0;
    font-size: 14px;
    color: #333;
    font-weight: 600;
}

.json-actions {
    display: flex;
    gap: 8px;
}

.btn-apply {
    padding: 6px 12px;
    background: #2196F3;
    color: white;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
    font-weight: 500;
    transition: all 0.2s;
}

.btn-apply.has-changes {
    background: #FF9800;
    animation: pulse 2s infinite;
}

@keyframes pulse {

    0%,
    100% {
        opacity: 1;
    }

    50% {
        opacity: 0.8;
    }
}

.btn-apply:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 6px rgba(33, 150, 243, 0.3);
}

.btn-format {
    padding: 6px 12px;
    background: white;
    color: #666;
    border: 1px solid #ddd;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
    font-weight: 500;
}

.btn-format:hover {
    background: #f5f5f5;
}

.json-editor {
    flex: 1;
    width: 100%;
    padding: 15px;
    border: none;
    font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
    font-size: 12px;
    line-height: 1.5;
    resize: none;
    outline: none;
    background: #fafafa;
    color: #333;
}

.json-editor:focus {
    background: white;
}

.json-error {
    padding: 10px 15px;
    background: #ffebee;
    color: #c62828;
    font-size: 12px;
    border-top: 1px solid #ef9a9a;
}

.json-hint {
    padding: 8px 15px;
    background: #e3f2fd;
    color: #1565c0;
    font-size: 11px;
    border-top: 1px solid #bbdefb;
}

.json-hint span {
    font-weight: 600;
    color: #0d47a1;
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

.icon-btn {
    background: none;
    border: none;
    padding: 8px;
    font-size: 18px;
    cursor: pointer;
    border-radius: 6px;
    transition: all 0.2s;
    opacity: 0.6;
}

.icon-btn:hover {
    opacity: 1;
    background: rgba(0, 0, 0, 0.05);
    transform: scale(1.1);
}

.icon-btn:active {
    transform: scale(0.95);
}

.icon-danger:hover {
    background: rgba(244, 67, 54, 0.1);
}

.save-time {
    font-size: 11px;
    color: #888;
    margin-left: 8px;
    padding-left: 8px;
    border-left: 1px solid #ddd;
    display: flex;
    align-items: center;
}

.toggle-switch {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    user-select: none;
}

.toggle-switch input {
    display: none;
}

.toggle-slider {
    position: relative;
    width: 48px;
    height: 26px;
    background: #ccc;
    border-radius: 13px;
    transition: background 0.3s;
    display: flex;
    align-items: center;
    padding: 2px;
}

.toggle-slider::before {
    content: '';
    width: 22px;
    height: 22px;
    background: white;
    border-radius: 50%;
    transition: transform 0.3s;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    margin-left: 1.2px;
}

.toggle-icon {
    position: absolute;
    left: 6px;
    font-size: 12px;
    transition: opacity 0.3s, transform 0.3s;
    opacity: 0;
}

.toggle-switch input:checked+.toggle-slider {
    background: #4CAF50;
}

.toggle-switch input:checked+.toggle-slider::before {
    transform: translateX(24px);
}

.toggle-switch input:checked+.toggle-slider .toggle-icon {
    opacity: 1;
    transform: translateX(24px);
}

.toggle-switch input:not(:checked)+.toggle-slider .toggle-icon {
    opacity: 1;
    left: auto;
    right: 6px;
    filter: grayscale(100%);
}

.toggle-label {
    font-size: 12px;
    color: #666;
    font-weight: 500;
}

.toggle-switch input:checked~.toggle-label {
    color: #4CAF50;
}

.toggle-switch:hover .toggle-slider {
    box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.1);
}

.toggle-switch input:disabled+.toggle-slider {
    opacity: 0.5;
    cursor: not-allowed;
}

.help-panel {
    position: fixed;
    bottom: 20px;
    right: 420px;
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

.ai-assistant {
    position: fixed;
    bottom: 10px;
    left: 10px;
    width: fit-content;
    height: 300px;
    background: transparent;
    z-index: 100;
}

.ai-message-popup {
    position: fixed;
    z-index: 1000;
    animation: messageAppear 0.3s ease;
}

@keyframes messageAppear {
    from {
        opacity: 0;
        transform: translateY(20px) scale(0.9);
    }

    to {
        opacity: 1;
        transform: translateY(0) scale(1);
    }
}

.ai-message-content {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    border: 1px solid #0f3460;
    border-radius: 16px;
    padding: 16px 20px;
    min-width: 220px;
    box-shadow:
        0 10px 40px rgba(0, 0, 0, 0.4),
        0 0 20px rgba(0, 180, 255, 0.1);
    position: relative;
}

.ai-message-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
    padding-bottom: 10px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.ai-icon {
    font-size: 20px;
}

.ai-title {
    font-size: 13px;
    font-weight: 600;
    color: #00b4d8;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    flex: 1;
}

.ai-close {
    background: none;
    border: none;
    color: #888;
    font-size: 20px;
    cursor: pointer;
    padding: 0;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 4px;
    transition: all 0.2s;
}

.ai-close:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
}

.ai-message-text {
    color: #e0e0e0;
    font-size: 14px;
    line-height: 1.6;
    margin-bottom: 16px;
}

.ai-message-text p {
    margin: 0 0 4px 0;
}

.ai-message-text p:first-child {
    color: #fff;
    font-weight: 500;
    font-size: 15px;
    margin-bottom: 8px;
}

.ai-message-actions {
    display: flex;
    gap: 8px;
}

.ai-btn-primary,
.ai-btn-secondary {
    padding: 8px 14px;
    border-radius: 8px;
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    border: none;
}

.ai-btn-primary {
    background: linear-gradient(135deg, #00b4d8 0%, #0077b6 100%);
    color: white;
    flex: 1;
}

.ai-btn-primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(0, 180, 216, 0.3);
}

.ai-btn-secondary {
    background: rgba(255, 255, 255, 0.1);
    color: #aaa;
}

.ai-btn-secondary:hover {
    background: rgba(255, 255, 255, 0.15);
    color: #fff;
}

.ai-message-tail {
    position: absolute;
    bottom: -8px;
    left: 30px;
    width: 16px;
    height: 16px;
    background: #1a1a2e;
    border-right: 1px solid #0f3460;
    border-bottom: 1px solid #0f3460;
    transform: rotate(45deg);
}

.ai-panel {
    position: fixed;
    bottom: 20px;
    left: 20px;
    width: 200px;
    height: 200px;
    z-index: 100;
    pointer-events: none;
}

.ai-panel>* {
    pointer-events: auto;
}

.history-indicator {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 4px 8px;
    background: #f5f5f5;
    border-radius: 4px;
    font-size: 12px;
}

.history-indicator button {
    background: none;
    border: none;
    cursor: pointer;
    font-size: 16px;
    padding: 2px;
    opacity: 0.3;
    transition: opacity 0.2s;
}

.history-indicator.can-undo button:first-child,
.history-indicator.can-redo button:last-child {
    opacity: 1;
    cursor: pointer;
}

.history-indicator button:hover:not(:disabled) {
    transform: scale(1.2);
}

.history-count {
    color: #666;
    font-family: monospace;
    min-width: 40px;
    text-align: center;
}
</style>