import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpecialistAppointmentsComponent } from './specialist-appointments.component';

describe('SpecialistAppointmentsComponent', () => {
  let component: SpecialistAppointmentsComponent;
  let fixture: ComponentFixture<SpecialistAppointmentsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpecialistAppointmentsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SpecialistAppointmentsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
