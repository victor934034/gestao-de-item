const sequelize = require('../config/database');
const Usuario = require('./Usuario');
const PermissaoUsuario = require('./PermissaoUsuario');
const Pedido = require('./Pedido');
const ItemPedido = require('./ItemPedido');
const HistoricoStatus = require('./HistoricoStatus');
const SessaoRevogada = require('./SessaoRevogada');

module.exports = {
    sequelize,
    Usuario,
    PermissaoUsuario,
    Pedido,
    ItemPedido,
    HistoricoStatus,
    SessaoRevogada
};
