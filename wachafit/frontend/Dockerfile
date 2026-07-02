# Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json .
RUN npm ci --prefer-offline
COPY . .
# VITE_API_URL='' → axios usa URLs relativas; nginx faz proxy de /api/ para backend
ARG VITE_API_URL=""
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# Serve
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
