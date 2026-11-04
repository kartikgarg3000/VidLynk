import dotenv from 'dotenv';
dotenv.config();

import { PrismaClient } from '@prisma/client';
import bcrypt from 'bcrypt';

const prisma = new PrismaClient();

async function main() {
  const email = 'admin@vidlynk.app';
  const rawPassword = 'VidLynkAdmin2026!';
  const passwordHash = await bcrypt.hash(rawPassword, 10);

  const admin = await prisma.admin.upsert({
    where: { email },
    update: {},
    create: {
      email,
      name: 'VidLynk Administrator',
      passwordHash,
    },
  });

  console.log(`✅ Default admin seeded:`);
  console.log(`   Email: ${admin.email}`);
  console.log(`   Password: ${rawPassword}`);
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
