# Multi-stage build: compile the bundle, then serve the static files from nginx.
#
#   docker build --build-arg VITE_API_BASE_URL=http://localhost:8081 .

FROM node:22-alpine AS build

WORKDIR /app

# Dependencies are copied first so the install layer is cached across code changes.
COPY package.json package-lock.json* ./
RUN npm install

COPY . .

# The API base URL is baked in at build time: the browser must know which origin to
# call, and it cannot be resolved at runtime from a static bundle.
ARG VITE_API_BASE_URL=http://localhost:8081
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

FROM nginx:1.27-alpine AS runtime

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

# nginx binds IPv4 only (listen 80 -> 0.0.0.0:80), while "localhost" resolves to
# ::1 first inside the container, so a localhost healthcheck fails forever on a
# perfectly healthy server. Probe the IPv4 loopback explicitly.
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ >/dev/null || exit 1

CMD ["nginx", "-g", "daemon off;"]