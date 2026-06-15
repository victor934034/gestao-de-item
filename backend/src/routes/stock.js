// Rotas do app Gestão de Items — Supabase (produtos + transações + upload)
const express = require('express');
const router = express.Router();
const multer = require('multer');
const path = require('path');
const supabase = require('../config/supabase');

const upload = multer({
    storage: multer.memoryStorage(),
    limits: { fileSize: 5 * 1024 * 1024 }
});

// Compatibility routes for "Gestão de Items" App
// These routes follow the structure expected by the Android app's Repository

router.get('/status', async (req, res) => {
    try {
        const { data, error, count } = await supabase
            .from('estoque')
            .select('*', { count: 'exact', head: true });

        if (error) throw error;

        res.json({
            initialized: true,
            table_accessible: true,
            count: count,
            success: true
        });
    } catch (error) {
        res.status(500).json({
            initialized: true,
            table_accessible: false,
            error: error.message,
            success: false
        });
    }
});

router.get('/products', async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('estoque')
            .select('*')
            .order('nome');

        if (error) throw error;

        res.json({
            success: true,
            products: data
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

router.patch('/products/:id/quantity', async (req, res) => {
    let { id } = req.params;
    // Clean ID: "14.0" -> "14"
    if (id && id.includes('.')) id = id.split('.')[0];

    const { quantidade } = req.body;
    console.log(`[STOCK] PATCH quantity for ID: ${id}, New Quantity: ${quantidade}`);

    try {
        const { data, error } = await supabase
            .from('estoque')
            .update({ quantidade })
            .eq('id', id)
            .select()
            .single();

        if (error) {
            console.error(`[STOCK] PATCH Error: ${error.message}`);
            throw error;
        }

        console.log(`[STOCK] PATCH Success for ID: ${id}`);
        res.json({
            success: true,
            product: data
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message,
            details: error
        });
    }
});

router.post('/products', async (req, res) => {
    const product = req.body;
    console.log(`[STOCK] POST new product: ${product.nome}`);
    // Remove ID if provided to let Supabase generate it
    delete product.id;

    try {
        const { data, error } = await supabase
            .from('estoque')
            .insert([product])
            .select()
            .single();

        if (error) {
            console.error(`[STOCK] POST Error: ${error.message}`);
            throw error;
        }

        console.log(`[STOCK] POST Success, New ID: ${data.id}`);
        res.json({
            success: true,
            product: data
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message,
            details: error
        });
    }
});

router.put('/products/:id', async (req, res) => {
    let { id } = req.params;
    // Clean ID: "14.0" -> "14"
    if (id && id.includes('.')) id = id.split('.')[0];

    const product = req.body;
    console.log(`[STOCK] PUT update product ID: ${id}`);
    delete product.id; // Ensure we don't try to update the ID

    try {
        const { data, error } = await supabase
            .from('estoque')
            .update(product)
            .eq('id', id)
            .select()
            .single();

        if (error) {
            console.error(`[STOCK] PUT Error: ${error.message}`);
            throw error;
        }

        console.log(`[STOCK] PUT Success for ID: ${id}`);
        res.json({
            success: true,
            product: data
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message,
            details: error
        });
    }
});

router.delete('/products/:id', async (req, res) => {
    const { id } = req.params;

    try {
        const { error } = await supabase
            .from('estoque')
            .delete()
            .eq('id', id);

        if (error) throw error;

        res.json({
            success: true
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

router.get('/version', (req, res) => {
    const baseUrl = process.env.BACKEND_URL || 'http://localhost:3000';
    res.json({
        success: true,
        version: {
            latestVersion: process.env.APP_VERSION || '1.0.2',
            minVersion: '1.0.0',
            apkUrl: `${baseUrl}/uploads/apk/app-release.apk`
        }
    });
});

// ── Upload de imagem → Supabase Storage ──────────────────────────────────────
router.post('/upload', upload.single('image'), async (req, res) => {
    try {
        if (!req.file) return res.status(400).json({ success: false, error: 'Nenhum arquivo enviado' });

        const fileExt = path.extname(req.file.originalname);
        const fileName = `${Date.now()}-${Math.round(Math.random() * 1e9)}${fileExt}`;
        const filePath = `products/${fileName}`;

        const { error } = await supabase.storage
            .from('product-images')
            .upload(filePath, req.file.buffer, { contentType: req.file.mimetype, upsert: true });
        if (error) throw error;

        const { data: { publicUrl } } = supabase.storage.from('product-images').getPublicUrl(filePath);
        res.json({ success: true, imageUrl: publicUrl });
    } catch (err) {
        console.error('[STOCK] Upload error:', err);
        res.status(500).json({ success: false, error: err.message });
    }
});

// Transactions Endpoints
router.get('/transactions', async (req, res) => {
    try {
        const { data, error } = await supabase
            .from('transacoes')
            .select('*')
            .order('data_hora', { ascending: false })
            .limit(50);

        if (error) throw error;

        res.json({
            success: true,
            transactions: data
        });
    } catch (error) {
        console.error('[STOCK] Get Transactions Error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

router.post('/transactions', async (req, res) => {
    const transaction = req.body;
    console.log(`[STOCK] POST new transaction for item: ${transaction.item_name}`);

    try {
        const { data, error } = await supabase
            .from('transacoes')
            .insert([transaction])
            .select()
            .single();

        if (error) throw error;

        res.json({
            success: true,
            transaction: data
        });
    } catch (error) {
        console.error('[STOCK] Post Transaction Error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

module.exports = router;
