import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpecialistResourcesComponent } from './specialist-resources.component';

describe('SpecialistResourcesComponent', () => {
  let component: SpecialistResourcesComponent;
  let fixture: ComponentFixture<SpecialistResourcesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SpecialistResourcesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SpecialistResourcesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
