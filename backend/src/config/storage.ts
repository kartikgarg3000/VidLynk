import { S3Client } from '@aws-sdk/client-s3';
import { config } from './index';

// Cloudflare R2 provides an S3-compatible API endpoint:
// https://<ACCOUNT_ID>.r2.cloudflarestorage.com
const endpoint = config.r2.accountId
  ? `https://${config.r2.accountId}.r2.cloudflarestorage.com`
  : undefined;

export const s3Client = new S3Client({
  region: 'auto',
  endpoint,
  credentials: {
    accessKeyId: config.r2.accessKeyId,
    secretAccessKey: config.r2.secretAccessKey,
  },
});

