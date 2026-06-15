const express = require('express');
const compression = require('compression');
const cors = require('cors');
const path = require('path');
require('dotenv').config();

const app = express();

app.use(compression());

// Middleware de Log para depuração profunda no EasyPanel
app.use((req, res, next) => {
    const start = Date.now();
    res.on('finish', () => {
        const duration = Date.now() - start;
        console.log(`[${new Date().toISOString()}] ${req.method} ${req.url} -> ${res.statusCode} (${duration}ms)`);
    });
    next();
});

app.use(cors({
    origin: '*',
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Monitor de requisições para debug remoto
let lastRequests = [];
app.use((req, res, next) => {
    if (req.url !== '/api/debug-logs') {
        lastRequests.unshift({
            timestamp: new Date().toISOString(),
            method: req.method,
            url: req.url,
            headers: req.headers
        });
        if (lastRequests.length > 50) lastRequests.pop();
    }
    next();
});

app.get('/api/debug-logs', (req, res) => {
    res.json({
        uptime: process.uptime(),
        lastRequests
    });
});

// Serve PDFs route
app.use('/uploads/pdfs', express.static(path.join(__dirname, '../uploads/pdfs')));

// Serve APK updates route
app.use('/uploads/apk', express.static(path.join(__dirname, '../uploads/apk')));

// Test route para verificar se o roteamento /api funciona
app.get('/api/test', (req, res) => {
    res.json({ message: 'Roteamento /api funcionando' });
});

// Routes
app.get('/api/health', (req, res) => {
    res.json({
        status: 'online',
        version: 'Parser V5 Final + Stock Sync',
        timestamp: new Date().toISOString()
    });
});

app.use('/api/auth', require('./routes/auth.routes'));
app.use('/api/pedidos', require('./routes/pedido.routes'));
app.use('/api/usuarios', require('./routes/usuario.routes'));
app.use('/api/relatorios', require('./routes/relatorio.routes'));
app.use('/api/estoque', require('./routes/estoque'));
app.use('/api/stock', require('./routes/stock'));
app.use('/api/version', require('./routes/version'));


app.get('/', (req, res) => {
    res.send('API Conexão BR277 funcionando!');
});

// Middleware para capturar 404 e fornecer mensagem detalhada para debug
app.use((req, res) => {
    console.log(`[404 NOT FOUND] ${req.method} ${req.url}`);
    res.status(404).json({
        error: `Rota não encontrada no servidor: ${req.method} ${req.url}`,
        tip: 'Verifique se a URL base no app termina com /api/ e se as rotas não começam com /'
    });
});

module.exports = app;
