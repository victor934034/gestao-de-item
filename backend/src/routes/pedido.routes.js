const express = require('express');
const router = express.Router();
const pedidoController = require('../controllers/pedidoController');
const authMiddleware = require('../middleware/auth.middleware');
const permissionMiddleware = require('../middleware/permission.middleware');
const upload = require('../middleware/upload.middleware');

router.use(authMiddleware);

router.get('/', pedidoController.listarPedidos);
router.get('/:id', pedidoController.detalhesPedido);
router.get('/entregador/:uid', pedidoController.pedidosEntregador);

// Atualização de status: ADM, SUPER_ADM, e ENTREGADOR (se atribuído/com permissão)
router.patch('/:id/status', permissionMiddleware('atualizar_status_entrega'), pedidoController.atualizarStatus);

// Somente SUPER_ADM ou ADM com permissão podem criar/editar/excluir pedidos ou importar PDF
router.post('/', permissionMiddleware('criar_pedido'), pedidoController.criarPedido);
router.put('/:id', permissionMiddleware('editar_pedido'), pedidoController.editarPedido);
router.delete('/:id', permissionMiddleware('excluir_pedido'), pedidoController.excluirPedido);

// Rota de Upload de PDF
router.post('/importar-pdf', permissionMiddleware('importar_pdf'), upload.single('pdf'), pedidoController.importarPdf);

// Rota de Upload em Lote (PDF + CSV)
router.post('/importar-lote', permissionMiddleware('importar_pdf'), upload.fields([{ name: 'pdf', maxCount: 1 }, { name: 'csv', maxCount: 1 }]), pedidoController.importarLote);

module.exports = router;
