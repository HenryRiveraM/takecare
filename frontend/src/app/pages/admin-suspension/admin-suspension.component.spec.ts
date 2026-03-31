import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminSuspensionComponent } from './admin-suspension.component';

describe('AdminSuspensionComponent', () => {
  let component: AdminSuspensionComponent;
  let fixture: ComponentFixture<AdminSuspensionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminSuspensionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminSuspensionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
