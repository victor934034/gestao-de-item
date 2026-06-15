const { DataTypes } = require('sequelize');
const sequelize = require('../config/database');
const Usuario = require('./Usuario');

const PermissaoUsuario = sequelize.define('PermissaoUsuario', {
    id: {
        type: DataTypes.INTEGER,
        autoIncrement: true,
        primaryKey: true,
    },
    usuario_id: {
        type: DataTypes.INTEGER,
        allowNull: false,
        references: {
            model: Usuario,
            key: 'id'
        }
    },
    permissao: {
        type: DataTypes.STRING(60),
        allowNull: false,
    },
    permitido: {
        type: DataTypes.BOOLEAN,
        allowNull: false,
        defaultValue: true,
    }
}, {
    tableName: 'permissoes_usuario',
    timestamps: false,
    indexes: [
        {
            unique: true,
            fields: ['usuario_id', 'permissao']
        }
    ]
});

Usuario.hasMany(PermissaoUsuario, { foreignKey: 'usuario_id', as: 'permissoes' });
PermissaoUsuario.belongsTo(Usuario, { foreignKey: 'usuario_id' });

module.exports = PermissaoUsuario;
