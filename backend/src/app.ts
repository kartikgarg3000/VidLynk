import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import path from 'path';
import authRoutes from './routes/auth.routes';
import adminRoutes from './routes/admin.routes';
import videoRoutes from './routes/video.routes';
import { VideoController } from './controllers/video.controller';
import { errorHandler } from './middlewares/error.middleware';

export const createApp = () => {
  const app = express();

  // Security & logging
  app.use(helmet({
    crossOriginResourcePolicy: { policy: 'cross-origin' },
    contentSecurityPolicy: false
  }));
  app.use(cors({ origin: '*' }));
  app.use(express.json());
  app.use(express.urlencoded({ extended: true }));
  app.use(morgan('combined'));

  // Admin Web Portal (Single Page App)
  app.use('/admin', express.static(path.join(process.cwd(), 'public/admin')));
  app.get(['/admin', '/admin/*'], (req, res) => {
    res.sendFile(path.join(process.cwd(), 'public/admin/index.html'));
  });

  // Static uploads serving for local testing
  app.use('/uploads', express.static(path.join(process.cwd(), 'uploads')));

  // Public Web Watch & OpenGraph Preview for Telegram/WhatsApp
  app.get('/watch/:slug', VideoController.renderWatchPage);

  // Android App Links verification route
  app.get('/.well-known/assetlinks.json', VideoController.getAssetLinks);

  // Health check
  app.get('/health', (req, res) => {
    res.json({ status: 'ok', service: 'vidlynk-api', timestamp: new Date().toISOString() });
  });

  // API Routes
  app.use('/api/v1/auth', authRoutes);
  app.use('/api/v1/admin', adminRoutes);
  app.use('/api/v1/videos', videoRoutes);

  // Global error handler
  app.use(errorHandler);

  return app;
};
