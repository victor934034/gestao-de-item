const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const supabaseUrl = process.env.SUPABASE_URL || 'https://dummy.supabase.co';
const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_KEY || 'dummy_key';

if (supabaseUrl === 'https://dummy.supabase.co') {
    console.warn('⚠️  Supabase URL ou Key não foram definidos no .env!');
}

// Node < 22 não tem WebSocket nativo — passa o pacote "ws" como transport
const wsLib = (() => { try { return require('ws'); } catch { return undefined; } })();

const supabase = createClient(supabaseUrl, supabaseKey, {
    auth: { persistSession: false },
    realtime: wsLib ? { transport: wsLib } : {}
});

module.exports = supabase;
