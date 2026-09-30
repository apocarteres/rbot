# MVP-01, RUN-QA
FROM nginx:1.30-alpine
COPY docker/frontend-runtime/nginx.conf /etc/nginx/conf.d/default.conf
COPY frontend/dist/admin/browser /usr/share/nginx/admin
COPY frontend/dist/bot/browser /usr/share/nginx/bot
EXPOSE 8080
