import { PutObjectCommand, DeleteObjectCommand } from '@aws-sdk/client-s3';
import { s3Client } from '../config/storage';
import { config } from '../config';
import fs from 'fs';
import path from 'path';

export class StorageService {
  private static isR2Configured(): boolean {
    const { accountId, accessKeyId, secretAccessKey } = config.r2;
    return (
      Boolean(accountId) &&
      accountId !== 'your_cloudflare_account_id' &&
      Boolean(accessKeyId) &&
      accessKeyId !== 'your_r2_access_key_id' &&
      Boolean(secretAccessKey) &&
      secretAccessKey !== 'your_r2_secret_access_key'
    );
  }

  /**
   * Upload buffer directly to Cloudflare R2 bucket, or local disk fallback for dev testing
   */
  static async uploadBuffer(
    buffer: Buffer,
    key: string,
    contentType: string,
    hostHeader?: string
  ): Promise<string> {
    if (this.isR2Configured()) {
      try {
        const command = new PutObjectCommand({
          Bucket: config.r2.bucketName,
          Key: key,
          Body: buffer,
          ContentType: contentType,
        });

        await s3Client.send(command);

        // Return public CDN streaming URL
        return `${config.r2.publicDomain.replace(/\/$/, '')}/${key}`;
      } catch (err) {
        console.warn('⚠️ Cloudflare R2 upload failed, falling back to local storage:', err);
      }
    }

    // Local Disk Storage Fallback (for immediate local development & testing)
    const uploadDir = path.join(process.cwd(), 'uploads');
    const filePath = path.join(uploadDir, key);

    await fs.promises.mkdir(path.dirname(filePath), { recursive: true });
    await fs.promises.writeFile(filePath, buffer);

    const host = hostHeader || `localhost:${config.port}`;
    const proto = (host.includes('localhost') || host.includes('127.0.0.1')) ? 'http' : 'https';
    return `${proto}://${host}/uploads/${key}`;
  }

  /**
   * Delete media object from R2 or local disk
   */
  static async deleteObject(key: string): Promise<void> {
    if (this.isR2Configured()) {
      try {
        const command = new DeleteObjectCommand({
          Bucket: config.r2.bucketName,
          Key: key,
        });
        await s3Client.send(command);
        return;
      } catch (e) {
        // continue to try local delete
      }
    }

    try {
      const filePath = path.join(process.cwd(), 'uploads', key);
      if (fs.existsSync(filePath)) {
        await fs.promises.unlink(filePath);
      }
    } catch (e) {
      // Ignore
    }
  }
}
