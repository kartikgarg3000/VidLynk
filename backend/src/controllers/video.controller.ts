import { Request, Response } from 'express';
import { VideoService } from '../services/video.service';

export class VideoController {
  /**
   * Public streaming endpoint: Fetch video metadata and CDN streaming URL by slug.
   * This is called by the Android VidLynk app immediately when a deep link is resolved.
   */
  static async getVideoBySlug(req: Request, res: Response) {
    try {
      const { slug } = req.params;

      if (!slug) {
        return res.status(400).json({
          success: false,
          message: 'Slug parameter is required',
        });
      }

      const video = await VideoService.getVideoBySlug(slug);

      if (!video) {
        return res.status(404).json({
          success: false,
          message: 'Video not found or has been removed',
        });
      }

      // If videoUrl was saved as local host, adapt it to the request host (e.g. 10.0.2.2:5000 for Android emulator)
      const host = req.get('host');
      let videoUrl = video.videoUrl;
      if (host && videoUrl.includes('localhost:')) {
        videoUrl = videoUrl.replace(/localhost:\d+/, host);
      }

      return res.json({
        success: true,
        data: {
          ...video,
          videoUrl,
        },
      });
    } catch (error: any) {
      console.error('Error fetching video by slug:', error);
      return res.status(500).json({
        success: false,
        message: 'Server error while resolving video',
      });
    }
  }

  /**
   * Health / Digital Asset Links for Android App Links verification
   * (.well-known/assetlinks.json)
   */
  static async getAssetLinks(req: Request, res: Response) {
    // In production, your SHA256 fingerprint from release keystore is added here
    res.setHeader('Content-Type', 'application/json');
    return res.json([
      {
        relation: ['delegate_permission/common.handle_all_urls'],
        target: {
          namespace: 'android_app',
          package_name: 'com.vidlynk.app',
          sha256_cert_fingerprints: [
            // Example production keystore SHA256
            '14:6D:E9:7D:0F:52:AB:E0:85:47:A9:73:5F:73:95:6D:A8:3E:8C:33:16:74:98:23:DD:32:4C:E6:E5:A9:39:1A',
          ],
        },
      },
    ]);
  }
}
