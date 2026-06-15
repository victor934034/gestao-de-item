const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');
const Pedido = require('./Pedido');
const Usuario = require('./Usuario');

const HistoricoStatus = sequelize.define('HistoricoStatus', {
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
    status_de: DataTypes.STRING(20),
    status_para: {
        type: DataTypes.STRING(20),
        allowNull: false,
    },
    alterado_por: {
        type: DataTypes.INTEGER,
        references: {
            model: Usuario,
            key: 'id'
        }
    },
    observacao: DataTypes.TEXT,
    alterado_em: {
        type: DataTypes.DATE,
        defaultValue: DataTypes.NOW,
    },
}, {
    tableName: 'historico_status',
    timestamps: false,
});

Pedido.hasMany(HistoricoStatus, { foreignKey: 'pedido_id', as: 'historicos', onDelete: 'CASCADE' });
HistoricoStatus.belongsTo(Pedido, { foreignKey: 'pedido_id' });
HistoricoStatus.belongsTo(Usuario, { foreignKey: 'alterado_por', as: 'autor' });

module.exports = HistoricoStatus;
