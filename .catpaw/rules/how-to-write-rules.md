---
ruleType: Manual
description: 元规则 - 如何编写和维护高质量的Rules
keywords: [rules, 规则编写, 维护, meta]
priority: CRITICAL
---

# 元规则：如何编写Rules

## 🎯 核心约束

```
MUST_HAVE: YAML元数据 + HTML注释分离 + 结构化内容
MUST_FOLLOW: RAG友好格式 + 人类可读设计
NEVER_MIX: 描述性文字与核心约束
FILE_SIZE: 2-4KB (平衡完整性与效率)
```

## 📋 标准Rules模板

~~~markdown
---
ruleType: Manual|Constraint|Pattern
description: 简洁描述
keywords: [关键词1, 关键词2]
priority: CRITICAL|HIGH|MEDIUM|LOW
---

<!-- 人类阅读的设计背景 -->

# 规则标题

## 🎯 核心约束
MUST_USE: 必须使用的模式
NEVER_USE: 禁止的方式

## 💻 代码模式
[标准实现示例]

<!-- 人类阅读的详细说明 -->
