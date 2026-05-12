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