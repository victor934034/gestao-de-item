const { createClient } = require('@supabase/supabase-js');
require('dotenv').config();

const supabaseUrl = 'https://rhgmitrybhmwwihznopj.supabase.co';
const supabaseKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InJoZ21pdHJ5Ymhtd3dpaHpub3BqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjkyMTMwNjcsImV4cCI6MjA4NDc4OTA2N30.hCu-znNbVmvuGujepYkKMuDHX28pC69YzZ3-zYMHU7c';
const supabase = createClient(supabaseUrl, supabaseKey);

async function test() {
    console.log('Testing Supabase connection...');
    try {
        const { data, error, count } = await supabase
            .from('estoque')
            .select('*', { count: 'exact' });

        if (error) {
            console.error('Error fetching from estoque:', error);
            return;
        }

        console.log('Success! Count:', count);
        if (data.length > 0) {
            const firstItem = data[0];
            console.log('Attempting to update item ID:', firstItem.id);
            const newQty = (firstItem.quantidade || 0) + 1;

            const { data: updateData, error: updateError } = await supabase
                .from('estoque')
                .update({ quantidade: newQty })
                .eq('id', firstItem.id)
                .select();

            if (updateError) {
                console.error('Update Failed!', updateError);
            } else {
                console.log('Update Successful!', updateData);
            }
        }
    } catch (e) {
        console.error('Exception:', e.message);
    }
}

test();
