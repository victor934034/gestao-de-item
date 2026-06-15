const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const SessaoRevogada = sequelize.define('SessaoRevogada', {
    id: {
        type: DataTypes.INTEGER,
        autoIncrement: true,
        primaryKey: true,
    },
    token_jti: {
        type: DataTypes.STRING(255),
        allowNull: false,
        unique: true,
    },
    revogado_em: {
        type: DataTypes.DATE,
        defaultValue: DataTypes.NOW,
    }
}, {
    tableName: 'sessoes_revogadas',
    timestamps: false,
});

module.exports = SessaoRevogada;
