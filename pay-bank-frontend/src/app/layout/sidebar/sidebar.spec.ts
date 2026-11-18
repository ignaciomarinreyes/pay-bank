import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SidebarComponent } from './sidebar';

describe('SidebarComponent', () => {
  it('has the "Nuevo pago" and "Movimientos" links, in that order', async () => {
    await TestBed.configureTestingModule({
      imports: [SidebarComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(SidebarComponent);
    await fixture.whenStable();

    const links = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a'));
    expect(links.map((l) => l.textContent?.trim())).toEqual(['Nuevo pago', 'Movimientos']);
    expect(links.map((l) => l.getAttribute('href'))).toEqual(['/payments/new', '/movements']);
  });
});
