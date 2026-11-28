# Etapa 1: dependencias, pruebas Jest y build de producción (si una prueba falla, la imagen no se construye).
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run test:ci && npx ng build --configuration production

# Etapa 2: solo los estáticos servidos por Nginx, que además hace de proxy hacia el backend (/api).
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/proyectos-frontend/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=3 CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1
