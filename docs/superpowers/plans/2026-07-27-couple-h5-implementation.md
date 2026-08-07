# 情侣 H5 实现计划

> **For agentic workers:** 按里程碑顺序实现；本仓库已有产品计划，此文档为落地拆分。

**Goal:** 落地个人情侣 H5（展示/正常双模式 + 纪念日/相册/问答/三挑战）。

**Architecture:** Spring Boot API + Vue3 H5；MySQL；本地上传；JWT 双人密码。

**Tech Stack:** Java 17+、Spring Boot 3.3、MyBatis-Plus、JJWT、Vue 3、Vite、Pinia、Vue Router

## Global Constraints

- 订阅号：无 OpenID
- 挑战：仅共同完成
- 问答：双方答完才互看
- 展示模式：仅封面公开字段

## 里程碑

见项目根计划：脚手架 → 鉴权封面 → 纪念日 → 相册 → 问答 → 挑战 → 部署说明
