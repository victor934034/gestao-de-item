const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');

const Usuario = sequelize.define('Usuario', {
    id: {
        type: DataTypes.INTEGER,
        autoIncrement: true,
        primaryKey: true,
    },
    nome: {
        type: DataTypes.STRING(100),
        allowNull: false,
    },
    email: {
        type: DataTypes.STRING(150),
        allowNull: false,
        unique: true,
    },
    senha_hash: {
        type: DataTypes.STRING(255),
        allowNull: false,
    },
    perfil: {
        type: DataTypes.ENUM('SUPER_ADM', 'ADM', 'ENTREGADOR'),
        allowNull: false,
        defaultValue: 'ENTREGADOR',
    },
    ativo: {
        type: DataTypes.BOOLEAN,
        allowNull: false,
        defaultValue: true,
    },
}, {
    tableName: 'usuarios',
    timestamps: true,
    createdAt: 'criado_em',
    updatedAt: 'atualizado_em',
});

module.exports = Usuario;
