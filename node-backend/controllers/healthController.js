const { makeRequest } = require('../utils/httpClient');

/**
 * Health check endpoint
 */
const getHealth = async (req, res) => {
  try {
    // Check Java backend health
    const javaHealth = await makeRequest('/health');
    res.json({ 
      status: 'ok', 
      message: 'Node.js backend is running',
      javaBackend: javaHealth
    });
  } catch (error) {
    res.status(503).json({ 
      status: 'error', 
      message: 'Node.js backend is running but Java backend is unavailable',
      error: error.message
    });
  }
};

module.exports = {
  getHealth
};
