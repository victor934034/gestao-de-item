const bcrypt = require('bcrypt');
const jwt = require('../config/jwt');
const { Usuario, SessaoRevogada } = require('../models');

exports.login = async (req, res) => {
    try {
        const { email, senha } = req.body;
        console.log(`🔐 Tentativa de login para: ${email}`);

        const usuario = await Usuario.findOne({ where: { email } });
        if (!usuario) {
            console.log(`❌ Usuário não encontrado: ${email}`);
            return res.status(401).json({ error: 'E-mail ou senha incorretos.' });
        }

        if (!usuario.ativo) {
            console.log(`⚠️ Usuário inativo: ${email}`);
            return res.status(403).json({ error: 'Usuário inativo.' });
        }

        const isMatch = await bcrypt.compare(senha, usuario.senha_hash);
        if (!isMatch) {
            return res.status(401).json({ error: 'E-mail ou senha incorretos.' });
        }

        const token = jwt.sign({ id: usuario.id, perfil: usuario.perfil });

        return res.json({
            token,
            usuario: {
                id: usuario.id,
                nome: usuario.nome,
                email: usuario.email,
                perfil: usuario.perfil
            }
        });
    } catch (error) {
        console.error('Erro no login:', error);
        res.status(500).json({ error: 'Erro interno no servidor.' });
    }
};

exports.logout = async (req, res) => {
    try {
        const token = req.token; // Preenchido no middleware de auth
        await SessaoRevogada.create({ token_jti: token });
        return res.json({ message: 'Logout realizado com sucesso.' });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao realizar logout.' });
    }
};

exports.me = async (req, res) => {
    try {
        const usuario = await Usuario.findByPk(req.usuario.id, {
            attributes: ['id', 'nome', 'email', 'perfil', 'ativo']
        });
        return res.json(usuario);
    } catch (error) {
        res.status(500).json({ error: 'Erro ao obter dados.' });
    }
};

exports.alterarSenha = async (req, res) => {
    try {
        const { senhaAtual, novaSenha } = req.body;
        const usuario = await Usuario.findByPk(req.usuario.id);

        const isMatch = await bcrypt.compare(senhaAtual, usuario.senha_hash);
        if (!isMatch) {
            return res.status(400).json({ error: 'Senha atual incorreta.' });
        }

        const salt = await bcrypt.genSalt(12);
        usuario.senha_hash = await bcrypt.hash(novaSenha, salt);
        await usuario.save();

        return res.json({ message: 'Senha alterada com sucesso.' });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao alterar a senha.' });
    }
};

exports.registrar = async (req, res) => {
    try {
        const { nome, email, senha } = req.body;
        console.log(`📝 Nova tentativa de cadastro: ${nome} <${email}>`);

        if (!nome || !email || !senha) {
            console.log('❌ Falha: Dados incompletos');
            return res.status(400).json({ error: 'Todos os campos são obrigatórios.' });
        }

        const existe = await Usuario.findOne({ where: { email } });
        if (existe) {
            console.log(`❌ Falha: E-mail ${email} já cadastrado`);
            return res.status(400).json({ error: 'Este e-mail já está sendo usado.' });
        }

        const salt = await bcrypt.genSalt(12);
        const senha_hash = await bcrypt.hash(senha, salt);

        const usuario = await Usuario.create({
            nome,
            email,
            senha_hash,
            perfil: 'ENTREGADOR',
            ativo: true
        });

        const token = jwt.sign({ id: usuario.id, perfil: usuario.perfil });

        return res.status(201).json({
            token,
            usuario: {
                id: usuario.id,
                nome: usuario.nome,
                email: usuario.email,
                perfil: usuario.perfil
            }
        });
    } catch (error) {
        console.error('Erro no registro:', error);
        res.status(500).json({ error: 'Erro ao realizar cadastro.' });
    }
};

exports.forgotPassword = async (req, res) => {
    try {
        const { email } = req.body;
        const usuario = await Usuario.findOne({ where: { email } });

        if (!usuario) {
            // Retornamos sucesso mesmo se não existir por segurança (evita enumeração de email)
            return res.json({ message: 'Se o e-mail estiver cadastrado, você receberá instruções para resetar a senha.' });
        }

        // Simulação de geração de token e envio de e-mail
        const resetToken = Math.random().toString(36).substring(2, 10).toUpperCase();
        console.log(`🔑 [MOCK] Token de reset para ${email}: ${resetToken}`);

        // Em uma implementação real, salvaríamos o token com expiração no banco
        // usuario.reset_token = resetToken;
        // usuario.reset_token_expiracao = Date.now() + 3600000; // 1 hora
        // await usuario.save();

        return res.json({
            message: 'Instruções enviadas para o seu e-mail.',
            debugToken: resetToken // Apenas para facilitar o teste inicial sem e-mail real
        });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao processar solicitação.' });
    }
};

exports.resetPassword = async (req, res) => {
    try {
        const { email, token, novaSenha } = req.body;
        const usuario = await Usuario.findOne({ where: { email } });

        if (!usuario) return res.status(404).json({ error: 'Usuário não encontrado.' });

        // Validação mock do token (em prod seria validado contra o banco)
        if (!token || token.length < 4) {
            return res.status(400).json({ error: 'Token inválido ou expirado.' });
        }

        const salt = await bcrypt.genSalt(12);
        usuario.senha_hash = await bcrypt.hash(novaSenha, salt);
        await usuario.save();

        return res.json({ message: 'Senha atualizada com sucesso.' });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao resetar senha.' });
    }
};
