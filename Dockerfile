# Backend unificado: Gestão de Items + Entrega Conexão
FROM node:18-alpine

WORKDIR /app

# Dependências nativas necessárias para bcrypt no Alpine
RUN apk add --no-cache make gcc g++ python3

COPY backend/package*.json ./

RUN npm install && apk del make gcc g++ python3

COPY backend/ .

RUN mkdir -p uploads/apk uploads/pdfs

EXPOSE 3000

ENV PORT=3000
ENV NODE_ENV=production

CMD ["node", "server.js"]

