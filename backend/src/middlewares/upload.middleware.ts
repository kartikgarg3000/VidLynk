import multer from 'multer';

// Use memory storage for direct upload streaming to Cloudflare R2
const storage = multer.memoryStorage();

export const uploadMedia = multer({
  storage,
  limits: {
    fileSize: 500 * 1024 * 1024, // 500 MB limit
  },
  fileFilter: (req, file, cb) => {
    if (file.fieldname === 'video') {
      if (
        file.mimetype.startsWith('video/') ||
        file.mimetype === 'application/x-mpegURL'
      ) {
        cb(null, true);
      } else {
        cb(new Error('Invalid video format. Supported formats: mp4, mkv, webm, m3u8'));
      }
    } else if (file.fieldname === 'thumbnail') {
      if (file.mimetype.startsWith('image/')) {
        cb(null, true);
      } else {
        cb(new Error('Invalid thumbnail format. Must be an image.'));
      }
    } else {
      cb(new Error('Unexpected field name'));
    }
  },
});

