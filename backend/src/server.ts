import { createApp } from './app';
import { config } from './config';
import { prisma } from './config/prisma';
import { redis } from './config/redis';

const app = createApp();

const startServer = async () => {
  try {
    // Connect to database
    await prisma.$connect();
    console.log('✅ PostgreSQL connected via Prisma');

    // Connect to Redis
    await redis.connect().catch(() => {
      console.warn('⚠️ Running Redis in lazy/fallback mode');
    });

    app.listen(config.port, () => {
      console.log(`🚀 VidLynk Server running on port ${config.port} [${config.env}]`);
      console.log(`📡 Deep link base URL: ${config.deepLinkBaseUrl}`);
    });
  } catch (error) {
    console.error('❌ Failed to start server:', error);
    process.exit(1);
  }
};

startServer();
