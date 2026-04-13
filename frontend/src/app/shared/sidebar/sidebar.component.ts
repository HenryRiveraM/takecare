import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../services/auth.service';
import { SidebarService } from '../../services/sidebar.service';

const ROL_MAP: { [key: number]: 'patient' | 'specialist' | 'admin' } = {
  1: 'patient',
  2: 'specialist',
  3: 'admin'
};

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslateModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.css']
})
export class SidebarComponent implements OnInit {
  role: 'patient' | 'specialist' | 'admin' | null = null;

  constructor(
    private authService: AuthService, 
    private router: Router,
    private cdr: ChangeDetectorRef,
    public sidebarService: SidebarService
  ) {}

  ngOnInit(): void {
    this.loadUserRole();
  }

  loadUserRole(): void {
    // Leemos directamente del storage para no modificar el AuthService ajeno
    const rawData = localStorage.getItem('user');
    
    if (rawData) {
      try {
        const parsed = JSON.parse(rawData);
        
        // Buscamos el rol donde sea que esté (directo o dentro de .data)
        const roleId = parsed.role || (parsed.data ? parsed.data.role : null);

        if (roleId) {
          this.role = ROL_MAP[Number(roleId)] || null;
          this.cdr.detectChanges(); // Forzamos a que el HTML vea el cambio
        }
      } catch (e) {
        console.error('Error al leer usuario en Sidebar:', e);
      }
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
