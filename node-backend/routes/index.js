const express = require('express');
const router = express.Router();

const healthRoutes = require('./health');
const userRoutes = require('./users');
const taskRoutes = require('./tasks');
const statsRoutes = require('./stats');

// Mount routes
router.use('/', healthRoutes);
router.use('/api/users', userRoutes);
router.use('/api/tasks', taskRoutes);
router.use('/api/stats', statsRoutes);

module.exports = router;
