import { Router } from 'express';
import { AdminController } from '../controllers/admin.controller';
import { requireAdminAuth } from '../middlewares/auth.middleware';
import { uploadMedia } from '../middlewares/upload.middleware';

const router = Router();

// Protect all admin routes with JWT
router.use(requireAdminAuth);

// Update admin credentials (email, password, name)
router.put('/profile', AdminController.updateCredentials);

router.post(
  '/videos/upload',
  uploadMedia.fields([
    { name: 'video', maxCount: 1 },
    { name: 'thumbnail', maxCount: 1 },
  ]),
  AdminController.uploadVideo
);

router.get('/videos', AdminController.getVideos);
router.delete('/videos/:id', AdminController.deleteVideo);

export default router;
