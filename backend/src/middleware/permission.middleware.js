const { PermissaoUsuario } = require('../models');

/**
 * Middleware para verificar a permissão de um usuário para acessar um recurso.
 * @param {string} permissaoNome Nome da permissão a verificar (ex: 'criar_pedido')
 */
const permissionMiddleware = (permissaoNome) => {
    return async (req, res, next) => {
        try {
            const usuario = req.usuario;

            // SUPER_ADM tem sempre acesso
            if (usuario.perfil === 'SUPER_ADM') {
                return next();
            }

            // Buscar as permissões no banco
            const permissoesUsuario = await PermissaoUsuario.findOne({
                where: {
                    usuario_id: usuario.id,
                    permissao: permissaoNome
                }
            });

            // Lógica de Defaults: (Se true -> default é permitido para ADM ou falso para Entregador dependendo)
            // Como pedido, vamos sempre exigir que exista a regra no BD ou usar o default baseado no perfil.

            let temPermissao = false;

            if (permissoesUsuario) {
                temPermissao = permissoesUsuario.permitido;
            } else {
                // Fallback default caso a permissão não esteja no banco (tabela de defaults)
                // Por exemplo: ADM sempre true pra criar_pedido
                if (usuario.perfil === 'ADM' && !['gerenciar_usuarios'].includes(permissaoNome)) {
                    temPermissao = true;
                } else if (usuario.perfil === 'ENTREGADOR' && permissaoNome === 'atualizar_status_entrega') {
                    temPermissao = true;
                }
            }

            if (!temPermissao) {
                return res.status(403).json({ error: 'Acesso negado. Permissão insuficiente.' });
            }

            next();
        } catch (error) {
            return res.status(500).json({ error: 'Erro ao verificar permissões.' });
        }
    };
};

module.exports = permissionMiddleware;
