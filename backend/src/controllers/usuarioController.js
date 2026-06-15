const bcrypt = require('bcrypt');
const { Usuario, PermissaoUsuario, Pedido } = require('../models');

exports.listarUsuarios = async (req, res) => {
    try {
        const usuarios = await Usuario.findAll({
            attributes: { exclude: ['senha_hash'] }
        });
        return res.json(usuarios);
    } catch (error) {
        res.status(500).json({ error: 'Erro ao buscar usuários.' });
    }
};

exports.detalhesUsuario = async (req, res) => {
    try {
        const usuario = await Usuario.findByPk(req.params.id, {
            attributes: { exclude: ['senha_hash'] },
            include: [{ model: PermissaoUsuario, as: 'permissoes' }]
        });
        if (!usuario) return res.status(404).json({ error: 'Usuário não encontrado.' });
        return res.json(usuario);
    } catch (error) {
        res.status(500).json({ error: 'Erro ao buscar usuário.' });
    }
};

exports.criarUsuario = async (req, res) => {
    try {
        const { nome, email, senha, perfil } = req.body;

        const existe = await Usuario.findOne({ where: { email } });
        if (existe) {
            return res.status(400).json({ error: 'E-mail já está em uso.' });
        }

        const salt = await bcrypt.genSalt(12);
        const senha_hash = await bcrypt.hash(senha, salt);

        const novoUsuario = await Usuario.create({
            nome, email, senha_hash, perfil
        });

        // Default permissions logic would go here if needed

        return res.status(201).json({ id: novoUsuario.id, nome, email, perfil });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao criar usuário.' });
    }
};

exports.editarUsuario = async (req, res) => {
    try {
        const { nome, email, perfil } = req.body;
        const usuario = await Usuario.findByPk(req.params.id);

        if (!usuario) return res.status(404).json({ error: 'Usuário não encontrado.' });

        usuario.nome = nome || usuario.nome;
        usuario.email = email || usuario.email;
        if (req.usuario.perfil === 'SUPER_ADM') {
            usuario.perfil = perfil || usuario.perfil;
        }

        await usuario.save();
        return res.json({ message: 'Usuário atualizado com sucesso.' });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao atualizar usuário.' });
    }
};

exports.alterarStatus = async (req, res) => {
    try {
        const { ativo } = req.body;
        const usuario = await Usuario.findByPk(req.params.id);

        if (!usuario) return res.status(404).json({ error: 'Usuário não encontrado.' });

        usuario.ativo = ativo;
        await usuario.save();

        return res.json({ message: `Usuário ${ativo ? 'ativado' : 'desativado'} com sucesso.` });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao alterar status.' });
    }
};

exports.listarPermissoes = async (req, res) => {
    try {
        const permissoes = await PermissaoUsuario.findAll({ where: { usuario_id: req.params.id } });
        return res.json(permissoes);
    } catch (error) {
        res.status(500).json({ error: 'Erro ao buscar permissões.' });
    }
};

exports.atualizarPermissoes = async (req, res) => {
    try {
        const { permissoes } = req.body; // array de perfis: { permissao: nome, permitido: true/false }
        const usuarioId = req.params.id;

        for (const p of permissoes) {
            const existente = await PermissaoUsuario.findOne({
                where: { usuario_id: usuarioId, permissao: p.permissao }
            });

            if (existente) {
                existente.permitido = p.permitido;
                await existente.save();
            } else {
                await PermissaoUsuario.create({
                    usuario_id: usuarioId,
                    permissao: p.permissao,
                    permitido: p.permitido
                });
            }
        }

        return res.json({ message: 'Permissões atualizadas com sucesso.' });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao atualizar permissões.' });
    }
};

exports.historicoEntregas = async (req, res) => {
    try {
        const pedidos = await Pedido.findAll({
            where: { entregador_id: req.params.id },
            order: [['data_pedido', 'DESC']]
        });
        return res.json(pedidos);
    } catch (error) {
        res.status(500).json({ error: 'Erro ao buscar histórico de entregas.' });
    }
};
exports.statsUsuario = async (req, res) => {
    try {
        const uid = req.params.uid || req.usuario.id;

        const totalEntregas = await Pedido.count({
            where: { entregador_id: uid, status: ['ENTREGUE', 'CONCLUIDO'] }
        });

        const ganhos = await Pedido.sum('total_liquido', {
            where: { entregador_id: uid, status: ['ENTREGUE', 'CONCLUIDO'] }
        });

        return res.json({
            total_entregas: totalEntregas || 0,
            ganhos: ganhos || 0.0
        });
    } catch (error) {
        console.error('Erro ao buscar estatísticas:', error);
        res.status(500).json({ error: 'Erro ao buscar estatísticas.' });
    }
};

exports.excluirUsuario = async (req, res) => {
    try {
        const usuario = await Usuario.findByPk(req.params.id);
        if (!usuario) return res.status(404).json({ error: 'Usuário não encontrado.' });

        if (usuario.perfil === 'SUPER_ADM') {
            return res.status(403).json({ error: 'Não é possível excluir um Super Administrador.' });
        }

        const pedidosPendentes = await Pedido.count({
            where: { entregador_id: usuario.id, status: ['PENDENTE', 'EM_ROTA', 'RETIRADO'] }
        });

        if (pedidosPendentes > 0) {
            return res.status(400).json({ error: 'Usuário possui pedidos em andamento. Impossível excluir.' });
        }

        await usuario.destroy();
        return res.json({ message: 'Usuário excluído com sucesso.' });
    } catch (error) {
        console.error('Erro ao excluir usuário:', error);
        res.status(500).json({ error: 'Erro ao excluir usuário.' });
    }
};

exports.desativarPropriaConta = async (req, res) => {
    try {
        const usuario = await Usuario.findByPk(req.usuario.id);
        if (!usuario) return res.status(404).json({ error: 'Usuário não encontrado.' });

        usuario.ativo = false;
        await usuario.save();

        return res.json({ message: 'Sua conta foi desativada com sucesso.' });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao desativar conta.' });
    }
};

exports.excluirPropriaConta = async (req, res) => {
    try {
        const usuario = await Usuario.findByPk(req.usuario.id);
        if (!usuario) return res.status(404).json({ error: 'Usuário não encontrado.' });

        // Verificar se há pedidos pendentes
        const pedidosPendentes = await Pedido.count({
            where: { entregador_id: usuario.id, status: ['PENDENTE', 'EM_ROTA', 'RETIRADO'] }
        });

        if (pedidosPendentes > 0) {
            return res.status(400).json({
                error: 'Não é possível excluir a conta com pedidos em andamento. Conclua suas entregas primeiro.'
            });
        }

        await usuario.destroy();
        return res.json({ message: 'Sua conta foi excluída permanentemente.' });
    } catch (error) {
        console.error('Erro ao excluir conta:', error);
        res.status(500).json({ error: 'Erro ao excluir conta.' });
    }
};
