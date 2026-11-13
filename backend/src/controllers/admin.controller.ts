import { Request, Response } from 'express';
import { StorageService } from '../services/storage.service';
import { VideoService } from '../services/video.service';
import { nanoid } from 'nanoid';
import { AuthenticatedRequest } from '../middlewares/auth.middleware';
import { prisma } from '../config/prisma';
import bcrypt from 'bcrypt';

export class AdminController {
  /**
   * Upload video file and optional thumbnail
   * Automatically streams to Cloudflare R2 and creates DB record
   */
  static async uploadVideo(req: Request, res: Response) {
    try {
      const files = req.files as { [fieldname: string]: Express.Multer.File[] };
      const videoFile = files?.['video']?.[0];
      const thumbnailFile = files?.['thumbnail']?.[0];

      if (!videoFile) {
        return res.status(400).json({
          success: false,
          message: 'Video file is required',
        });
      }

      const { title, description, duration, resolution } = req.body;

      if (!title) {
        return res.status(400).json({
          success: false,
          message: 'Title is required',
        });
      }

      // Generate unique storage keys for Cloudflare R2
      const videoExt = videoFile.originalname.split('.').pop() || 'mp4';
      const videoKey = `videos/${nanoid(12)}.${videoExt}`;
      const host = req.get('host');
      const videoUrl = await StorageService.uploadBuffer(
        videoFile.buffer,
        videoKey,
        videoFile.mimetype,
        host
      );

      let thumbnailUrl: string | undefined = undefined;
      if (thumbnailFile) {
        const thumbExt = thumbnailFile.originalname.split('.').pop() || 'jpg';
        const thumbKey = `thumbnails/${nanoid(12)}.${thumbExt}`;
        thumbnailUrl = await StorageService.uploadBuffer(
          thumbnailFile.buffer,
          thumbKey,
          thumbnailFile.mimetype,
          host
        );
      }

      // Save to database
      const videoRecord = await VideoService.createVideo({
        title,
        description,
        videoUrl,
        thumbnailUrl,
        storageKey: videoKey,
        mimeType: videoFile.mimetype,
        sizeBytes: BigInt(videoFile.size),
        duration: duration ? parseInt(duration, 10) : 0,
        resolution: resolution || '1080p',
      });

      return res.status(201).json({
        success: true,
        message: 'Video uploaded and published successfully',
        data: videoRecord,
      });
    } catch (error: any) {
      console.error('Error in uploadVideo:', error);
      return res.status(500).json({
        success: false,
        message: error.message || 'Failed to upload video',
      });
    }
  }

  /**
   * List all videos for admin
   */
  static async getVideos(req: Request, res: Response) {
    try {
      const page = parseInt(req.query.page as string, 10) || 1;
      const limit = parseInt(req.query.limit as string, 10) || 20;

      const result = await VideoService.listAdminVideos(page, limit);
      return res.json({
        success: true,
        data: result,
      });
    } catch (error: any) {
      return res.status(500).json({ success: false, message: error.message });
    }
  }

  /**
   * Delete video by ID
   */
  static async deleteVideo(req: Request, res: Response) {
    try {
      const { id } = req.params;
      const video = await VideoService.deleteVideo(id);

      if (!video) {
        return res.status(404).json({
          success: false,
          message: 'Video not found',
        });
      }

      // Cleanup object from Cloudflare R2
      await StorageService.deleteObject(video.storageKey).catch(() => {});

      return res.json({
        success: true,
        message: 'Video deleted successfully',
      });
    } catch (error: any) {
      return res.status(500).json({ success: false, message: error.message });
    }
  }

  /**
   * Change admin email and password securely
   */
  static async updateCredentials(req: AuthenticatedRequest, res: Response) {
    try {
      const adminId = req.admin?.id;
      if (!adminId) {
        return res.status(401).json({ success: false, message: 'Unauthorized' });
      }

      const { currentPassword, newEmail, newPassword, newName } = req.body;

      if (!currentPassword) {
        return res.status(400).json({
          success: false,
          message: 'Current password is required to verify identity',
        });
      }

      const admin = await prisma.admin.findUnique({ where: { id: adminId } });
      if (!admin) {
        return res.status(404).json({ success: false, message: 'Admin not found' });
      }

      const isMatch = await bcrypt.compare(currentPassword, admin.passwordHash);
      if (!isMatch) {
        return res.status(401).json({ success: false, message: 'Incorrect current password' });
      }

      const updateData: any = {};

      if (newEmail && newEmail.trim() !== '') {
        const trimmedEmail = newEmail.trim().toLowerCase();
        const existing = await prisma.admin.findUnique({ where: { email: trimmedEmail } });
        if (existing && existing.id !== adminId) {
          return res.status(400).json({ success: false, message: 'Email already in use by another account' });
        }
        updateData.email = trimmedEmail;
      }

      if (newPassword) {
        if (newPassword.length < 6) {
          return res.status(400).json({ success: false, message: 'New password must be at least 6 characters' });
        }
        updateData.passwordHash = await bcrypt.hash(newPassword, 10);
      }

      if (newName && newName.trim() !== '') {
        updateData.name = newName.trim();
      }

      const updated = await prisma.admin.update({
        where: { id: adminId },
        data: updateData,
        select: { id: true, email: true, name: true, updatedAt: true },
      });

      return res.json({
        success: true,
        message: 'Admin credentials updated successfully. Please use your new email and password for future logins.',
        data: updated,
      });
    } catch (error: any) {
      console.error('Error updating admin credentials:', error);
      return res.status(500).json({ success: false, message: error.message });
    }
  }
}
