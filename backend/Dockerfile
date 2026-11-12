FROM node:22-alpine AS builder

RUN apk add --no-cache openssl

WORKDIR /app

COPY package*.json ./
COPY prisma ./prisma/

RUN npm install

COPY . .

RUN npm run prisma:generate
RUN npm run build

# Production image
FROM node:22-alpine AS runner

RUN apk add --no-cache openssl

WORKDIR /app

ENV NODE_ENV=production

COPY package*.json ./
RUN npm install --only=production && npm install prisma --no-save

COPY --from=builder /app/dist ./dist
COPY --from=builder /app/prisma ./prisma
COPY --from=builder /app/node_modules/.prisma ./node_modules/.prisma
COPY --from=builder /app/node_modules/@prisma ./node_modules/@prisma

EXPOSE 5000

CMD ["sh", "-c", "npx prisma db push --skip-generate && node dist/server.js"]
