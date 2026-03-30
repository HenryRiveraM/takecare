import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminValidateSpecialistsComponent } from './admin-validate-specialists.component';

describe('AdminValidateSpecialistsComponent', () => {
  let component: AdminValidateSpecialistsComponent;
  let fixture: ComponentFixture<AdminValidateSpecialistsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminValidateSpecialistsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminValidateSpecialistsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
