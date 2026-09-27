# ========== 阶段 1：构建静态文件 ==========
FROM node:22-alpine AS builder
WORKDIR /build

# 先拷依赖清单 —— 利用缓存，package.json 没变就不用重装
COPY package*.json ./
RUN npm ci

# 再拷源码并构建
COPY . .
RUN npm run build

# ========== 阶段 2：nginx 提供静态文件 ==========
FROM nginx:alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80