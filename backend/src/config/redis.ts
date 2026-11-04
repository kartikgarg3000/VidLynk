import Redis from 'ioredis';
import { config } from './index';

export const redis = new Redis(config.redisUrl, {
  maxRetriesPerRequest: 3,
  retryStrategy(times) {
    const delay = Math.min(times * 100, 3000);
    return delay;
  },
  lazyConnect: true,
});

redis.on('connect', () => {
  console.log('⚡ Redis connected successfully');
});

redis.on('error', (err) => {
  console.warn('⚠️ Redis error, falling back to direct DB queries:', err.message);
});

