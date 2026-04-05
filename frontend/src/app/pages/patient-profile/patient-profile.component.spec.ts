import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PatientProfileComponent } from './patient-profile.component';
import { provideHttpClient } from '@angular/common/http'; // Necesario si usas servicios
import { provideRouter } from '@angular/router'; // Necesario si usas routerLink

describe('PatientProfileComponent', () => {
  let component: PatientProfileComponent;
  let fixture: ComponentFixture<PatientProfileComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      // Si el componente es Standalone, va en imports
      imports: [PatientProfileComponent],
      // Aquí añadimos los proveedores básicos para que no explote
      providers: [
        provideHttpClient(),
        provideRouter([])
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PatientProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
