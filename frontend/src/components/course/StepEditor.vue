<script>
import { marked } from 'marked'
import hljs from 'highlight.js'

export default {
    name: 'StepEditor',

    props: {
        visible: Boolean,
        stepId: [String, Number],
        stepName: String,
        stepType: {
            type: String,
            default: 'THEORY'
        },
        content: String,
        originPosition: Object
    },

    emits: ['update:content', 'close', 'save'],

    data() {
        return {
            localContent: '',
            textareaRef: null,
            phase: 'closed',
            origin: { x: 0, y: 0, width: 100, height: 100 },

            practiceTab: 'theory',
            practiceData: {
                task: '',
                input: '',
                output: '',
                solution: '',
                hints: ['']
            },

            testData: {
                multipleChoice: false,
                shuffleAnswers: true,
                minScore: 50,
                questions: [{
                    score: 1,
                    text: '',
                    answers: [{ text: '', correct: false }, { text: '', correct: false }],
                    explanation: ''
                }]
            },
            currentQuestion: 0
        }
    },

    computed: {
        computedStyle() {
            if (this.phase === 'closed') return {}

            if (this.phase === 'opening') {
                return {
                    left: `${this.origin.x + this.origin.width / 2}px`,
                    top: `${this.origin.y + this.origin.height / 2}px`,
                    width: '0px',
                    height: '0px',
                    borderRadius: '50%',
                    opacity: '0',
                    transform: 'translate(-50%, -50%) scale(0)'
                }
            }

            if (this.phase === 'closing') {
                return {
                    left: `${this.origin.x + this.origin.width / 2}px`,
                    top: `${this.origin.y + this.origin.height / 2}px`,
                    width: '0px',
                    height: '0px',
                    borderRadius: '50%',
                    opacity: '0',
                    transform: 'translate(-50%, -50%) scale(0)'
                }
            }

            return {
                left: '50%',
                top: '50%',
                width: '90%',
                height: '90%',
                borderRadius: '16px',
                opacity: '1',
                transform: 'translate(-50%, -50%) scale(1)'
            }
        },

        highlightedMarkdown() {
            let text = this.localContent || ''
            text = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            text = text.replace(/^(#{1,6})\s(.+)$/gm, '<span class="md-heading">$1 $2</span>')
            text = text.replace(/\*\*(.+?)\*\*/g, '<span class="md-bold">**$1**</span>')
            text = text.replace(/\*(.+?)\*/g, '<span class="md-italic">*$1*</span>')
            text = text.replace(/`([^`]+)`/g, '<span class="md-code">`$1`</span>')
            return text
        },

        highlightedSolution() {
            return this.practiceData.solution || ''
        },

        renderedMarkdown() {
            marked.setOptions({
                highlight: (code, lang) => {
                    if (lang && hljs.getLanguage(lang)) {
                        try {
                            return hljs.highlight(code, { language: lang }).value
                        } catch (e) { }
                    }
                    return hljs.highlightAuto(code).value
                },
                breaks: true,
                gfm: true
            })
            return marked(this.localContent)
        },

        contentStats() {
            if (this.stepType === 'THEORY') {
                const words = this.localContent.trim().split(/\s+/).filter(w => w.length > 0).length
                return `${words} слов`
            }
            if (this.stepType === 'PRACTICE') {
                return `${this.practiceData.hints.length} подсказок`
            }
            if (this.stepType === 'TEST') {
                return `${this.testData.questions.length} вопросов`
            }
            return ''
        },

        stepTypeLabel() {
            const labels = { THEORY: 'Теория', PRACTICE: 'Практика', TEST: 'Тест' }
            return labels[this.stepType] || 'Шаг'
        }
    },

    watch: {
        visible: {
            handler(newVal) {
                if (newVal) {
                    this.localContent = this.content || ''
                    this.origin = this.originPosition || { x: 0, y: 0, width: 100, height: 100 }
                    try {
                        const parsed = JSON.parse(this.content || '{}')
                        if (parsed.practiceData) {
                            Object.assign(this.practiceData, parsed.practiceData)
                        }
                        if (parsed.testData) {
                            Object.assign(this.testData, parsed.testData)
                        }
                        if (parsed.markdown) {
                            this.localContent = parsed.markdown
                        }
                    } catch {}

                    this.phase = 'opening'

                    this.$nextTick(() => {
                        requestAnimationFrame(() => {
                            this.phase = 'open'
                            setTimeout(() => {
                                const textarea = this.$refs.textareaRef
                                if (textarea) textarea.focus()
                            }, 400)
                        })
                    })
                } else if (this.phase !== 'closed') {
                    this.phase = 'closing'
                    setTimeout(() => {
                        this.phase = 'closed'
                    }, 350)
                }
            },
            immediate: true
        }
    },

    methods: {
        addHint() {
            this.practiceData.hints.push('')
        },

        removeHint(index) {
            this.practiceData.hints.splice(index, 1)
        },

        addQuestion() {
            this.testData.questions.push({
                score: 1,
                text: '',
                answers: [{ text: '', correct: false }, { text: '', correct: false }],
                explanation: ''
            })
            this.currentQuestion = this.testData.questions.length - 1
        },

        removeQuestion(index) {
            this.testData.questions.splice(index, 1)
            if (this.currentQuestion >= this.testData.questions.length) {
                this.currentQuestion = Math.max(0, this.testData.questions.length - 1)
            }
        },

        addAnswer(qIndex) {
            this.testData.questions[qIndex].answers.push({ text: '', correct: false })
        },

        removeAnswer(qIndex, aIndex) {
            this.testData.questions[qIndex].answers.splice(aIndex, 1)
        },

        toggleCorrect(qIndex, aIndex) {
            const question = this.testData.questions[qIndex]
            if (this.testData.multipleChoice) {
                question.answers[aIndex].correct = !question.answers[aIndex].correct
            } else {
                question.answers.forEach((a, i) => (a.correct = i === aIndex))
            }
        },

        syncScroll(e) {
            const highlightLayer = e.target.parentElement?.querySelector('.highlight-layer')
            if (highlightLayer) {
                highlightLayer.scrollTop = e.target.scrollTop
                highlightLayer.scrollLeft = e.target.scrollLeft
            }
        },

        insertSyntax(before, after = '') {
            const textarea = this.$refs.textareaRef
            if (!textarea) return
            const start = textarea.selectionStart
            const end = textarea.selectionEnd
            const text = this.localContent
            const selected = text.substring(start, end)
            this.localContent = text.substring(0, start) + before + selected + after + text.substring(end)

            this.$nextTick(() => {
                textarea.focus()
                const newCursor = start + before.length + selected.length
                textarea.setSelectionRange(newCursor, newCursor)
            })
        },

        close() {
            if (this.phase === 'closing' || this.phase === 'closed') return
            this.phase = 'closing'
            setTimeout(() => {
                this.phase = 'closed'
                this.$emit('close')
            }, 350)
        },

        save() {
            if (this.phase === 'closing') return

            let contentToSave = this.localContent

            if (this.stepType === 'PRACTICE') {
                contentToSave = JSON.stringify({
                    markdown: this.localContent,
                    practiceData: this.practiceData
                })
            }

            if (this.stepType === 'TEST') {
                const data = this.testData
                for (let i of data.questions) {
                    if (i.score < 1) i.score = 1
                    else if (i.score > 100) i.score = 100
                }
                if (data.minScore < 0) data.minScore = 0
                else if (data.minScore > 100) data.minScore = 100
                contentToSave = JSON.stringify({
                    testData: data
                })
            }

            this.$emit('update:content', contentToSave)
            this.$emit('save', { id: this.stepId, content: contentToSave, type: this.stepType })
            this.close()
        }
    }
}
</script>

<template>
    <Teleport to="body">
        <div v-if="visible" class="step-editor-overlay" @click.self="close">
            <div class="backdrop-blur" :class="{ 'active': phase === 'open' || phase === 'opening' }"></div>

            <div class="step-editor-window" :class="[phase, stepType.toLowerCase()]" :style="computedStyle">
                <div v-if="phase === 'open'" class="editor-content">
                    <div class="editor-header">
                        <div class="editor-title">
                            <span class="step-type-badge" :class="stepType">
                                {{ stepTypeLabel }}
                            </span>
                            <span class="step-name">{{ stepName }}</span>
                        </div>
                        <button class="close-btn" @click.stop="close">
                            <span class="close-icon">×</span>
                        </button>
                    </div>

                    <div v-if="stepType === 'THEORY'" class="editor-body">
                        <div class="editor-left">
                            <div class="panel-header">
                                <span class="panel-title">Markdown</span>
                                <div class="toolbar">
                                    <button @click.stop="insertSyntax('**', '**')" title="Жирный">B</button>
                                    <button @click.stop="insertSyntax('*', '*')" title="Курсив">I</button>
                                    <button @click.stop="insertSyntax('# ')" title="Заголовок">H</button>
                                    <button @click.stop="insertSyntax('- ')" title="Список">•</button>
                                    <button @click.stop="insertSyntax('```\n', '\n```')" title="Код">&lt;/&gt;</button>
                                    <button @click.stop="insertSyntax('> ')" title="Цитата">"</button>
                                    <button @click.stop="insertSyntax('![alt](', ')')" title="Изображение">🖼️</button>
                                </div>
                            </div>
                            <div class="editor-container">
                                <pre class="highlight-layer"><code v-html="highlightedMarkdown"></code></pre>
                                <textarea ref="textareaRef" v-model="localContent" class="markdown-input"
                                    placeholder="Введите теоретический материал..." spellcheck="false"
                                    @scroll="syncScroll"></textarea>
                            </div>
                        </div>

                        <div class="editor-right">
                            <div class="panel-header">
                                <span class="panel-title">Preview</span>
                            </div>
                            <div class="preview-container" v-html="renderedMarkdown"></div>
                        </div>
                    </div>

                    <div v-else-if="stepType === 'PRACTICE'" class="editor-body practice-layout">
                        <div class="practice-tabs">
                            <button :class="{ active: practiceTab === 'theory' }"
                                @click="practiceTab = 'theory'">Теория</button>
                            <button :class="{ active: practiceTab === 'task' }"
                                @click="practiceTab = 'task'">Задание</button>
                            <button :class="{ active: practiceTab === 'solution' }"
                                @click="practiceTab = 'solution'">Решение</button>
                            <button :class="{ active: practiceTab === 'hints' }"
                                @click="practiceTab = 'hints'">Подсказки</button>
                        </div>

                        <div class="practice-content">
                            <div v-if="practiceTab === 'theory'" class="tab-panel editor-body">
                                <div class="editor-left">
                                    <div class="panel-header">
                                        <span class="panel-title">Markdown</span>
                                        <div class="toolbar">
                                            <button @click.stop="insertSyntax('**', '**')" title="Жирный">B</button>
                                            <button @click.stop="insertSyntax('*', '*')" title="Курсив">I</button>
                                            <button @click.stop="insertSyntax('# ')" title="Заголовок">H</button>
                                            <button @click.stop="insertSyntax('- ')" title="Список">•</button>
                                            <button @click.stop="insertSyntax('```\n', '\n```')"
                                                title="Код">&lt;/&gt;</button>
                                            <button @click.stop="insertSyntax('> ')" title="Цитата">"</button>
                                            <button @click.stop="insertSyntax('![alt](', ')')"
                                                title="Изображение">🖼️</button>
                                        </div>
                                    </div>
                                    <div class="editor-container">
                                        <pre class="highlight-layer"><code v-html="highlightedMarkdown"></code></pre>
                                        <textarea ref="textareaRef" v-model="localContent" class="markdown-input"
                                            placeholder="Введите теоретический материал (как вступление)..."
                                            spellcheck="false" @scroll="syncScroll"></textarea>
                                    </div>
                                </div>

                                <div class="editor-right">
                                    <div class="panel-header">
                                        <span class="panel-title">Preview</span>
                                    </div>
                                    <div class="preview-container" v-html="renderedMarkdown"></div>
                                </div>
                            </div>

                            <div v-if="practiceTab === 'task'" class="tab-panel">
                                <div class="task-editor">
                                    <div class="task-section">
                                        <label>Условие задачи:</label>
                                        <textarea v-model="practiceData.task" class="task-textarea"
                                            placeholder="Опишите задание для студента..."></textarea>
                                    </div>
                                    <div class="task-section">
                                        <label>Входные данные:</label>
                                        <textarea v-model="practiceData.input" class="task-textarea code"
                                            placeholder="Пример входных данных..."></textarea>
                                    </div>
                                    <div class="task-section">
                                        <label>Ожидаемый результат:</label>
                                        <textarea v-model="practiceData.output" class="task-textarea code"
                                            placeholder="Пример правильного ответа..."></textarea>
                                    </div>
                                </div>
                            </div>

                            <div v-if="practiceTab === 'solution'" class="tab-panel">
                                <div class="editor-container">
                                    <pre class="highlight-layer"><code v-html="highlightedSolution"></code></pre>
                                    <textarea ref="textareaRef" v-model="practiceData.solution"
                                        class="markdown-input code-mode"
                                        placeholder="Подробное решение с объяснением..." spellcheck="false"
                                        @scroll="syncScroll"></textarea>
                                </div>
                            </div>

                            <div v-if="practiceTab === 'hints'" class="tab-panel">
                                <div class="hints-list">
                                    <div v-for="(hint, index) in practiceData.hints" :key="index" class="hint-item">
                                        <div class="d-flex">
                                            <span class="hint-number">Подсказка {{ index + 1 }}</span>
                                        </div>
                                        <textarea v-model="practiceData.hints[index]" class="hint-textarea"
                                            placeholder="Введите подсказку..."></textarea>
                                        <div class="d-flex">
                                            <button @click="removeHint(index)" class="hint-remove">×</button>
                                        </div>
                                    </div>
                                    <button @click="addHint" class="add-hint">+ Добавить подсказку</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div v-else-if="stepType === 'TEST'" class="editor-body test-layout">
                        <div class="test-sidebar">
                            <div class="test-settings">
                                <label>
                                    <input type="checkbox" v-model="testData.multipleChoice">
                                    Множественный выбор
                                </label>
                                <label>
                                    <input type="checkbox" v-model="testData.shuffleAnswers">
                                    Перемешивать ответы
                                </label>
                                <label>
                                    Минимальный процент выполнения:
                                    <input type="number" v-model="testData.minScore" min="0" max="100">%
                                </label>
                            </div>
                            <button @click="addQuestion" class="add-question">+ Добавить вопрос</button>
                        </div>

                        <div class="test-content">
                            <div v-for="(question, qIndex) in testData.questions" :key="qIndex" class="question-card"
                                :class="{ active: currentQuestion === qIndex }" @click="currentQuestion = qIndex">
                                <div class="question-header">
                                    <span>Вопрос {{ qIndex + 1 }}</span>
                                    <div class="test-settings">
                                        <label>
                                            Балл за правильный ответ:
                                            <input type="number" v-model="question.score" min="1" max="100">
                                        </label>
                                    </div>
                                    <button @click.stop="removeQuestion(qIndex)" class="remove-btn">×</button>
                                </div>

                                <textarea v-model="question.text" class="question-text"
                                    placeholder="Текст вопроса..."></textarea>

                                <div class="answers-list">
                                    <div v-for="(answer, aIndex) in question.answers" :key="aIndex" class="answer-item">
                                        <input :type="testData.multipleChoice ? 'checkbox' : 'radio'"
                                            :name="'question-' + qIndex" :checked="answer.correct"
                                            @change="toggleCorrect(qIndex, aIndex)">
                                        <input v-model="answer.text" class="answer-input"
                                            placeholder="Вариант ответа...">
                                        <button @click.stop="removeAnswer(qIndex, aIndex)"
                                            class="remove-btn small">×</button>
                                    </div>
                                    <button @click="addAnswer(qIndex)" class="add-answer">+ Добавить ответ</button>
                                </div>

                                <div class="question-explanation">
                                    <label>Объяснение (показывается после ответа):</label>
                                    <textarea v-model="question.explanation" class="explanation-textarea"
                                        placeholder="Почему правильный ответ именно такой..."></textarea>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="editor-footer">
                        <span class="last-edit">{{ contentStats }}</span>
                        <div class="actions">
                            <button class="btn-secondary" @click.stop="close">Отмена</button>
                            <button class="btn-primary" @click.stop="save">Сохранить</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </Teleport>
</template>

<style scoped>
.step-editor-overlay {
    position: fixed;
    inset: 0;
    z-index: 10000;
    display: flex;
    align-items: center;
    justify-content: center;
}

.backdrop-blur {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(10px);
    opacity: 0;
    transition: opacity 0.35s ease;
}

.backdrop-blur.active {
    opacity: 1;
}

.step-editor-window {
    position: fixed;
    background: #1e1e2e;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.1);
    transition: all 0.4s cubic-bezier(0.32, 0.72, 0, 1);
    will-change: left, top, width, height, transform, opacity, border-radius;
}

.step-editor-window.opening,
.step-editor-window.closing {
    pointer-events: none;
}

.step-editor-window.open {
    pointer-events: auto;
}

.step-editor-window.theory {
    border-top: 4px solid #2196F3;
}

.step-editor-window.practice {
    border-top: 4px solid #4CAF50;
}

.step-editor-window.test {
    border-top: 4px solid #E91E63;
}

.editor-content {
    display: flex;
    flex-direction: column;
    height: 100%;
    animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
    from {
        opacity: 0;
    }

    to {
        opacity: 1;
    }
}

.editor-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px 20px;
    background: #252536;
    border-bottom: 1px solid #333344;
    flex-shrink: 0;
}

.editor-title {
    display: flex;
    align-items: center;
    gap: 12px;
}

.step-type-badge {
    padding: 4px 12px;
    border-radius: 20px;
    font-size: 11px;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    border: 0.2em solid;
}

.step-type-badge.THEORY {
    border-color: #2196F3;
    color: white;
}

.step-type-badge.PRACTICE {
    border-color: #4CAF50;
    color: white;
}

.step-type-badge.TEST {
    border-color: #E91E63;
    color: white;
}

.step-name {
    color: #fff;
    font-size: 16px;
    font-weight: 500;
}

.close-btn {
    width: 36px;
    height: 36px;

    border-radius: 50%;
    border: none;
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s;
}

.close-btn:hover {
    background: rgba(255, 255, 255, 0.2);
    transform: rotate(90deg);
}

.close-icon {
    font-size: 24px;
    line-height: 1;
}

.editor-body {
    flex: 1;
    display: flex;
    overflow: hidden;
}

.editor-left,
.editor-right {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

.editor-left {
    border-right: 1px solid #333344;
}

.practice-layout {
    flex-direction: column;
}

.practice-tabs {
    display: flex;
    gap: 4px;
    padding: 8px 16px;
    background: #1a1a2e;
    border-bottom: 1px solid #333344;
}

.practice-tabs button {
    padding: 8px 16px;
    border: none;
    background: transparent;
    color: #888;
    cursor: pointer;
    border-radius: 8px;
    font-size: 13px;
    transition: all 0.2s;
}

.practice-tabs button:hover {
    background: #252536;
    color: #ccc;
}

.practice-tabs button.active {
    background: #4CAF50;
    color: white;
}

.practice-content {
    flex: 1;
    overflow: auto;
    padding: 20px;
}

.tab-panel {
    height: 100%;
}

.task-editor {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.task-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.task-section label {
    color: #888;
    font-size: 13px;
    font-weight: 500;
}

.task-textarea {
    padding: 12px;
    background: #252536;
    border: 1px solid #333344;
    border-radius: 8px;
    color: #cdd6f4;
    font-family: 'JetBrains Mono', monospace;
    font-size: 14px;
    line-height: 1.6;
    resize: vertical;
    min-height: 100px;
}

.task-textarea.code {
    background: #11111b;
    font-family: 'JetBrains Mono', monospace;
}

.hints-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.hint-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px;
    background: #252536;
    border-radius: 8px;
    border: 1px solid #333344;
}

.hint-number {
    color: #4CAF50;
    font-size: 16px;
    font-weight: 600;
    white-space: nowrap;
}

.d-flex {
    display: flex;
    align-items: center;
    height: stretch;
}

.hint-textarea {
    flex: 1;
    padding: 8px 12px;
    background: #1e1e2e;
    border: 1px solid #333344;
    border-radius: 6px;
    color: #cdd6f4;
    font-size: 14px;
    resize: vertical;
    min-height: 60px;
}

.hint-remove {
    width: 28px;
    height: 28px;
    border: none;
    background: #f44336;
    color: white;
    border-radius: 6px;
    cursor: pointer;
    font-size: 18px;
    line-height: 1;
}

.add-hint {
    padding: 12px;
    border: 2px dashed #4CAF50;
    background: transparent;
    color: #4CAF50;
    border-radius: 8px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
}

.add-hint:hover {
    background: rgba(76, 175, 80, 0.1);
}

.test-layout {
    flex-direction: row;
}

.test-sidebar {
    width: 250px;
    padding: 20px;
    background: #1a1a2e;
    border-right: 1px solid #333344;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.test-settings {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.test-settings label {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #888;
    font-size: 13px;
    cursor: pointer;
}

.test-settings input[type="number"] {
    width: 38px;
    padding: 4px 8px;
    background: #252536;
    border: 1px solid #333344;
    border-radius: 4px;
    color: #cdd6f4;
}

.add-question {
    padding: 12px;
    background: #E91E63;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.2s;
}

.add-question:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(233, 30, 99, 0.3);
}

.test-content {
    flex: 1;
    overflow: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.question-card {
    padding: 20px;
    background: #252536;
    border-radius: 12px;
    border: 2px solid transparent;
    cursor: pointer;

}

.question-card:hover {
    border-color: #444455;
}

.question-card.active {
    border-color: #E91E63;
    background: #2a2a3e;
}

.question-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.question-header span {
    color: #E91E63;
    font-size: 13px;
    font-weight: 600;
}

.question-text {
    width: stretch;
    padding: 12px;
    background: #1e1e2e;
    border: 1px solid #333344;
    border-radius: 8px;
    color: #cdd6f4;
    font-size: 14px;
    margin-bottom: 16px;
    resize: vertical;
    min-height: 80px;
}

.answers-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    margin-bottom: 16px;
}

.answer-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 12px;
    background: #1e1e2e;
    border-radius: 8px;
}

.answer-item input[type="checkbox"],
.answer-item input[type="radio"] {
    width: 18px;
    height: 18px;
    cursor: pointer;
}

.answer-input {
    flex: 1;
    padding: 8px 12px;
    background: transparent;
    border: 1px solid #333344;
    border-radius: 6px;
    color: #cdd6f4;
    font-size: 14px;
}

.answer-input:focus {
    border-color: #E91E63;
    outline: none;
}

.add-answer {
    padding: 8px 16px;
    background: transparent;
    border: 1px dashed #666;
    color: #888;
    border-radius: 6px;
    cursor: pointer;
    font-size: 13px;
    transition: all 0.2s;
}

.add-answer:hover {
    border-color: #E91E63;
    color: #E91E63;
}

.question-explanation {
    padding-top: 16px;
    border-top: 1px solid #333344;
}

.question-explanation label {
    display: block;
    color: #888;
    font-size: 12px;
    margin-bottom: 8px;
}

.explanation-textarea {
    width: stretch;
    padding: 12px;
    background: #1e1e2e;
    border: 1px solid #333344;
    border-radius: 8px;
    color: #cdd6f4;
    font-size: 14px;
    resize: vertical;
    min-height: 60px;
}

.remove-btn {
    width: 28px;
    height: 28px;
    border: none;
    background: #f44336;
    color: white;
    border-radius: 6px;
    cursor: pointer;
    font-size: 16px;
    line-height: 1;
    transition: all 0.2s;
}

.remove-btn.small {
    width: 24px;
    height: 24px;
    font-size: 14px;
}

.remove-btn:hover {
    transform: scale(1.1);
}

.panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #252536;
    border-bottom: 1px solid #333344;
    flex-shrink: 0;
}

.panel-title {
    font-size: 13px;
    color: #888;
    font-weight: 500;
}

.toolbar {
    display: flex;
    gap: 6px;
}

.toolbar button {
    width: 28px;
    height: 28px;
    border: none;
    background: #333344;
    color: #ccc;
    border-radius: 6px;
    cursor: pointer;
    font-size: 12px;
    font-weight: 600;
    transition: all 0.2s;
}

.toolbar button:hover {
    background: #444455;
    color: #fff;
}

.editor-container {
    flex: 1;
    position: relative;
    overflow: hidden;
    height: 100%
}

.highlight-layer,
.markdown-input {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 16px;
    border: none;
    font-family: 'JetBrains Mono', 'Fira Code', 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    line-height: 1.7;
    white-space: pre;
    overflow: auto;
    tab-size: 2;
}

.highlight-layer {
    background: #1e1e2e;
    color: #cdd6f4;
    pointer-events: none;
    z-index: 1;
}

.highlight-layer code {
    font-family: inherit;
    background: none !important;
}

:deep(.md-heading) {
    color: #f38ba8;
    font-weight: bold;
}

:deep(.md-bold) {
    color: #a6e3a1;
    font-weight: bold;
}

:deep(.md-italic) {
    color: #cba6f7;
    font-style: italic;
}

:deep(.md-code) {
    color: #f5c2e7;
    background: rgba(245, 194, 231, 0.1);
    border-radius: 3px;
}

:deep(.md-code-block) {
    color: #89b4fa;
}

:deep(.md-link) {
    color: #89b4fa;
    text-decoration: underline;
}

:deep(.md-list) {
    color: #fab387;
}

:deep(.md-quote) {
    color: #9399b2;
    font-style: italic;
}

:deep(.md-hr) {
    color: #585b70;
}

.markdown-input {
    background: transparent;
    color: transparent;
    caret-color: #fff;
    z-index: 2;
    resize: none;
    outline: none;
}

.markdown-input::selection {
    background: rgba(137, 180, 250, 0.3);
    color: transparent;
}

.preview-container {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
    background: #181825;
    color: #cdd6f4;
}

.preview-container :deep(h1) {
    color: #f38ba8;
    font-size: 28px;
    border-bottom: 2px solid #45475a;
    padding-bottom: 8px;
    margin-bottom: 16px;
    margin-top: 0;
}

.preview-container :deep(h2) {
    color: #fab387;
    font-size: 22px;
    margin-top: 24px;
    margin-bottom: 12px;
}

.preview-container :deep(h3) {
    color: #f9e2af;
    font-size: 18px;
    margin-top: 20px;
    margin-bottom: 10px;
}

.preview-container :deep(p) {
    line-height: 1.8;
    margin-bottom: 12px;
    color: #cdd6f4;
}

.preview-container :deep(strong) {
    color: #a6e3a1;
    font-weight: 600;
}

.preview-container :deep(em) {
    color: #cba6f7;
    font-style: italic;
}

.preview-container :deep(code) {
    background: #313244;
    padding: 2px 8px;
    border-radius: 4px;
    font-family: 'JetBrains Mono', monospace;
    font-size: 13px;
    color: #f5c2e7;
}

.preview-container :deep(pre) {
    background: #11111b;
    padding: 16px;
    border-radius: 8px;
    overflow-x: auto;
    margin: 16px 0;
    border: 1px solid #313244;
}

.preview-container :deep(pre code) {
    background: none;
    padding: 0;
    color: #cdd6f4;
    font-size: 13px;
    line-height: 1.6;
}

.preview-container :deep(.hljs-keyword),
.preview-container :deep(.hljs-selector-tag),
.preview-container :deep(.hljs-tag) {
    color: #f38ba8;
}

.preview-container :deep(.hljs-string),
.preview-container :deep(.hljs-regexp),
.preview-container :deep(.hljs-addition) {
    color: #a6e3a1;
}

.preview-container :deep(.hljs-number),
.preview-container :deep(.hljs-literal) {
    color: #fab387;
}

.preview-container :deep(.hljs-comment),
.preview-container :deep(.hljs-quote) {
    color: #585b70;
    font-style: italic;
}

.preview-container :deep(.hljs-function),
.preview-container :deep(.hljs-title),
.preview-container :deep(.hljs-section) {
    color: #89b4fa;
}

.preview-container :deep(.hljs-params),
.preview-container :deep(.hljs-variable) {
    color: #cdd6f4;
}

.preview-container :deep(.hljs-attr),
.preview-container :deep(.hljs-property) {
    color: #f9e2af;
}

.preview-container :deep(.hljs-built_in),
.preview-container :deep(.hljs-builtin-name) {
    color: #f5c2e7;
}

.preview-container :deep(.hljs-operator),
.preview-container :deep(.hljs-punctuation) {
    color: #9399b2;
}

.preview-container :deep(.hljs-meta) {
    color: #74c7ec;
}

.preview-container :deep(.hljs-emphasis) {
    font-style: italic;
}

.preview-container :deep(.hljs-strong) {
    font-weight: bold;
}

.preview-container :deep(ul),
.preview-container :deep(ol) {
    margin: 12px 0;
    padding-left: 24px;
}

.preview-container :deep(li) {
    margin: 6px 0;
    color: #cdd6f4;
}

.preview-container :deep(li::marker) {
    color: #89b4fa;
}

.preview-container :deep(blockquote) {
    border-left: 4px solid #89b4fa;
    padding-left: 16px;
    margin: 16px 0;
    color: #a6adc8;
    font-style: italic;
}

.preview-container :deep(a) {
    color: #89b4fa;
    text-decoration: none;
}

.preview-container :deep(a:hover) {
    text-decoration: underline;
}

.preview-container :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;
}

.preview-container :deep(th),
.preview-container :deep(td) {
    border: 1px solid #45475a;
    padding: 8px 12px;
    text-align: left;
}

.preview-container :deep(th) {
    background: #313244;
    color: #f9e2af;
    font-weight: 600;
}

.preview-container :deep(tr:nth-child(even)) {
    background: #1e1e2e;
}

.preview-container :deep(img) {
    max-width: 100%;
    border-radius: 8px;
    margin: 16px 0;
}

.editor-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    background: #252536;
    border-top: 1px solid #333344;
    animation: fadeIn 0.2s ease;
    flex-shrink: 0;
}

.last-edit {
    font-size: 12px;
    color: #585b70;
}

.actions {
    display: flex;
    gap: 12px;
}

.btn-secondary,
.btn-primary {
    padding: 8px 20px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    border: none;
}

.btn-secondary {
    background: #313244;
    color: #cdd6f4;
}

.btn-secondary:hover {
    background: #414155;
}

.btn-primary {
    background: linear-gradient(135deg, #89b4fa 0%, #74c7ec 100%);
    color: #1e1e2e;
}

.btn-primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(137, 180, 250, 0.3);
}

::-webkit-scrollbar {
    width: 10px;
    height: 10px;
}

::-webkit-scrollbar-track {
    background: #1e1e2e;
}

::-webkit-scrollbar-thumb {
    background: #45475a;
    border-radius: 5px;
}

::-webkit-scrollbar-thumb:hover {
    background: #585b70;
}
</style>