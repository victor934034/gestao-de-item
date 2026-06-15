const multer = require('multer');
const path = require('path');
const fs = require('fs');

const uploadDir = process.env.UPLOAD_DIR || './uploads/pdfs';

// Garante que o diretório exista
if (!fs.existsSync(uploadDir)) {
    fs.mkdirSync(uploadDir, { recursive: true });
}

const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, uploadDir);
    },
    filename: function (req, file, cb) {
        // uuidv4_timestamp.pdf
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, uniqueSuffix + path.extname(file.originalname));
    }
});

const fileFilter = (req, file, cb) => {
    const allowedMimeTypes = ['application/pdf', 'text/csv', 'application/vnd.ms-excel', 'text/plain'];
    if (allowedMimeTypes.includes(file.mimetype)) {
        cb(null, true);
    } else {
        cb(new Error('Apenas arquivos PDF e CSV são suportados! Formato recebido: ' + file.mimetype), false);
    }
};

const upload = multer({
    storage: storage,
    limits: {
        fileSize: parseInt(process.env.MAX_FILE_SIZE) || 10 * 1024 * 1024 // 10MB
    },
    fileFilter: fileFilter
});

module.exports = upload;
