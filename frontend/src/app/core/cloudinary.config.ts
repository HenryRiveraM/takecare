import { environment } from '../../environments/environment';

export const CLOUDINARY_CONFIG = {
  cloudName: environment.cloudinary.cloudName,
  uploadPreset: environment.cloudinary.uploadPreset,
  uploadUrl: `https://api.cloudinary.com/v1_1/${environment.cloudinary.cloudName}/image/upload`
};