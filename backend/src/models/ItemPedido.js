const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');
const Pedido = require('./Pedido');

const ItemPedido = sequelize.define('ItemPedido', {
    id: {
        type: DataTypes.INTEGER,
        autoIncrement: true,
        primaryKey: true,
    },
    pedido_id: {
        type: DataTypes.INTEGER,
        allowNull: false,
        references: {
            model: Pedido,
            key: 'id'
        }
    },
    codigo: DataTypes.STRING(30),
    descricao: {
        type: DataTypes.STRING(300),
        allowNull: false,
    },
    quantidade: {
        type: DataTypes.DECIMAL(10, 2),
        allowNull: false,
    },
    unidade: DataTypes.STRING(10),
    valor_unitario: DataTypes.DECIMAL(10, 2),
    valor_total: DataTypes.DECIMAL(10, 2),
}, {
    tableName: 'itens_pedido',
    timestamps: false,
});

Pedido.hasMany(ItemPedido, { foreignKey: 'pedido_id', as: 'itens', onDelete: 'CASCADE' });
ItemPedido.belongsTo(Pedido, { foreignKey: 'pedido_id' });

module.exports = ItemPedido;
