const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const cors = require('cors');
const multer = require('multer');
const path = require('path');
require('dotenv').config();

const app = express();
const port = process.env.PORT || 3000;

// Configure Multer for memory storage
const upload = multer({
    storage: multer.memoryStorage(),
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB limit
    }
});

// Supabase configuration
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_KEY;
const supabase = createClient(supabaseUrl, supabaseKey);

app.use(cors());
app.use(express.json());

// Serve static files (APKs for updates)
app.use('/public', express.static(path.join(__dirname, 'public')));

// Routes
app.get('/api/stock/status', async (req, res) => {
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

// Versioning Endpoint for App Updates
const APP_VERSION_INFO = {
    latestVersion: "1.0.2",
    minVersion: "1.0.0",
    apkUrl: "https://app-backend.zdc13k.easypanel.host/public/apk/app-release.apk"
};

app.get('/api/stock/version', (req, res) => {
    res.json({
        success: true,
        version: APP_VERSION_INFO
    });
});

app.get('/api/stock/products', async (req, res) => {
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

app.patch('/api/stock/products/:id/quantity', async (req, res) => {
    let { id } = req.params;
    // Clean ID: "14.0" -> "14"
    if (id && id.includes('.')) id = id.split('.')[0];

    const { quantidade } = req.body;
    console.log(`[BRIDGE] PATCH quantity for ID: ${id}, New Quantity: ${quantidade}`);

    try {
        const { data, error } = await supabase
            .from('estoque')
            .update({ quantidade })
            .eq('id', id)
            .select()
            .single();

        if (error) {
            console.error(`[BRIDGE] PATCH Error: ${error.message}`);
            throw error;
        }

        console.log(`[BRIDGE] PATCH Success for ID: ${id}`);
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

app.post('/api/stock/products', async (req, res) => {
    const product = req.body;
    console.log(`[BRIDGE] POST new product: ${product.nome}`);
    // Remove ID if provided to let Supabase generate it
    delete product.id;

    try {
        const { data, error } = await supabase
            .from('estoque')
            .insert([product])
            .select()
            .single();

        if (error) {
            console.error(`[BRIDGE] POST Error: ${error.message}`);
            throw error;
        }

        console.log(`[BRIDGE] POST Success, New ID: ${data.id}`);
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

app.put('/api/stock/products/:id', async (req, res) => {
    let { id } = req.params;
    // Clean ID: "14.0" -> "14"
    if (id && id.includes('.')) id = id.split('.')[0];

    const product = req.body;
    console.log(`[BRIDGE] PUT update product ID: ${id}`);
    delete product.id; // Ensure we don't try to update the ID

    try {
        const { data, error } = await supabase
            .from('estoque')
            .update(product)
            .eq('id', id)
            .select()
            .single();

        if (error) {
            console.error(`[BRIDGE] PUT Error: ${error.message}`);
            throw error;
        }

        console.log(`[BRIDGE] PUT Success for ID: ${id}`);
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

app.delete('/api/stock/products/:id', async (req, res) => {
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

// New Upload Endpoint
app.post('/api/stock/upload', upload.single('image'), async (req, res) => {
    try {
        if (!req.file) {
            return res.status(400).json({ success: false, error: 'No file uploaded' });
        }

        const file = req.file;
        const fileExt = path.extname(file.originalname);
        const fileName = `${Date.now()}-${Math.round(Math.random() * 1E9)}${fileExt}`;
        const filePath = `products/${fileName}`;

        const { data, error } = await supabase.storage
            .from('product-images')
            .upload(filePath, file.buffer, {
                contentType: file.mimetype,
                upsert: true
            });

        if (error) throw error;

        // Get public URL
        const { data: { publicUrl } } = supabase.storage
            .from('product-images')
            .getPublicUrl(filePath);

        res.json({
            success: true,
            imageUrl: publicUrl
        });
    } catch (error) {
        console.error('[BRIDGE] Upload Error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

// Transactions Endpoints
app.get('/api/stock/transactions', async (req, res) => {
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
        console.error('[BRIDGE] Get Transactions Error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

app.post('/api/stock/transactions', async (req, res) => {
    const transaction = req.body;
    console.log(`[BRIDGE] POST new transaction for item: ${transaction.item_name}`);

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
        console.error('[BRIDGE] Post Transaction Error:', error);
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

app.listen(port, () => {
    console.log(`Bridge server running on port ${port}`);
});
