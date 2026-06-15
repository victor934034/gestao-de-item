const supabase = require('../config/supabase');
const { gerarPdfEstoque } = require('../services/pdfExportService');
const { parseEstoquePdf } = require('../services/pdfParserService');

exports.listarEstoque = async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('estoque')
            .select('*')
            .order('id', { ascending: false });

        if (error) throw error;
        res.json(data);
    } catch (error) {
        console.error('Erro ao listar estoque:', error);
        res.status(500).json({ error: 'Erro ao listar estoque.' });
    }
};

exports.detalhesEstoque = async (req, res) => {
    try {
        const { id } = req.params;
        const { data, error } = await supabase
            .from('estoque')
            .select('*')
            .eq('id', id)
            .single();

        if (error) throw error;
        if (!data) return res.status(404).json({ error: 'Item não encontrado.' });

        res.json(data);
    } catch (error) {
        console.error('Erro ao buscar item no estoque:', error);
        res.status(500).json({ error: 'Erro ao buscar item.' });
    }
};

exports.criarItemEstoque = async (req, res) => {
    try {
        // Barcode integration as per Integration Guide
        const { nome, quantidade, modo_estocagem, custo, preco_venda, barcode } = req.body;
        let foto_url = null;

        if (req.file) {
            // Upload to Supabase Storage
            const fileExt = req.file.originalname.split('.').pop();
            const fileName = `${Date.now()}-${Math.random()}.${fileExt}`;
            const filePath = `${fileName}`;

            const { data: storageData, error: storageError } = await supabase.storage
                .from('estoque_imagens')
                .upload(filePath, req.file.buffer, {
                    contentType: req.file.mimetype,
                });

            if (storageError) throw storageError;

            const { data: publicUrlData } = supabase.storage
                .from('estoque_imagens')
                .getPublicUrl(filePath);

            foto_url = publicUrlData.publicUrl;
        }

        const { data, error } = await supabase
            .from('estoque')
            .insert([{
                nome,
                quantidade: parseFloat(quantidade) || 0,
                modo_estocagem,
                custo: parseFloat(custo) || 0,
                preco_venda: parseFloat(preco_venda) || 0,
                foto_url,
                barcode: barcode || null
            }])
            .select();

        if (error) throw error;
        res.status(201).json(data[0]);
    } catch (error) {
        console.error('Erro ao criar item no estoque:', error);
        res.status(500).json({ error: 'Erro ao criar item.', details: error.message });
    }
};

exports.atualizarItemEstoque = async (req, res) => {
    try {
        const { id } = req.params;
        const { nome, quantidade, modo_estocagem, custo, preco_venda, barcode } = req.body;

        let updateData = {
            nome,
            quantidade: parseFloat(quantidade) || 0,
            modo_estocagem,
            custo: parseFloat(custo) || 0,
            preco_venda: parseFloat(preco_venda) || 0,
            barcode: barcode || null // Standardized to null for consistency
        };

        if (req.file) {
            const fileExt = req.file.originalname.split('.').pop();
            const fileName = `${Date.now()}-${Math.random()}.${fileExt}`;
            const filePath = `${fileName}`;

            const { data: storageData, error: storageError } = await supabase.storage
                .from('estoque_imagens')
                .upload(filePath, req.file.buffer, {
                    contentType: req.file.mimetype,
                });

            if (storageError) throw storageError;

            const { data: publicUrlData } = supabase.storage
                .from('estoque_imagens')
                .getPublicUrl(filePath);

            updateData.foto_url = publicUrlData.publicUrl;
        }

        const { data, error } = await supabase
            .from('estoque')
            .update(updateData)
            .eq('id', id)
            .select();

        if (error) throw error;
        res.json(data[0]);
    } catch (error) {
        console.error('Erro ao atualizar item do estoque:', error);
        res.status(500).json({ error: 'Erro ao atualizar item.', details: error.message });
    }
};

exports.excluirItemEstoque = async (req, res) => {
    try {
        const { id } = req.params;
        const { error } = await supabase
            .from('estoque')
            .delete()
            .eq('id', id);

        if (error) throw error;
        res.json({ message: 'Item excluído com sucesso.' });
    } catch (error) {
        res.status(500).json({ error: 'Erro ao excluir item.' });
    }
};

exports.exportarPdf = async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('estoque')
            .select('*')
            .order('nome', { ascending: true });

        if (error) throw error;

        const pdfBytes = await gerarPdfEstoque(data);

        res.setHeader('Content-Type', 'application/pdf');
        res.setHeader('Content-Disposition', 'attachment; filename=estoque.pdf');
        res.send(Buffer.from(pdfBytes));
    } catch (error) {
        console.error('Erro ao exportar PDF do estoque:', error);
        res.status(500).json({ error: 'Erro ao exportar PDF.' });
    }
};

exports.importarPdf = async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ error: 'Nenhum arquivo PDF enviado.' });
        }
        const items = await parseEstoquePdf(req.file.path);
        return res.json(items);
    } catch (error) {
        console.error('Erro ao processar PDF de estoque:', error);
        res.status(500).json({ error: 'Erro ao processar PDF.', details: error.message });
    }
};
exports.batchCriarItens = async (req, res) => {
    try {
        console.log('--- BATCH IMPORT START (v2.1) ---');
        const items = req.body;
        if (!Array.isArray(items) || items.length === 0) {
            console.error('Lote vazio ou inválido:', items);
            return res.status(400).json({ error: 'Nenhum item enviado.' });
        }

        console.log(`Processando lote de ${items.length} itens.`);

        // 1. Gera o array de itens validados
        const allItems = items.map((item, index) => {
            const qty = parseFloat(item.quantidade);
            const cst = parseFloat(item.custo);
            const vnd = parseFloat(item.preco_venda);

            // Barcode field included in batch processing

            return {
                nome: (item.nome || `Produto Sem Nome ${index}`).trim(),
                quantidade: isNaN(qty) ? 0 : qty,
                modo_estocagem: item.modo_estocagem || 'un',
                custo: isNaN(cst) ? 0 : cst,
                preco_venda: isNaN(vnd) ? 0 : vnd,
                barcode: item.barcode || null
            };
        });

        // 2. Deduplica pelo NOME (mantém apenas a primeira ocorrência no lote)
        const uniqueItemsMap = new Map();
        allItems.forEach(item => {
            if (!uniqueItemsMap.has(item.nome.toLowerCase())) {
                uniqueItemsMap.set(item.nome.toLowerCase(), item);
            }
        });
        const itemsToUpsert = Array.from(uniqueItemsMap.values());

        console.log(`Deduplicado de ${allItems.length} para ${itemsToUpsert.length} itens únicos no lote.`);
        console.log('Exemplo do payload:', JSON.stringify(itemsToUpsert.slice(0, 1), null, 2));

        // 3. Usa UPSERT baseado na coluna 'nome'
        // IMPORTANTE: Requer restrição de UNIQUE na coluna 'nome' no banco.
        const { data, error } = await supabase
            .from('estoque')
            .upsert(itemsToUpsert, { onConflict: 'nome' })
            .select();

        if (error) {
            console.error('Erro no UPSERT Supabase:', error);
            throw error;
        }

        console.log('Lote processado com sucesso. Itens:', data?.length);
        res.status(201).json({
            message: 'Itens importados/atualizados com sucesso',
            count: data?.length,
            data: data
        });
    } catch (error) {
        console.error('CRITICAL: Erro ao salvar itens em lote:', error);
        res.status(500).json({
            error: 'Erro interno ao salvar lote no banco.',
            details: error.message || error.details,
            message: error.message,
            code: error.code || error.statusCode,
            hint: error.hint,
            stack: process.env.NODE_ENV === 'development' ? error.stack : undefined
        });
    }
};
