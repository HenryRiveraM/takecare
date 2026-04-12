import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.css'
})
export class FooterComponent implements OnInit {
  showFooter = true;

  constructor(
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.updateFooterState();

    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => this.updateFooterState());
  }

  private updateFooterState(): void {
    const currentUrl = this.router.url.split('?')[0];
    const firstSegment = currentUrl.split('/').filter(Boolean)[0] ?? '';

    if (firstSegment === 'patient' || firstSegment === 'specialist' || firstSegment === 'admin') {
      this.showFooter = false;
      return;
    }

    let currentRoute = this.route.firstChild;

    while (currentRoute?.firstChild) {
      currentRoute = currentRoute.firstChild;
    }

    this.showFooter = currentRoute?.snapshot.data['showNavbar'] !== false;
  }
}
