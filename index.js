const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const cors = require('cors');
require('dotenv').config();

const app = express();
const port = process.env.PORT || 3000;

// Supabase configuration
const supabaseUrl = process.env.SUPABASE_URL;
const supabaseKey = process.env.SUPABASE_KEY;
const supabase = createClient(supabaseUrl, supabaseKey);

app.use(cors());
app.use(express.json());

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
    const { id } = req.params;
    const { quantidade } = req.body;

    try {
        const { data, error } = await supabase
            .from('estoque')
            .update({ quantidade })
            .eq('id', id)
            .select()
            .single();

        if (error) throw error;

        res.json({
            success: true,
            product: data
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

app.post('/api/stock/products', async (req, res) => {
    const product = req.body;
    // Remove ID if provided to let Supabase generate it
    delete product.id;

    try {
        const { data, error } = await supabase
            .from('estoque')
            .insert([product])
            .select()
            .single();

        if (error) throw error;

        res.json({
            success: true,
            product: data
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
        });
    }
});

app.put('/api/stock/products/:id', async (req, res) => {
    const { id } = req.params;
    const product = req.body;
    delete product.id; // Ensure we don't try to update the ID

    try {
        const { data, error } = await supabase
            .from('estoque')
            .update(product)
            .eq('id', id)
            .select()
            .single();

        if (error) throw error;

        res.json({
            success: true,
            product: data
        });
    } catch (error) {
        res.status(500).json({
            success: false,
            error: error.message
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

app.listen(port, () => {
    console.log(`Bridge server running on port ${port}`);
});
