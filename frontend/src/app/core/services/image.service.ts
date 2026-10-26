import { Injectable } from '@angular/core';

/** Client-side compression so uploads stay small on mobile data. */
@Injectable({ providedIn: 'root' })
export class ImageService {
  async compress(file: File, maxSide = 1280, quality = 0.82): Promise<Blob> {
    const bitmap = await createImageBitmap(file);
    const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height));
    const width = Math.round(bitmap.width * scale);
    const height = Math.round(bitmap.height * scale);

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Canvas not supported');
    ctx.drawImage(bitmap, 0, 0, width, height);
    bitmap.close();

    return new Promise((resolve, reject) =>
      canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Could not process the photo'))), 'image/jpeg', quality),
    );
  }
}
