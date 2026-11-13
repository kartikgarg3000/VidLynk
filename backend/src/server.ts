import { createApp } from './app';
import { config } from './config';
import { prisma } from './config/prisma';
import { redis } from './config/redis';
import bcrypt from 'bcrypt';

const app = createApp();

async function autoSeedAdmin() {
  try {
    const adminCount = await prisma.admin.count();
    if (adminCount === 0) {
      const email = 'admin@vidlynk.app';
      const rawPassword = 'VidLynkAdmin2026!';
      const passwordHash = await bcrypt.hash(rawPassword, 10);

      await prisma.admin.create({
        data: {
          email,
          name: 'VidLynk Administrator',
          passwordHash,
        },
      });
      console.log('✅ Default admin automatically seeded: admin@vidlynk.app');
    }
  } catch (err: any) {
    console.warn('⚠️ Auto-seed check skipped:', err.message);
  }
}

const startServer = async () => {
  try {
    // Connect to database
    await prisma.$connect();
    console.log('✅ PostgreSQL connected via Prisma');

    // Automatically seed admin if database is fresh
    await autoSeedAdmin();

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
