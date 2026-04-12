import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd, ActivatedRoute, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
})
export class NavbarComponent implements OnInit {

  showNavbar: boolean = true;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    public languageService: LanguageService
  ) {}

  ngOnInit(): void {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        let currentRoute = this.route.firstChild;

        while (currentRoute?.firstChild) {
          currentRoute = currentRoute.firstChild;
        }

        this.showNavbar = currentRoute?.snapshot.data['showNavbar'] !== false;
      });
  }

  changeLanguage(event: Event): void {
    const lang = (event.target as HTMLSelectElement).value as 'es' | 'en';
    this.languageService.setLanguage(lang);
  }
}