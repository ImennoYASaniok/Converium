<script>
import { courseApi } from '../../api/course_api.js'
import AIIcon from './AIIcon.vue'
import JsonEditor from './JsonEditor.vue'
import StepEditor from './StepEditor.vue'

const STORAGE_KEY = 'course_editor_data'
const STORAGE_VERSION = '1.0'
const HISTORY_LIMIT = 50

try {
    NProgress.configure({ easing: 'linear', speed: 200, trickleSpeed: 120, showSpinner: false });
} catch { }

export default {
    name: 'CourseEditor',

    props: {
        courseId: {
            type: Number,
            required: true
        },
        courseName: {
            type: String,
            default: 'Новый курс'
         }
    },

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

            isAiActive: false,
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
                    steps: this.steps.map(s => ({ ...s })),
                    edges: this.edges.map(e => ({ ...e }))
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
                edges: Object.values(edges)
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
        window.addEventListener('beforeunload', this.saveToStorage)


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

    beforeUnmount() {
        if (this.saveTimeout) {
            clearTimeout(this.saveTimeout)
        }
        this.saveToStorage()
        document.removeEventListener('click', this.hideContextMenus)
        window.removeEventListener('keydown', this.handleKeyDown)
        window.removeEventListener('beforeunload', this.saveToStorage)
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
    <StepEditor :visible="stepEditor.visible" :step-id="stepEditor.step?.id" :step-name="stepEditor.step?.name"
        :step-type="stepEditor.step?.type" :content="stepEditor.step?.content"
        :origin-position="stepEditor.originPosition" @close="stepEditor.visible = false" @save="saveStepContent" />
</template>