import { TestBed } from '@angular/core/testing';

import { ClinicalDocumentService } from './clinical-document.service';

describe('ClinicalDocumentService', () => {
  let service: ClinicalDocumentService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ClinicalDocumentService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
