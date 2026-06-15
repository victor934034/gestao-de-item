const { Pedido, Usuario, sequelize } = require('../models');
const { Op } = require('sequelize');

exports.dashboard = async (req, res) => {
    try {
        const hoje = new Date();
        hoje.setHours(0, 0, 0, 0);

        const pendentes = await Pedido.count({ where: { status: 'PENDENTE' } });
        const emRota = await Pedido.count({ where: { status: 'EM_ROTA' } });
        const entregues = await Pedido.count({ where: { status: 'ENTREGUE' } });
        const hojeCount = await Pedido.count({
            where: {
                data_pedido: { [Op.gte]: hoje }
            }
        });

        return res.json({
            totalHoje: hojeCount,
            pendentes,
            emRota,
            entregues
        });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao gerar dashboard.' });
    }
};

exports.rankingEntregadores = async (req, res) => {
    try {
        const ranking = await Pedido.findAll({
            attributes: [
                'entregador_id',
                [sequelize.fn('COUNT', sequelize.col('Pedido.id')), 'total_entregas']
            ],
            where: {
                status: 'ENTREGUE',
                entregador_id: { [Op.not]: null }
            },
            include: [
                { model: Usuario, as: 'entregador', attributes: ['nome'] }
            ],
            group: ['entregador_id', 'entregador.id', 'entregador.nome'],
            order: [[sequelize.col('total_entregas'), 'DESC']]
        });

        return res.json(ranking);
    } catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Erro ao gerar ranking de entregadores.' });
    }
};

exports.pedidosDia = async (req, res) => {
    try {
        const hoje = new Date();
        hoje.setHours(0, 0, 0, 0);

        const pedidos = await Pedido.findAll({
            where: {
                data_pedido: { [Op.gte]: hoje }
            },
            include: [
                { model: Usuario, as: 'entregador', attributes: ['nome'] }
            ]
        });

        return res.json(pedidos);
    } catch (error) {
        res.status(500).json({ error: 'Erro ao listar pedidos do dia.' });
    }
};
