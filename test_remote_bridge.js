const https = require('https');

https.get('https://app-backend.zdc13k.easypanel.host/api/stock/status', (res) => {
    let data = '';
    res.on('data', (chunk) => { data += chunk; });
    res.on('end', () => {
        console.log('Status Response:', data);
    });
}).on('error', (err) => {
    console.error('Error:', err.message);
});

https.get('https://app-backend.zdc13k.easypanel.host/api/stock/products', (res) => {
    let data = '';
    res.on('data', (chunk) => { data += chunk; });
    res.on('end', () => {
        try {
            const json = JSON.parse(data);
            console.log('Products Count:', json.products ? json.products.length : 'N/A');
            if (json.products && json.products.length > 0) {
                console.log('First Product:', json.products[0].nome);
            }
        } catch (e) {
            console.log('Raw Products Response (not JSON):', data.substring(0, 100));
        }
    });
}).on('error', (err) => {
    console.error('Error:', err.message);
});
