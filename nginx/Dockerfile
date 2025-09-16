FROM nginx:alpine

COPY nginx.conf.template /etc/nginx/templates/nginx.conf.template

COPY entrypoint.sh /docker-entrypoint.d/entrypoint.sh
RUN chmod +x /docker-entrypoint.d/entrypoint.sh

RUN rm /etc/nginx/conf.d/default.conf
