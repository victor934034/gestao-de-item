const express = require('express');
const router = express.Router();

// NOTE: Hardcoded for now. 
// Can be moved to database (Supabase/PostgreSQL) later.
// "latestVersion" is the version code of the app that should be prompt for an update
// "minVersion" is the minimum version code the app requires to function without forcing update
// "apkUrl" is the direct URL to download the new APK
router.get('/', (req, res) => {
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

module.exports = router;
