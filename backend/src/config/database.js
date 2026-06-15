const { Sequelize } = require('sequelize');
require('dotenv').config();

const sequelize = new Sequelize(
  process.env.DB_NAME || 'entregaconexao',
  process.env.DB_USER || 'mysql',
  process.env.DB_PASSWORD || 'conexao2019',
  {
    host: process.env.DB_HOST || 'app_database',
    port: process.env.DB_PORT || 3306,
    dialect: 'mysql',
    logging: console.log,
  }
);

module.exports = sequelize;
