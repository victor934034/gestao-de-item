const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const supabaseUrl = 'https://rhgmitrybhmwwihznopj.supabase.co';
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJoZ21pdHJ5Ymhtd3dpaHpub3BqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkyMTMwNjcsImV4cCI6MjA4NDc4OTA2N30.hCu-znNbVmvuGujepYkKMuDHX28pC69YzZ3-zYMHU7c';
const supabase = createClient(supabaseUrl, supabaseKey);

async function checkTable() {
    try {
        const { data, error } = await supabase
            .from('transacoes')
            .select('*')
            .limit(1);

        if (error) {
            console.log('Table transacoes does NOT exist or is not accessible:', error.message);
        } else {
            console.log('Table transacoes EXISTS!');
        }
    } catch (e) {
        console.log('Exception:', e.message);
    }
}

checkTable();
