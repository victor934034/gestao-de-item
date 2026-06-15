const jwt = require('jsonwebtoken');
require('dotenv').config();

const JWT_SECRET = process.env.JWT_SECRET || 'conexaobr277_secret_key_2026';
const JWT_EXPIRATION = process.env.JWT_EXPIRATION || '24h';

module.exports = {
    sign: (payload) => {
        return jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRATION });
    },
    verify: (token) => {
        return jwt.verify(token, JWT_SECRET);
    }
};
