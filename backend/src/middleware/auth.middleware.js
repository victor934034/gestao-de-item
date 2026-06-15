const jwt = require('../config/jwt');
const { SessaoRevogada, Usuario } = require('../models');

module.exports = async (req, res, next) => {
    try {
        const authHeader = req.headers.authorization;
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            console.log('⚠️ AuthMiddleware: Header de autorização ausente ou malformatado');
            return res.status(401).json({ error: 'Token não fornecido ou inválido.' });
        }

        const token = authHeader.split(' ')[1];

        // Verifica se token foi revogado (Logout)
        const revogado = await SessaoRevogada.findOne({ where: { token_jti: token } });
        if (revogado) {
            return res.status(401).json({ error: 'Sessão encerrada. Faça login novamente.' });
        }

        const decoded = jwt.verify(token);

        // Verifica se usuário existe e está ativo
        const usuario = await Usuario.findByPk(decoded.id);
        if (!usuario || !usuario.ativo) {
            return res.status(401).json({ error: 'Usuário inativo ou não encontrado.' });
        }

        // Pendura usuário na requisição
        req.usuario = usuario;
        req.token = token;

        next();
    } catch (error) {
        console.error('❌ Erro no AuthMiddleware:', error.message);
        return res.status(401).json({ error: 'Token expirado ou inválido.', details: error.message });
    }
};
