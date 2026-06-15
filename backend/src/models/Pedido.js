const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');
const Usuario = require('./Usuario');

const Pedido = sequelize.define('Pedido', {
    id: {
        type: DataTypes.INTEGER,
        autoIncrement: true,
        primaryKey: true,
    },
    numero_pedido: {
        type: DataTypes.STRING(20),
        allowNull: false,
        unique: true,
    },
    data_pedido: {
        type: DataTypes.DATE,
        allowNull: false,
    },
    data_entrega_inicio: DataTypes.DATEONLY,
    data_saida: DataTypes.DATEONLY,
    hora_prevista: DataTypes.TIME,

    data_entrega_programada: DataTypes.DATEONLY,
    hora_entrega_programada: DataTypes.TIME,

    nome_cliente: DataTypes.STRING(150),
    telefone_cliente: DataTypes.STRING(20),
    email_cliente: DataTypes.STRING(150),
    celular_cliente: DataTypes.STRING(20),

    endereco: DataTypes.STRING(200),
    numero_end: DataTypes.STRING(20),
    complemento: DataTypes.STRING(100),
    bairro: DataTypes.STRING(100),
    cidade: DataTypes.STRING(100),
    estado: DataTypes.CHAR(2),
    cep: DataTypes.STRING(10),
    endereco_completo: DataTypes.TEXT,
    observacao_endereco: DataTypes.TEXT,

    total_itens: {
        type: DataTypes.DECIMAL(10, 2),
        defaultValue: 0.00,
    },
    total_liquido: {
        type: DataTypes.DECIMAL(10, 2),
        defaultValue: 0.00,
    },
    forma_pagamento: DataTypes.STRING(30),
    vencimento: DataTypes.DATEONLY,

    status: {
        type: DataTypes.ENUM('PENDENTE', 'EM_ROTA', 'ENTREGUE', 'CANCELADO'),
        defaultValue: 'PENDENTE',
    },
    entregador_id: {
        type: DataTypes.INTEGER,
        references: {
            model: 'usuarios',
            key: 'id'
        }
    },
    observacoes: DataTypes.TEXT,
    arquivo_pdf_path: DataTypes.STRING(500),
    criado_por: {
        type: DataTypes.INTEGER,
        references: {
            model: 'usuarios',
            key: 'id'
        }
    },
}, {
    tableName: 'pedidos',
    timestamps: true,
    createdAt: 'criado_em',
    updatedAt: 'atualizado_em',
    indexes: [
        { fields: ['status'] },
        { fields: ['entregador_id'] },
        { fields: ['data_pedido'] }
    ]
});

Pedido.belongsTo(Usuario, { foreignKey: 'entregador_id', as: 'entregador' });
Pedido.belongsTo(Usuario, { foreignKey: 'criado_por', as: 'criador' });

module.exports = Pedido;
