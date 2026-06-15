const { Pedido, ItemPedido, HistoricoStatus, Usuario, sequelize } = require('../models');
const { parsePedidoPdf } = require('../services/pdfParserService');
const socketService = require('../services/socketService');
const supabase = require('../config/supabase');
const { Op } = require('sequelize');

exports.listarPedidos = async (req, res) => {
    try {
        const { status, busca, dataInicio, dataFim } = req.query;

        const where = {};

        // Se for entregador, ele vê todos os pedidos
        if (req.usuario.perfil === 'ENTREGADOR') {
            // Removemos o filtro que excluía ENTREGUE para que ele veja seu histórico
        } else if (status) {
            where.status = status;
        }

        const pedidos = await Pedido.findAll({
            where,
            attributes: [
                'id', 'numero_pedido', 'data_pedido', 'nome_cliente', 'status',
                'endereco', 'numero_end', 'bairro', 'cidade', 'total_liquido',
                'data_entrega_programada', 'hora_entrega_programada', 'observacao_endereco', 'total_itens'
            ],
            include: [
                { model: Usuario, as: 'entregador', attributes: ['id', 'nome'] }
            ],
            order: [['data_pedido', 'DESC']]
        });

        return res.json(pedidos);
    } catch (error) {
        console.error('Erro ao listar pedidos:', error);
        res.status(500).json({ error: 'Erro ao buscar pedidos.', details: error.message });
    }
};

exports.detalhesPedido = async (req, res) => {
    try {
        const pedido = await Pedido.findByPk(req.params.id, {
            include: [
                { model: ItemPedido, as: 'itens' },
                { model: Usuario, as: 'entregador', attributes: ['id', 'nome'] },
                { model: HistoricoStatus, as: 'historicos', include: [{ model: Usuario, as: 'autor', attributes: ['nome'] }] }
            ]
        });

        if (!pedido) return res.status(404).json({ error: 'Pedido não encontrado.' });

        // Regra de negócios para ENTREGADOR ver o pedido (Seu próprio ou Pendente)
        if (req.usuario.perfil === 'ENTREGADOR' && pedido.entregador_id !== req.usuario.id && pedido.status !== 'PENDENTE') {
            return res.status(403).json({ error: 'Acesso negado a este pedido.' });
        }

        return res.json(pedido);
    } catch (error) {
        console.error('Erro ao buscar detalhes do pedido:', error);
        res.status(500).json({ error: 'Erro ao buscar detalhes do pedido.', details: error.message });
    }
};

exports.criarPedido = async (req, res) => {
    const t = await sequelize.transaction();
    try {
        const dadosPedido = req.body;
        dadosPedido.criado_por = req.usuario.id;

        const novoPedido = await Pedido.create(dadosPedido, { transaction: t });

        if (dadosPedido.itens && dadosPedido.itens.length > 0) {
            const itens = dadosPedido.itens.map(i => ({ ...i, pedido_id: novoPedido.id }));
            await ItemPedido.bulkCreate(itens, { transaction: t });

            // Atualiza o total_itens no pedido
            const totalItens = dadosPedido.itens.reduce((sum, item) => sum + (parseFloat(item.quantidade) || 0), 0);
            await novoPedido.update({ total_itens: totalItens }, { transaction: t });

            // Subtrai do estoque no Supabase
            try {
                for (let item of dadosPedido.itens) {
                    const itemName = item.descricao;
                    const itemQtd = parseFloat(item.quantidade) || 0;

                    // Busca o item no estoque pelo nome exato ou código de barras (Unified Lookup)
                    const { data: estoqueData } = await supabase
                        .from('estoque')
                        .select('id, quantidade')
                        .or(`nome.ilike."${itemName}",barcode.eq."${itemName}"`)
                        .limit(1)
                        .single();

                    if (estoqueData) {
                        const novaQtd = estoqueData.quantidade - itemQtd;
                        await supabase
                            .from('estoque')
                            .update({ quantidade: novaQtd })
                            .eq('id', estoqueData.id);
                    }
                }
            } catch (estoqueError) {
                console.error('Erro ao atualizar estoque:', estoqueError.message);
                // Não bloquear a criação do pedido se o estoque falhar, mas logar o erro
            }
        }

        await HistoricoStatus.create({
            pedido_id: novoPedido.id,
            status_para: novoPedido.status,
            alterado_por: req.usuario.id,
            observacao: 'Pedido criado.'
        }, { transaction: t });

        await t.commit();

        socketService.getIO().emit('pedido:criado', novoPedido);

        return res.status(201).json(novoPedido);
    } catch (error) {
        console.error('Erro ao criar pedido:', error);
        await t.rollback();
        res.status(500).json({ error: 'Erro ao criar pedido.' });
    }
};

exports.importarPdf = async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'Nenhum arquivo PDF enviado.' });
        }
        const pdfPath = req.file.path;
        const extractedData = await parsePedidoPdf(pdfPath);

        extractedData.arquivo_pdf_path = req.file.filename;

        return res.json(extractedData);
    } catch (error) {
        console.error('Erro ao processar PDF:', error);
        res.status(500).json({ error: 'Erro ao processar PDF.', details: error.message });
    }
};

exports.importarLote = async (req, res) => {
    try {
        if (!req.files || !req.files.pdf || !req.files.csv) {
            return res.status(400).json({ error: 'É necessário enviar os arquivos PDF e CSV.' });
        }

        const pdfFile = req.files.pdf[0];
        const csvFile = req.files.csv[0];

        // 1. Processar PDF
        const pdfData = await parsePedidoPdf(pdfFile.path);

        // 2. Processar CSV (Puxar buffer para string e quebrar em linhas)
        const fs = require('fs');
        const buffer = fs.readFileSync(csvFile.path);

        // Decodificar Windows-1252 (Latin-1) manualmente para lidar com caracteres brasileiros
        let csvString = '';
        for (let i = 0; i < buffer.length; i++) {
            const b = buffer[i];
            if (b < 128) csvString += String.fromCharCode(b);
            else if (b === 0xe3) csvString += 'ã';
            else if (b === 0xf5) csvString += 'õ';
            else if (b === 0xe7) csvString += 'ç';
            else if (b === 0xed) csvString += 'í';
            else if (b === 0xe1) csvString += 'á';
            else if (b === 0xe9) csvString += 'é';
            else if (b === 0xfa) csvString += 'ú';
            else csvString += String.fromCharCode(b); // Fallback básico
        }

        const csvLines = csvString.split(/\r?\n/);
        const addressMap = new Map();

        csvLines.forEach((line, index) => {
            if (index === 0 || !line.trim()) return;

            // Parser robusto char-a-char para lidar com aspas e espaços múltiplos
            const cols = [];
            let current = '';
            let inQuotes = false;
            for (let i = 0; i < line.length; i++) {
                const char = line[i];
                if (char === '"') inQuotes = !inQuotes;
                else if (char === ' ' && !inQuotes) {
                    if (current.trim() || (i > 0 && line[i - 1] === '"')) {
                        cols.push(current.trim());
                        current = '';
                    }
                } else current += char;
            }
            if (current.trim()) cols.push(current.trim());

            if (cols.length >= 8) {
                const nf = cols[2]?.trim();
                const cliente = cols[6]?.trim();
                const municipio = cols.length > 15 ? cols[8]?.trim() : cols[7]?.trim();

                let logradouro = '';
                let bairro = cols[7]?.trim();

                // Localizar rua dinamicamente (pode estar em colunas diferentes)
                const addrIdx = cols.findIndex((c, i) => i > 8 && (
                    c.toUpperCase().startsWith('RUA ') ||
                    c.toUpperCase().startsWith('R ') ||
                    c.toUpperCase().startsWith('AV ') ||
                    c.toUpperCase().startsWith('AVENIDA ') ||
                    c.toUpperCase().startsWith('RODOVIA ')
                ));

                if (addrIdx !== -1) {
                    logradouro = cols[addrIdx];
                    if (bairro === municipio) bairro = '';
                } else if (cols.length > 13) {
                    // Fallback se não achou prefixo (geralmente posição 13 ou 16)
                    logradouro = cols[13] || cols[16] || '';
                }

                if (nf) {
                    const nfNormalizada = nf.replace(/^0+/, '');
                    addressMap.set(nfNormalizada, { cliente, bairro, municipio, logradouro });
                }
                if (cliente) {
                    addressMap.set(cliente.toUpperCase(), { nf, bairro, municipio, logradouro });
                }
            }
        });

        // 3. Mesclar Dados (Somente se for Multi (lote))
        if (pdfData.isMulti && pdfData.pedidos) {
            pdfData.pedidos = pdfData.pedidos.map(pedido => {
                const numeroPDF = (pedido.numeroPedido?.value || '').replace(/^0+/, '');
                const nomePDF = (pedido.nomeCliente?.value || '').toUpperCase();

                let addrInfo = addressMap.get(numeroPDF);
                if (!addrInfo) {
                    for (const [key, val] of addressMap.entries()) {
                        if (typeof key === 'string' && (key.includes(nomePDF) || nomePDF.includes(key))) {
                            addrInfo = val;
                            break;
                        }
                    }
                }

                if (addrInfo) {
                    let enderecoParsed = addrInfo.logradouro || '';
                    let numParsed = '';
                    if (enderecoParsed.includes(',')) {
                        const parts = enderecoParsed.split(',');
                        enderecoParsed = parts[0].trim();
                        numParsed = parts[1].trim();
                    }

                    if (!pedido.endereco) pedido.endereco = {};
                    pedido.endereco.endereco = enderecoParsed;
                    pedido.endereco.numero = numParsed || pedido.endereco.numero || 'S/N';
                    pedido.endereco.bairro = addrInfo.bairro;
                    pedido.cidade = addrInfo.municipio;
                    if (addrInfo.cliente) {
                        pedido.nomeCliente = { value: addrInfo.cliente, ...pedido.nomeCliente };
                    }
                }
                return pedido;
            });
        }

        return res.json(pdfData);

    } catch (error) {
        console.error('Erro ao importar lote PDF + CSV:', error);
        res.status(500).json({ error: 'Erro ao processar lote.', details: error.message });
    }
};

exports.editarPedido = async (req, res) => {
    const t = await sequelize.transaction();
    try {
        console.log('--- Iniciando Edição de Pedido ---');
        console.log('ID:', req.params.id);
        console.log('Body Keys:', Object.keys(req.body));

        const pedido = await Pedido.findByPk(req.params.id);
        if (!pedido) {
            console.log('Pedido não encontrado para o ID:', req.params.id);
            await t.rollback();
            return res.status(404).json({ error: 'Pedido não encontrado.' });
        }

        const statusDe = pedido.status;

        // Normalizar campos para evitar erros de tipo no banco
        const dadosUpdate = { ...req.body };
        if (dadosUpdate.entregador_id === "") dadosUpdate.entregador_id = null;
        if (dadosUpdate.vencimento === "") dadosUpdate.vencimento = null;
        if (dadosUpdate.total_liquido) {
            dadosUpdate.total_liquido = parseFloat(dadosUpdate.total_liquido.toString().replace(',', '.')) || 0;
        }

        console.log('Atualizando dados básicos do pedido...');
        await pedido.update(dadosUpdate, { transaction: t });

        if (req.body.itens) {
            console.log(`Limpando e recriando ${req.body.itens.length} itens...`);

            // --- LÓGICA DE ESTOQUE (DEVOLVER ANTIGOS) ---
            try {
                const itensAntigos = await ItemPedido.findAll({ where: { pedido_id: pedido.id }, transaction: t });
                for (let item of itensAntigos) {
                    const { data: estoqueData } = await supabase
                        .from('estoque')
                        .select('id, quantidade')
                        .or(`nome.ilike."${item.descricao}",barcode.eq."${item.descricao}"`) // Unified Lookup
                        .limit(1)
                        .single();

                    if (estoqueData) {
                        await supabase
                            .from('estoque')
                            .update({ quantidade: estoqueData.quantidade + parseFloat(item.quantidade) })
                            .eq('id', estoqueData.id);
                    }
                }
            } catch (err) {
                console.error('Erro ao devolver estoque na edição:', err.message);
            }
            // --------------------------------------------

            await ItemPedido.destroy({ where: { pedido_id: pedido.id }, transaction: t });

            // Strip IDs to avoid primary key conflicts after destroy
            const itens = req.body.itens.map(i => {
                const { id, idx, ...itemData } = i;

                // Converter quantidade para número válido (pode vir com vírgula do front)
                let qtd = itemData.quantidade;
                if (typeof qtd === 'string') {
                    qtd = parseFloat(qtd.replace(',', '.'));
                }

                return {
                    ...itemData,
                    quantidade: isNaN(qtd) ? 0 : qtd,
                    pedido_id: pedido.id
                };
            });

            await ItemPedido.bulkCreate(itens, { transaction: t });

            // Recalculate total_itens
            const totalItens = itens.reduce((acc, item) => acc + (parseFloat(item.quantidade) || 0), 0);
            await pedido.update({ total_itens: totalItens }, { transaction: t });

            // --- LÓGICA DE ESTOQUE (SUBTRAIR NOVOS) ---
            try {
                for (let item of itens) {
                    const { data: estoqueData } = await supabase
                        .from('estoque')
                        .select('id, quantidade')
                        .or(`nome.ilike."${item.descricao}",barcode.eq."${item.descricao}"`)
                        .limit(1)
                        .single();

                    if (estoqueData) {
                        await supabase
                            .from('estoque')
                            .update({ quantidade: estoqueData.quantidade - parseFloat(item.quantidade) })
                            .eq('id', estoqueData.id);
                    }
                }
            } catch (err) {
                console.error('Erro ao subtrair estoque na edição:', err.message);
            }
            // ------------------------------------------
        }

        // Registrar no histórico
        console.log('Registrando histórico...');
        await HistoricoStatus.create({
            pedido_id: pedido.id,
            status_de: statusDe,
            status_para: req.body.status || statusDe,
            alterado_por: req.usuario?.id || null, // Garantir que não quebra se req.usuario disparar undefined
            observacao: req.body.observacao_historico || 'Pedido editado via Painel Admin'
        }, { transaction: t });

        await t.commit();
        console.log('✅ Commit realizado com sucesso.');

        try {
            socketService.getIO().emit('pedido:atualizado', pedido);
        } catch (sockErr) {
            console.error('⚠️ Erro ao emitir via Socket:', sockErr.message);
        }

        return res.json({ message: 'Pedido atualizado.', pedido });
    } catch (error) {
        console.error('❌ ERRO AO EDITAR PEDIDO:', error);
        if (t && !t.finished) await t.rollback();
        res.status(500).json({
            error: 'Erro ao editar pedido.',
            details: error.message,
            stack: error.stack
        });
    }
};

exports.atualizarStatus = async (req, res) => {
    try {
        const { status, observacao } = req.body;
        const pedido = await Pedido.findByPk(req.params.id);

        if (!pedido) return res.status(404).json({ error: 'Pedido não encontrado.' });

        const statusDe = pedido.status;
        pedido.status = status;

        // --- LÓGICA DE ESTOQUE (CANCELAMENTO) ---
        if (status === 'CANCELADO' && statusDe !== 'CANCELADO') {
            try {
                const itens = await ItemPedido.findAll({ where: { pedido_id: pedido.id } });
                for (let item of itens) {
                    const { data: estoqueData } = await supabase
                        .from('estoque')
                        .select('id, quantidade')
                        .or(`nome.ilike."${item.descricao}",barcode.eq."${item.descricao}"`)
                        .limit(1)
                        .single();

                    if (estoqueData) {
                        await supabase
                            .from('estoque')
                            .update({ quantidade: estoqueData.quantidade + parseFloat(item.quantidade) })
                            .eq('id', estoqueData.id);
                    }
                }
            } catch (err) {
                console.error('Erro ao devolver estoque no cancelamento:', err.message);
            }
        }
        // Se voltar de Cancelado para outro status (reativar pedido pendente)
        else if (statusDe === 'CANCELADO' && status !== 'CANCELADO') {
            try {
                const itens = await ItemPedido.findAll({ where: { pedido_id: pedido.id } });
                for (let item of itens) {
                    const { data: estoqueData } = await supabase
                        .from('estoque')
                        .select('id, quantidade')
                        .or(`nome.ilike."${item.descricao}",barcode.eq."${item.descricao}"`)
                        .limit(1)
                        .single();

                    if (estoqueData) {
                        await supabase
                            .from('estoque')
                            .update({ quantidade: estoqueData.quantidade - parseFloat(item.quantidade) })
                            .eq('id', estoqueData.id);
                    }
                }
            } catch (err) {
                console.error('Erro ao subtrair estoque na reativação:', err.message);
            }
        }
        // ----------------------------------------

        // Se o entregador está assumindo um pedido pendente ou reatribuindo para si mesmo
        if (status === 'EM_ROTA' && req.usuario.perfil === 'ENTREGADOR') {
            pedido.entregador_id = req.usuario.id;
        }

        await pedido.save();

        await HistoricoStatus.create({
            pedido_id: pedido.id,
            status_de: statusDe,
            status_para: status,
            alterado_por: req.usuario.id,
            observacao: observacao || ''
        });

        socketService.getIO().emit('pedido:status', { pedidoId: pedido.id, status });
        socketService.getIO().emit('pedido:atualizado', pedido);

        return res.json({ message: 'Status atualizado com sucesso.', pedido });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao atualizar status.' });
    }
};

exports.excluirPedido = async (req, res) => {
    try {
        const pedido = await Pedido.findByPk(req.params.id);
        if (!pedido) return res.status(404).json({ error: 'Pedido não encontrado.' });

        // --- LÓGICA DE ESTOQUE (DEVOLVER ITENS) ---
        // Só devolvemos se o pedido não estiver cancelado (pois se tiver cancelado já devolvemos itens no status)
        if (pedido.status !== 'CANCELADO') {
            try {
                const itens = await ItemPedido.findAll({ where: { pedido_id: pedido.id } });
                for (let item of itens) {
                    const { data: estoqueData } = await supabase
                        .from('estoque')
                        .select('id, quantidade')
                        .or(`nome.ilike."${item.descricao}",barcode.eq."${item.descricao}"`)
                        .limit(1)
                        .single();

                    if (estoqueData) {
                        await supabase
                            .from('estoque')
                            .update({ quantidade: estoqueData.quantidade + parseFloat(item.quantidade) })
                            .eq('id', estoqueData.id);
                    }
                }
            } catch (err) {
                console.error('Erro ao devolver estoque na exclusão:', err.message);
            }
        }
        // -----------------------------------------

        await pedido.destroy();

        socketService.getIO().emit('pedido:excluido', { pedidoId: req.params.id });

        return res.json({ message: 'Pedido excluído com sucesso.' });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao excluir pedido.' });
    }
};

exports.pedidosEntregador = async (req, res) => {
    try {
        const { dataInicio, dataFim } = req.query;
        const uid = parseInt(req.params.uid);

        console.log(`[DEBUG_API] Buscando pedidos para UID: ${uid}. Query params:`, req.query);

        const where = {};

        if (dataInicio) {
            // Se vier apenas a data (YYYY-MM-DD), criamos o intervalo do dia todo
            const dateStr = dataInicio.split('T')[0];
            const dInicio = new Date(`${dateStr}T00:00:00`);
            const dFim = new Date(`${dateStr}T23:59:59`);

            where.data_pedido = {
                [Op.between]: [dInicio, dFim]
            };

            // Para o Calendário: Mostrar pendentes daquele dia + Todo o histórico deste entregador no dia
            where[Op.or] = [
                { status: 'PENDENTE' },
                { entregador_id: uid }
            ];
        } else {
            where[Op.or] = [
                { status: 'PENDENTE' },
                { entregador_id: uid }
            ];
        }

        const pedidos = await Pedido.findAll({
            where,
            attributes: [
                'id', 'numero_pedido', 'data_pedido', 'nome_cliente', 'status',
                'endereco', 'numero_end', 'bairro', 'cidade', 'total_liquido',
                'data_entrega_programada', 'hora_entrega_programada', 'observacao_endereco', 'total_itens'
            ],
            include: [{ model: Usuario, as: 'entregador', attributes: ['id', 'nome'] }],
            order: [['data_pedido', 'DESC']]
        });

        console.log(`[DEBUG_API] Encontrados ${pedidos.length} pedidos.`);
        return res.json(pedidos);
    } catch (error) {
        console.error('Erro ao buscar entregas:', error);
        res.status(500).json({ error: 'Erro ao buscar entregas.' });
    }
};
