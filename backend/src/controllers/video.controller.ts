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

      // Adapt URLs if saved with localhost or relative path
      const host = req.get('host');
      const proto = (host && (host.includes('localhost') || host.includes('127.0.0.1'))) ? 'http' : 'https';

      let videoUrl = video.videoUrl;
      if (host && videoUrl.includes('localhost:')) {
        videoUrl = videoUrl.replace(/https?:\/\/localhost:\d+/, `${proto}://${host}`);
      }

      let thumbnailUrl = video.thumbnailUrl;
      if (thumbnailUrl) {
        if (host && thumbnailUrl.includes('localhost:')) {
          thumbnailUrl = thumbnailUrl.replace(/https?:\/\/localhost:\d+/, `${proto}://${host}`);
        } else if (thumbnailUrl.startsWith('/') && host) {
          thumbnailUrl = `${proto}://${host}${thumbnailUrl}`;
        }
      }

      return res.json({
        success: true,
        data: {
          ...video,
          videoUrl,
          thumbnailUrl,
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
   * Public Web Watch & Open Graph preview route (/watch/:slug)
   * Rendered for Telegram bots, WhatsApp link unfurlers, and browser visitors.
   */
  static async renderWatchPage(req: Request, res: Response) {
    try {
      const { slug } = req.params;
      const video = await VideoService.getVideoBySlug(slug);

      if (!video) {
        return res.status(404).send(`
          <html><body style="background:#090A0F; color:#fff; text-align:center; padding:50px; font-family:sans-serif;">
            <h2>Video Not Found</h2>
            <p>This stream has expired or has been removed.</p>
          </body></html>
        `);
      }

      const host = req.get('host') || 'vidlynk.app';
      const isHttps = req.secure || (!host.includes('localhost') && !host.includes('127.0.0.1'));
      const protocol = isHttps ? 'https' : 'http';
      const shareUrl = `${protocol}://${host}/watch/${video.slug}`;

      let thumbnailUrl = video.thumbnailUrl || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&q=80';
      if (thumbnailUrl.startsWith('/')) {
        thumbnailUrl = `${protocol}://${host}${thumbnailUrl}`;
      } else if (thumbnailUrl.includes('localhost:') && host) {
        thumbnailUrl = thumbnailUrl.replace(/https?:\/\/localhost:\d+/, `${protocol}://${host}`);
      }

      const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${video.title} - VidLynk Stream</title>
  
  <!-- Telegram / WhatsApp / Twitter Open Graph Previews -->
  <meta property="og:type" content="video.other" />
  <meta property="og:site_name" content="VidLynk" />
  <meta property="og:title" content="${video.title}" />
  <meta property="og:description" content="🌀 Open link & watch online + download 👇" />
  <meta property="og:image" content="${thumbnailUrl}" />
  <meta property="og:image:secure_url" content="${thumbnailUrl}" />
  <meta property="og:image:width" content="1280" />
  <meta property="og:image:height" content="720" />
  <meta property="og:url" content="${shareUrl}" />
  <meta name="twitter:card" content="summary_large_image" />
  <meta name="twitter:title" content="${video.title}" />
  <meta name="twitter:description" content="🌀 Open link & watch online + download 👇" />
  <meta name="twitter:image" content="${thumbnailUrl}" />

  <style>
    body { background-color: #090A0F; color: #E5E7EB; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 20px; display: flex; align-items: center; justify-content: center; min-height: 100vh; box-sizing: border-box; }
    .card { max-width: 480px; width: 100%; background: #12141C; border-radius: 20px; border: 1px solid rgba(255,255,255,0.1); padding: 24px; text-align: center; box-shadow: 0 10px 40px rgba(0,0,0,0.6); }
    .thumb { width: 100%; aspect-ratio: 16/9; object-fit: cover; border-radius: 14px; margin-bottom: 18px; border: 1px solid rgba(255,255,255,0.08); }
    .btn { display: block; width: 100%; box-sizing: border-box; background: #00E5FF; color: #090A0F; font-weight: 800; font-size: 16px; padding: 15px; border-radius: 14px; text-decoration: none; margin-top: 18px; transition: transform 0.15s; }
    .btn:active { transform: scale(0.98); }
  </style>

  <script>
    // Automatically attempt to trigger VidLynk Android App deep link
    setTimeout(() => {
      window.location.href = "intent://watch/${video.slug}#Intent;scheme=vidlynk;package=com.vidlynk.app;end";
    }, 400);
  </script>
</head>
<body>
  <div class="card">
    <img src="${thumbnailUrl}" alt="${video.title}" class="thumb" />
    <h2 style="font-size: 20px; margin: 0 0 8px 0; color: #fff;">${video.title}</h2>
    <p style="color: #9CA3AF; font-size: 14px; margin: 0 0 6px 0;">🌀 Open link & watch online + download 👇</p>
    <a href="intent://watch/${video.slug}#Intent;scheme=vidlynk;package=com.vidlynk.app;end" class="btn">
      ⚡ Open in VidLynk App
    </a>
  </div>
</body>
</html>`;

      res.setHeader('Content-Type', 'text/html; charset=utf-8');
      return res.send(html);
    } catch (err: any) {
      console.error('Error rendering watch page:', err);
      return res.status(500).send('Server Error');
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
