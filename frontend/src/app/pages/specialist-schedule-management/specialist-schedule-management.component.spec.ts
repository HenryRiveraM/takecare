import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpecialistScheduleManagementComponent } from './specialist-schedule-management.component';

describe('SpecialistScheduleManagementComponent', () => {
  let component: SpecialistScheduleManagementComponent;
  let fixture: ComponentFixture<SpecialistScheduleManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpecialistScheduleManagementComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SpecialistScheduleManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
