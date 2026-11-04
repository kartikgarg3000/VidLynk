import { redis } from '../config/redis';
import { prisma } from '../config/prisma';

export class RedisService {
  private static readonly VIDEO_CACHE_TTL = 3600; // 1 hour
  private static readonly VIEW_BUFFER_PREFIX = 'views:buffer:';

  /**
   * Get cached video metadata
   */
  static async getCachedVideo(slug: string): Promise<any | null> {
    try {
      const data = await redis.get(`video:slug:${slug}`);
      if (data) {
        return JSON.parse(data);
      }
    } catch (e) {
      // Graceful fallback if Redis is down
    }
    return null;
  }

  /**
   * Cache video metadata
   */
  static async setCachedVideo(slug: string, video: any): Promise<void> {
    try {
      // Convert BigInt to string before serialization if any
      const serializable = JSON.parse(
        JSON.stringify(video, (key, value) =>
          typeof value === 'bigint' ? value.toString() : value
        )
      );
      await redis.setex(
        `video:slug:${slug}`,
        this.VIDEO_CACHE_TTL,
        JSON.stringify(serializable)
      );
    } catch (e) {
      // Non-critical, ignore
    }
  }

  /**
   * Invalidate video cache (when admin updates/deletes)
   */
  static async invalidateVideoCache(slug: string): Promise<void> {
    try {
      await redis.del(`video:slug:${slug}`);
    } catch (e) {
      // Non-critical, ignore
    }
  }

  /**
   * Atomically increment view count in Redis
   * Flush to DB periodically or asynchronously
   */
  static async incrementViews(slug: string, videoId: string): Promise<void> {
    try {
      const key = `${this.VIEW_BUFFER_PREFIX}${videoId}`;
      const count = await redis.incr(key);

      // Batch sync to Postgres when threshold reaches 50 views
      if (count >= 50) {
        await redis.del(key);
        await prisma.video.update({
          where: { id: videoId },
          data: { viewsCount: { increment: count } },
        });
      }
    } catch (e) {
      // Fallback direct DB increment
      try {
        await prisma.video.update({
          where: { id: videoId },
          data: { viewsCount: { increment: 1 } },
        });
      } catch (err) {
        // Silently continue
      }
    }
  }
}

