# Этап сборки
FROM node:20-alpine AS build
WORKDIR /app

# Установка зависимостей
COPY package*.json ./
RUN npm install

# Копирование исходников и сборка
COPY . .
RUN npm run build

# Финальный этап
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]