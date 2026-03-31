import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpecialistDashboardComponent } from './specialist-dashboard.component';

describe('SpecialistDashboardComponent', () => {
  let component: SpecialistDashboardComponent;
  let fixture: ComponentFixture<SpecialistDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpecialistDashboardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SpecialistDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
