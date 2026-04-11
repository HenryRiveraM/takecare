import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
  providedIn: 'root'
})
export class LanguageService {
  constructor(private translate: TranslateService) {
    const savedLang = localStorage.getItem('lang') || 'es';
    this.translate.use(savedLang);
  }

  setLanguage(lang: 'es' | 'en') {
    this.translate.use(lang);
    localStorage.setItem('lang', lang);
  }

  getCurrentLanguage(): 'es' | 'en' {
    return (this.translate.currentLang as 'es' | 'en') || 'es';
  }
}