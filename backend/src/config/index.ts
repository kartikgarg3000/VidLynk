import dotenv from 'dotenv';
dotenv.config();

export const config = {
  env: process.env.NODE_ENV || 'development',
  port: parseInt(process.env.PORT || '5000', 10),
  databaseUrl: process.env.DATABASE_URL || '',
  redisUrl: process.env.REDIS_URL || 'redis://localhost:6379',
  jwt: {
    secret: process.env.JWT_SECRET || 'vidlynk_default_jwt_secret',
    expiresIn: process.env.JWT_EXPIRES_IN || '7d',
  },
  r2: {
    accountId: process.env.R2_ACCOUNT_ID || '',
    accessKeyId: process.env.R2_ACCESS_KEY_ID || '',
    secretAccessKey: process.env.R2_SECRET_ACCESS_KEY || '',
    bucketName: process.env.R2_BUCKET_NAME || 'vidlynk-media',
    publicDomain: process.env.R2_PUBLIC_DOMAIN || 'https://cdn.vidlynk.app',
  },
  deepLinkBaseUrl: process.env.APP_DEEP_LINK_BASE_URL || 'https://vidlynk.app/watch',
};
