import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  // Usamos una Signal para que Angular detecte cambios al instante
  isOpen = signal(false);

  toggle() {
    this.isOpen.update(value => !value);
  }

  close() {
    this.isOpen.set(false);
  }
}
