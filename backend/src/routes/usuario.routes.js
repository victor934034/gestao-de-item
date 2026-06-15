const express = require('express');
const router = express.Router();
const usuarioController = require('../controllers/usuarioController');
const authMiddleware = require('../middleware/auth.middleware');
const permissionMiddleware = require('../middleware/permission.middleware');

// Todas as rotas de usuários exigem autenticação
router.use(authMiddleware);

// Rotas que o próprio usuário pode acessar
router.get('/stats', usuarioController.statsUsuario);
router.get('/:uid/stats', usuarioController.statsUsuario);
router.patch('/me/desativar', usuarioController.desativarPropriaConta);
router.delete('/me/excluir', usuarioController.excluirPropriaConta);

// Apenas SUPER_ADM e ADM com permissão podem gerenciar usuários
router.use(permissionMiddleware('gerenciar_usuarios'));

router.delete('/:id', usuarioController.excluirUsuario);
router.get('/', usuarioController.listarUsuarios);
router.post('/', usuarioController.criarUsuario);
router.get('/:id', usuarioController.detalhesUsuario);
router.put('/:id', usuarioController.editarUsuario);
router.patch('/:id/status', usuarioController.alterarStatus);
router.get('/:id/permissoes', usuarioController.listarPermissoes);
router.put('/:id/permissoes', usuarioController.atualizarPermissoes);
router.get('/:id/entregas', usuarioController.historicoEntregas);

module.exports = router;
