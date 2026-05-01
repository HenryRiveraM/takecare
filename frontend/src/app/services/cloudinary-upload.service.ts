import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CLOUDINARY_CONFIG } from '../core/cloudinary.config';
import { environment } from '../../environments/environment';

export interface CloudinaryUploadResponse {
  asset_id: string;
  public_id: string;
  version: number;
  resource_type: string;
  created_at: string;
  bytes: number;
  type: string;
  etag: string;
  url: string;
  secure_url: string;
  folder?: string;
  original_filename?: string;
  format?: string;
  width?: number;
  height?: number;
}

@Injectable({
  providedIn: 'root'
})
export class CloudinaryUploadService {

  private readonly cloudName = environment.cloudinary.cloudName;
  private readonly uploadPreset = environment.cloudinary.uploadPreset;

  constructor(private http: HttpClient) {}

  async uploadImage(file: File, folder: string): Promise<CloudinaryUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', CLOUDINARY_CONFIG.uploadPreset);
    formData.append('folder', folder);

    return await firstValueFrom(
      this.http.post<CloudinaryUploadResponse>(
        `https://api.cloudinary.com/v1_1/${this.cloudName}/image/upload`,
        formData
      )
    );
  }

    async uploadAuto(file: File, folder: string): Promise<CloudinaryUploadResponse> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('upload_preset', this.uploadPreset);
    formData.append('folder', folder);

    return await firstValueFrom(
      this.http.post<CloudinaryUploadResponse>(
        `https://api.cloudinary.com/v1_1/${this.cloudName}/auto/upload`,
        formData
      )
    );
  }
}