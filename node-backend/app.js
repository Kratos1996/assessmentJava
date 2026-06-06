const express = require('express');
const cors = require('cors');
const config = require('./config');
const routes = require('./routes');
const { errorHandler, notFoundHandler } = require('./middleware/errorHandler');

const app = express();

// Middleware
app.use(cors()); // Enable CORS for all routes not allowed other domains to access the API
app.use(express.json()); // Parse JSON request bodies

// Routes
app.use(routes);

// Error handling
app.use(errorHandler);
app.use(notFoundHandler);

// Start server
app.listen(config.PORT, () => {
  console.log(`Node.js backend server running on http://localhost:${config.PORT}`);
  console.log(`Connecting to Java backend at ${config.JAVA_BACKEND_URL}`);
  console.log(`Health check: http://localhost:${config.PORT}/health`);
});

module.exports = app;
