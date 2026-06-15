const express = require('express');
const router = express.Router();
const relatorioController = require('../controllers/relatorioController');
const authMiddleware = require('../middleware/auth.middleware');
const permissionMiddleware = require('../middleware/permission.middleware');

router.use(authMiddleware);
router.use(permissionMiddleware('ver_relatorios'));

router.get('/dashboard', relatorioController.dashboard);
router.get('/entregadores', relatorioController.rankingEntregadores);
router.get('/pedidos-dia', relatorioController.pedidosDia);

module.exports = router;
