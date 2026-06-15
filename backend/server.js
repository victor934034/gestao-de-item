const http = require('http');
const app = require('./src/app');
const { sequelize } = require('./src/models');
const socketService = require('./src/services/socketService');

const PORT = process.env.PORT || 3000;

const server = http.createServer(app);

// Inicializar Socket.io
socketService.init(server);

// Iniciar o servidor IMEDIATAMENTE para evitar 502/Bad Gateway no EasyPanel
server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Servidor backend rodando em http://0.0.0.0:${PORT}`);
    console.log('--- Iniciando conexão com o Banco de Dados ---');
});

// Tentar conectar e sincronizar em segundo plano
sequelize.authenticate()
    .then(() => {
        console.log('✅ Banco de dados conectado com sucesso.');
        return sequelize.sync({ alter: true });
    })
    .then(async () => {
        console.log('✅ Tabelas sincronizadas.');

        // Garantir que o admin existe
        const { Usuario } = require('./src/models');
        const bcrypt = require('bcrypt');
        const adminEmail = 'admin@conexaobr277.com.br';

        const adminExiste = await Usuario.findOne({ where: { email: adminEmail } });
        if (!adminExiste) {
            console.log('🚀 Criando usuário administrador padrão...');
            const salt = await bcrypt.genSalt(12);
            const senhaHash = await bcrypt.hash('admin123', salt);

            await Usuario.create({
                nome: 'Administrador Principal',
                email: adminEmail,
                senha_hash: senhaHash,
                perfil: 'SUPER_ADM',
                ativo: true
            });
            console.log('✅ Administrador criado: admin@conexaobr277.com.br / admin123');
        }
    })
    .catch(err => {
        console.error('❌ ERRO NO BANCO DE DADOS:', err.message);
        console.log('⚠️ O servidor continuará rodando para debug, mas as requisições ao banco falharão.');
    });
