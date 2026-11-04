import { Router } from 'express';
import { VideoController } from '../controllers/video.controller';

const router = Router();

// Android deep link metadata lookup: /api/v1/videos/:slug
router.get('/:slug', VideoController.getVideoBySlug);

export default router;

