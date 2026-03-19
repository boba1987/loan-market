FROM node:20-alpine

WORKDIR /app

ARG NEXT_PUBLIC_API_BASE=http://backend:3000
ENV NEXT_PUBLIC_API_BASE=${NEXT_PUBLIC_API_BASE}

COPY package.json package-lock.json /app/
RUN npm ci

COPY . /app

RUN npm run build

EXPOSE 3001

CMD ["npm", "run", "start", "--", "-p", "3001"]
