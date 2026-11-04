import { prisma } from '../config/prisma';
import { nanoid } from 'nanoid';
import { RedisService } from './redis.service';
import { config } from '../config';

export class VideoService {
  /**
   * Generates a short, user-friendly slug for links (e.g. "vl_k29a4m")
   */
  static generateSlug(): string {
    return `vl_${nanoid(8)}`;
  }

  /**
   * Get public video metadata by slug for Android streaming
   */
  static async getVideoBySlug(slug: string) {
    // 1. Check Redis cache first
    const cached = await RedisService.getCachedVideo(slug);
    if (cached) {
      // Async trigger view count increment
      RedisService.incrementViews(slug, cached.id).catch(() => {});
      return cached;
    }

    // 2. Query Postgres
    const video = await prisma.video.findUnique({
      where: { slug, isPublished: true },
      select: {
        id: true,
        slug: true,
        title: true,
        description: true,
        duration: true,
        thumbnailUrl: true,
        videoUrl: true,
        mimeType: true,
        resolution: true,
        viewsCount: true,
        createdAt: true,
      },
    });

    if (!video) {
      return null;
    }

    const payload = {
      ...video,
      shareUrl: `${config.deepLinkBaseUrl}/${video.slug}`,
    };

    // 3. Cache in Redis
    await RedisService.setCachedVideo(slug, payload);

    // 4. Trigger view increment
    RedisService.incrementViews(slug, video.id).catch(() => {});

    return payload;
  }

  /**
   * List videos for admin dashboard with pagination
   */
  static async listAdminVideos(page = 1, limit = 20) {
    const skip = (page - 1) * limit;

    const [total, videos] = await Promise.all([
      prisma.video.count(),
      prisma.video.findMany({
        skip,
        take: limit,
        orderBy: { createdAt: 'desc' },
      }),
    ]);

    return {
      total,
      page,
      limit,
      totalPages: Math.ceil(total / limit),
      videos: videos.map((v) => ({
        ...v,
        sizeBytes: v.sizeBytes.toString(),
        shareUrl: `${config.deepLinkBaseUrl}/${v.slug}`,
      })),
    };
  }

  /**
   * Create video record
   */
  static async createVideo(data: {
    title: string;
    description?: string;
    videoUrl: string;
    thumbnailUrl?: string;
    storageKey: string;
    mimeType: string;
    sizeBytes: bigint;
    duration?: number;
    resolution?: string;
  }) {
    const slug = this.generateSlug();

    const video = await prisma.video.create({
      data: {
        slug,
        title: data.title,
        description: data.description,
        videoUrl: data.videoUrl,
        thumbnailUrl: data.thumbnailUrl,
        storageKey: data.storageKey,
        mimeType: data.mimeType,
        sizeBytes: data.sizeBytes,
        duration: data.duration || 0,
        resolution: data.resolution || '1080p',
      },
    });

    return {
      ...video,
      sizeBytes: video.sizeBytes.toString(),
      shareUrl: `${config.deepLinkBaseUrl}/${video.slug}`,
    };
  }

  /**
   * Delete video
   */
  static async deleteVideo(id: string) {
    const video = await prisma.video.findUnique({ where: { id } });
    if (!video) return null;

    await RedisService.invalidateVideoCache(video.slug);
    await prisma.video.delete({ where: { id } });

    return video;
  }
}
