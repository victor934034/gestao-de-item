const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const supabaseUrl = process.env.SUPABASE_URL || 'https://dummy.supabase.co';
const supabaseKey = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_KEY || 'dummy_key';

if (supabaseUrl === 'https://dummy.supabase.co') {
    console.warn('⚠️  Supabase URL ou Key não foram definidos no .env!');
}

const supabase = createClient(supabaseUrl, supabaseKey, {
    auth: {
        persistSession: false
    }
});

module.exports = supabase;
