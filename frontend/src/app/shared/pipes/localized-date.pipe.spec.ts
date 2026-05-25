import { LocalizedDatePipe } from './localized-date.pipe';
import { LanguageService } from '../../services/language.service';

describe('LocalizedDatePipe', () => {
  it('keeps a calendar-only date on the same day', () => {
    const languageService = {
      getCurrentLanguage: () => 'es'
    } as LanguageService;
    const pipe = new LocalizedDatePipe(languageService);

    expect(pipe.transform('2026-05-01')).toBe('01/05/2026');
  });
});
